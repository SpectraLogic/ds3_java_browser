package com.spectralogic.dsbrowser.gui.components.localfiletreetable;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Response;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.helpers.FileObjectGetter;
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.models.common.CommonPrefixes;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Ds3GetJob extends Ds3JobTask {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PutJob.class);
    private final ImmutableSet.Builder<String> partOfDirBuilder;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final List<Ds3TreeTableValueCustom> list;
    private final Path fileTreeModel;
    private final Ds3Client ds3Client;
    private final ArrayList<Ds3TreeTableValueCustom> nodes;
    private final String jobPriority;
    final Map<Path, Path> map;
    private UUID jobId;
    private final int maximumNumberOfParallelThreads;

    public Ds3GetJob(final List<Ds3TreeTableValueCustom> list, final Path fileTreeModel, final Ds3Client client, final DeepStorageBrowserPresenter deepStorageBrowserPresenter, final String jobPriority, final int maximumNumberOfParallelThreads) {
        this.list = list;
        this.fileTreeModel = fileTreeModel;
        this.ds3Client = client;
        partOfDirBuilder = ImmutableSet.builder();
        nodes = new ArrayList<>();
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.jobPriority = jobPriority;
        this.map = new HashMap<>();
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobID(UUID jobID) {
        this.jobId = jobID;
    }

    @Override
    public void executeJob() throws Exception {
        try {
            updateTitle("GET Job" + ds3Client.getConnectionDetails().getEndpoint() + " " + DateFormat.formatDate(new Date()));
            Platform.runLater(() -> deepStorageBrowserPresenter.logText("Get Job initiated " + ds3Client.getConnectionDetails().getEndpoint(), LogType.INFO));
            updateMessage("Transferring..");

            final ImmutableList<Ds3TreeTableValueCustom> directories = list.stream().filter(value -> !value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());
            final ImmutableList<Ds3TreeTableValueCustom> files = list.stream().filter(value -> value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());

            directories.stream().forEach(i -> {
                if (files.size() == 0 && i.getType().equals(Ds3TreeTableValue.Type.File)) {
                    final Path filePath = Paths.get(i.getFullName());
                    nodes.add(i);
                    if (filePath.getParent() != null) {
                        map.put(filePath, filePath.getParent());
                    } else {
                        map.put(filePath, null);
                    }
                }
            });

            files.stream().forEach(nodes::add);

            directories.stream().forEach(value -> {
                if (value.getType().equals(Ds3TreeTableValue.Type.Directory)) {
                    if (Paths.get(value.getFullName()).getParent() != null) {
                        addAllDescendents(value, nodes, Paths.get(value.getFullName()).getParent());
                    } else {
                        addAllDescendents(value, nodes, null);
                    }
                }
            });

            final List<Ds3TreeTableValueCustom> filteredNode = nodes.stream().filter(i -> !i.getSize().equals("--")).collect(Collectors.toList());

            final ImmutableList<Ds3Object> objects = filteredNode.stream().map(pair -> {
                try {
                    return new Ds3Object(pair.getFullName(), FileSizeFormat.convertSizeToByte(pair.getSize()));
                } catch (final SecurityException e) {
                    LOG.error("Exception while creation directories ", e);
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("GET Job failed: " + ds3Client.getConnectionDetails().getEndpoint() + " " + DateFormat.formatDate(new Date()) + " " + e.toString(), LogType.SUCCESS));
                    return null;
                }
            }).filter(item -> item != null).collect(GuavaCollectors.immutableList());

            long totalJobSize = objects.stream().mapToLong(Ds3Object::getSize).sum();

            final ImmutableList<String> buckets = filteredNode.stream().map(Ds3TreeTableValueCustom::getBucketName).distinct().collect(GuavaCollectors.immutableList());

            final String bucket = buckets.stream().findFirst().get();

            final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(ds3Client);

            final Ds3ClientHelpers.Job getJob = helpers.startReadJob(bucket, objects).withMaxParallelRequests(maximumNumberOfParallelThreads);

            if (getJobId() == null) {
                setJobID(getJob.getJobId());
            }

            updateMessage("Transferring " + FileSizeFormat.getFileSizeType(totalJobSize) + " from " + bucket + " to " + fileTreeModel);

            if (jobPriority != null && !jobPriority.isEmpty()) {
                ds3Client.modifyJobSpectraS3(new ModifyJobSpectraS3Request(getJob.getJobId()).withPriority(Priority.valueOf(jobPriority)));
            }

            final AtomicLong totalSent = new AtomicLong(0L);

            getJob.attachDataTransferredListener(l -> {
                updateProgress(totalSent.getAndAdd(l) / 2, totalJobSize);
                totalSent.addAndGet(l);
            });

            getJob.attachObjectCompletedListener(obj -> {
                Platform.runLater(() -> deepStorageBrowserPresenter.logText("Successfully transferred: " + obj + " to " + fileTreeModel, LogType.SUCCESS));
                updateMessage(FileSizeFormat.getFileSizeType(totalSent.get() / 2) + "/" + FileSizeFormat.getFileSizeType(totalJobSize) + " Transferring file -> " + obj + " to " + fileTreeModel);
                updateProgress(totalSent.get(), totalJobSize);
            });

            getJob.transfer(l -> {
                final File file = new File(l);
                String skipPath = null;

                if (map.size() == 0 && file.getParent() != null) {
                    skipPath = file.getParent();
                }

                if (files.size() == 0) {
                    final Path skipPath1 = map.get(file.toPath());
                    if (skipPath1 != null) {
                        skipPath = skipPath1.toString();
                    }
                }

                if (skipPath != null) {
                    return new PrefixRemoverObjectChannelBuilder(new FileObjectGetter(fileTreeModel), skipPath).buildChannel(l.substring(("/" + skipPath).length()));
                } else {
                    return new FileObjectGetter(fileTreeModel).buildChannel(l);
                }
            });

            //Can't assign final.
            GetJobSpectraS3Response response = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));

            while (response.getMasterObjectListResult().getStatus().toString().equals("IN_PROGRESS")) {
                updateMessage("Transferred to blackpearl cache ");
                response = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
            }

            Platform.runLater(() -> deepStorageBrowserPresenter.logText("GET Job completed " + ds3Client.getConnectionDetails().getEndpoint() + " " + DateFormat.formatDate(new Date()), LogType.SUCCESS));


        } catch (final Exception e) {
            //Could be permission issue. Need to handle
            LOG.info("Exception" + e.toString());
            Platform.runLater(() -> deepStorageBrowserPresenter.logText("GET Job Failed" + ds3Client.getConnectionDetails().getEndpoint() + ". Reason: " + e.toString(), LogType.ERROR));
        }
    }

    private void addAllDescendents(final Ds3TreeTableValueCustom value, final ArrayList nodes, final Path path) {
        try {
            final GetBucketRequest request = new GetBucketRequest(value.getBucketName()).withDelimiter("/");
            // Don't include the prefix if the item we are looking up from is the base bucket
            if (value.getType() != Ds3TreeTableValue.Type.Bucket) {
                request.withPrefix(value.getFullName());
            }
            final GetBucketResponse bucketResponse = ds3Client.getBucket(request);
            final ImmutableList<Ds3TreeTableValueCustom> files = bucketResponse.getListBucketResult()
                    .getObjects().stream()
                    .map(f -> new Ds3TreeTableValueCustom(value.getBucketName(), f.getKey(), Ds3TreeTableValue.Type.File, FileSizeFormat.getFileSizeType(f.getSize()), "", f.getOwner().getDisplayName(), false)
                    ).collect(GuavaCollectors.immutableList());

            files.stream().forEach(i -> {
                nodes.add(i);
                map.put(Paths.get(i.getFullName()), path);
            });

            final ImmutableList<Ds3TreeTableValueCustom> directoryValues = bucketResponse.getListBucketResult()
                    .getCommonPrefixes().stream().map(CommonPrefixes::getPrefix)
                    .map(c -> new Ds3TreeTableValueCustom(value.getBucketName(), c, Ds3TreeTableValue.Type.Directory, FileSizeFormat.getFileSizeType(0), "", "--", false))
                    .collect(GuavaCollectors.immutableList());

            directoryValues.stream().forEach(i -> addAllDescendents(i, nodes, path));

        } catch (final IOException e) {
            Platform.runLater(() -> deepStorageBrowserPresenter.logText("GET Job Cancelled. Response code:" + ds3Client.getConnectionDetails().getEndpoint() + ". Reason+" + e.toString(), LogType.ERROR));
        }
    }

    public Ds3Client getDs3Client() {
        return ds3Client;
    }
}
