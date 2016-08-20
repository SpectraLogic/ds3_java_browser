package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Response;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.gui.util.PathUtil;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class Ds3PutJob extends Ds3JobTask {

    public Ds3Client getClient() {
        return client;
    }

    public DeepStorageBrowserPresenter getDeepStorageBrowserPresenter() {
        return deepStorageBrowserPresenter;
    }

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PutJob.class);

    private final Ds3Client client;
    private final List<File> files;
    private final String bucket;
    private final String targetDir;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final String jobPriority;
    private UUID jobId;
    private final int maximumNumberOfParallelThreads;

    public Ds3PutJob(final Ds3Client client, final List<File> files, final String bucket, final String targetDir, final DeepStorageBrowserPresenter deepStorageBrowserPresenter, final String jobPriority, final int maximumNumberOfParallelThreads) {
        this.client = client;
        this.files = files;
        this.bucket = bucket;
        this.targetDir = targetDir;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.jobPriority = jobPriority;
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
    }

    @Override
    public void executeJob() throws Exception {

        try {
            final String date = DateFormat.formatDate(new Date());
            updateTitle("PUT Job " + client.getConnectionDetails().getEndpoint() + " " + date);
            Platform.runLater(() -> deepStorageBrowserPresenter.logText("PUT Job initiated " + client.getConnectionDetails().getEndpoint(), LogType.INFO));

            final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(client);

            final ImmutableList<Path> paths = files.stream().map(File::toPath).collect(GuavaCollectors.immutableList());
            final ImmutableList<Path> directories = paths.stream().filter(path -> Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
            final ImmutableList<Path> files = paths.stream().filter(path -> !Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
            final ImmutableSet.Builder<Path> partOfDirBuilder = ImmutableSet.builder();
            final ImmutableMultimap.Builder<Path, Path> expandedPaths = ImmutableMultimap.builder();

            files.stream().forEach(path1 -> {
                if (directories.size() != 0) {
                    final Path pathNew = path1.getParent();
                    partOfDirBuilder.add(pathNew);
                }
                expandedPaths.put(path1.getParent(), path1);
            });

            directories.stream().forEach(path -> {
                try {
                    partOfDirBuilder.add(path);
                    expandedPaths.putAll(path, Files.walk(path).filter(child -> !Files.isDirectory(child)).collect(GuavaCollectors.immutableList()));
                } catch (final IOException e) {
                    LOG.error("Failed to list files for directory: " + path.toString(), e);
                }
            });

            final ImmutableSet<Path> partOfDir = partOfDirBuilder.build();
            final ImmutableMap.Builder<String, Path> fileMapBuilder = ImmutableMap.builder();
            final ImmutableList<Ds3Object> objects = expandedPaths.build().entries().stream().map(pair -> {
                try {
                    final long size = Files.size(pair.getValue());
                    String ds3ObjPath = null;
                    if (files.size() == 0) {
                        ds3ObjPath = PathUtil.toDs3Obj(pair.getKey(), pair.getValue(), partOfDir.contains(pair.getKey()));
                    } else {
                        if (directories.size() == 0) {
                            ds3ObjPath = PathUtil.toDs3Obj(pair.getKey(), pair.getValue(), partOfDir.contains(pair.getKey()));
                        } else {
                            ds3ObjPath = PathUtil.toDs3ObjWithFiles(pair.getKey().getRoot(), pair.getValue());
                        }
                    }

                    final String ds3FileName = PathUtil.toDs3Path(targetDir, ds3ObjPath);
                    fileMapBuilder.put(ds3FileName, pair.getValue());
                    return new Ds3Object(ds3FileName, size);
                } catch (final IOException e) {
                    System.out.println("Failed to get file size for: " + pair.getValue() + "---" + e);
                    return null;
                }
            }).filter(item -> item != null).collect(GuavaCollectors.immutableList());

            final ImmutableMap<String, Path> fileMapper = fileMapBuilder.build();
            final long totalJobSize = objects.stream().mapToLong(Ds3Object::getSize).sum();

            updateMessage("Transferring " + FileSizeFormat.getFileSizeType(totalJobSize) + " in " + bucket + "\\" + targetDir);

            final Ds3ClientHelpers.Job job = helpers.startWriteJob(bucket, objects).withMaxParallelRequests(maximumNumberOfParallelThreads);

            jobId = job.getJobId();

            if (jobPriority != null && !jobPriority.isEmpty()) {
                client.modifyJobSpectraS3(new ModifyJobSpectraS3Request(job.getJobId()).withPriority(Priority.valueOf(jobPriority)));
            }

            final AtomicLong totalSent = new AtomicLong(0L);

            job.attachDataTransferredListener(l -> {
                updateProgress(totalSent.getAndAdd(l) / 2, totalJobSize);
                totalSent.addAndGet(l);
            });

            job.attachObjectCompletedListener(obj -> {
                if (obj.contains("/")) {
                    final int i = obj.lastIndexOf("/");
                    updateMessage(FileSizeFormat.getFileSizeType(totalSent.get() / 2) + " / " + FileSizeFormat.getFileSizeType(totalJobSize) + " Transferred file -> " + obj.substring(i, obj.length()) + " to " + bucket + "/" + targetDir);
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Successfully transferred (BlackPearl cache): " + obj.substring(i, obj.length()) + " to " + bucket + "/" + targetDir, LogType.SUCCESS));
                } else {
                    updateMessage(FileSizeFormat.getFileSizeType(totalSent.get() / 2) + " / " + FileSizeFormat.getFileSizeType(totalJobSize) + "Transferred file -> " + obj.substring(0, obj.length()) + " to " + bucket + "/" + targetDir);
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Successfully transferred (BlackPearl cache): " + obj.substring(0, obj.length()) + " to " + bucket + "/" + targetDir, LogType.SUCCESS));
                }


            });

            job.transfer(s -> FileChannel.open(PathUtil.resolveForSymbolic(fileMapper.get(s)), StandardOpenOption.READ));

            updateProgress(totalJobSize, totalJobSize);
            updateMessage("Files transferred to" + " bucket -" + bucket + " at location - " + targetDir + ". (BlackPearl cache). Waiting for the storage target allocation.");
            updateProgress(totalJobSize, totalJobSize);

            Platform.runLater(() -> deepStorageBrowserPresenter.logText("PUT job completed. Files transferred to" + " bucket -" + bucket + " at location - " + targetDir + ". (BlackPearl cache). Waiting for the storage target allocation.", LogType.SUCCESS));

            //Can not assign final.
            GetJobSpectraS3Response response = client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));

            while (!response.getMasterObjectListResult().getStatus().toString().equals("COMPLETED")) {
                Thread.sleep(10000);
                response = client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
            }

            Platform.runLater(() -> deepStorageBrowserPresenter.logText("PUT job completed. File transferred to storage location", LogType.SUCCESS));
        } catch (final Exception e) {
            Platform.runLater(() -> deepStorageBrowserPresenter.logText("PUT Job Failed " + client.getConnectionDetails().getEndpoint() + ". Reason+" + e.toString(), LogType.ERROR));
        }
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }
}
