package com.spectralogic.dsbrowser.gui.components.localfiletreetable;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.GetObjectRequest;
import com.spectralogic.ds3client.commands.GetObjectResponse;
import com.spectralogic.ds3client.commands.spectrads3.GetBulkJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetBulkJobSpectraS3Response;
import com.spectralogic.ds3client.models.BulkObject;
import com.spectralogic.ds3client.models.MasterObjectList;
import com.spectralogic.ds3client.models.Objects;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.models.common.CommonPrefixes;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Ds3GetJob extends Ds3JobTask {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PutJob.class);
    private final ImmutableSet.Builder<String> partOfDirBuilder;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final List<Ds3TreeTableValue> list;
    private final FileTreeModel fileTreeModel;
    private final Ds3Client ds3Client;
    private final ArrayList<Ds3TreeTableValue> nodes;
    private String bucket;

    public Ds3GetJob(final List<Ds3TreeTableValue> list, final FileTreeModel fileTreeModel, final Ds3Client client, final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        this.list = list;
        this.fileTreeModel = fileTreeModel;
        this.ds3Client = client;
        this.bucket = null;
        partOfDirBuilder = ImmutableSet.builder();
        nodes = new ArrayList<>();
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
    }

    @Override
    public void executeJob() throws Exception {
        try {
            updateTitle("GET Job" + ds3Client.getConnectionDetails().getEndpoint() + " " + DateFormat.formatDate(new Date()));
            Platform.runLater(() -> {
                deepStorageBrowserPresenter.logText("Get Job initiated " + ds3Client.getConnectionDetails().getEndpoint(), LogType.INFO);
            });

            updateMessage("Transferring..");

            //assigning value in next step hence no final
            Path localDirPath = fileTreeModel.getPath();
            if (fileTreeModel.getType().equals(FileTreeModel.Type.File)) {
                localDirPath = localDirPath.getParent();
            }

            final ImmutableList<Ds3TreeTableValue> directories = list.stream().filter(value -> !value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());
            final ImmutableList<Ds3TreeTableValue> files = list.stream().filter(value -> value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());


            files.stream().forEach(value -> {
                nodes.add(value);
            });

            directories.stream().forEach(value -> {
                addAllDescendents(value, nodes);

            });


            final List<Ds3TreeTableValue> filteredNode = nodes.stream().filter(i -> !i.getSize().equals("--")).collect(Collectors.toList());

            final ImmutableList<Ds3Object> objects = filteredNode.stream().map(pair -> {
                try {
                    if (bucket == null) {
                        bucket = pair.getBucketName();
                    }
                    if (partOfDirBuilder.build().size() == 0)
                        return new Ds3Object(pair.getFullName(), Long.parseLong(pair.getSize().replaceAll("\\D+", "")));
                    else {
                        //can not assign final
                        Path dirPath = fileTreeModel.getPath();
                        if (fileTreeModel.getType().equals(FileTreeModel.Type.File)) {
                            dirPath = dirPath.getParent();
                        }

                        long size = 0;
                        try {
                            size = Long.parseLong(pair.getSize().replaceAll("\\D+", ""));
                        } catch (final Exception e) {
                            return null;
                        }

                        final Path pathToFile = Paths.get(dirPath.toString(), pair.getFullName());
                        Files.createDirectories(pathToFile.getParent());
                        return new Ds3Object(pair.getFullName(), size);

                    }
                } catch (final SecurityException | IOException e) {
                    LOG.error("Exception while creation directories ", e);
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("GET Job failed: " + ds3Client.getConnectionDetails().getEndpoint() + " " + DateFormat.formatDate(new Date()) + " " + e.toString(), LogType.SUCCESS);
                    });

                    return null;
                }
            }).filter(item -> item != null).collect(GuavaCollectors.immutableList());

            LOG.info("" + objects.size());
            GetBulkJobSpectraS3Response bulkResponse = null;
            switch (deepStorageBrowserPresenter.savedJobPrioritiesStore.getJobSettings().getGetJobPriority()) {
                case "HIGH":
                    bulkResponse = ds3Client
                            .getBulkJobSpectraS3(new GetBulkJobSpectraS3Request(bucket, objects).withPriority(Priority.HIGH));
                    break;
                case "CRITICAL":
                    bulkResponse = ds3Client
                            .getBulkJobSpectraS3(new GetBulkJobSpectraS3Request(bucket, objects).withPriority(Priority.CRITICAL));
                    break;
                case "URGENT":
                    bulkResponse = ds3Client
                            .getBulkJobSpectraS3(new GetBulkJobSpectraS3Request(bucket, objects).withPriority(Priority.URGENT));
                    break;
                case "NORMAL":
                    bulkResponse = ds3Client
                            .getBulkJobSpectraS3(new GetBulkJobSpectraS3Request(bucket, objects).withPriority(Priority.NORMAL));
                    break;
                case "LOW":
                    bulkResponse = ds3Client
                            .getBulkJobSpectraS3(new GetBulkJobSpectraS3Request(bucket, objects).withPriority(Priority.LOW));
                    break;
                case "BACKGROUND":
                    bulkResponse = ds3Client
                            .getBulkJobSpectraS3(new GetBulkJobSpectraS3Request(bucket, objects).withPriority(Priority.BACKGROUND));
                    break;
                default:
                    bulkResponse = ds3Client
                            .getBulkJobSpectraS3(new GetBulkJobSpectraS3Request(bucket, objects));
                    break;
            }

            long workDone = 0;
            final MasterObjectList list = bulkResponse.getResult();
            for (final Objects objects1 : list.getObjects()) {
                long totalSize = objects1.getObjects().stream().mapToLong(BulkObject::getLength).sum();
                for (final BulkObject obj : objects1.getObjects()) {
                    final File file = new File(obj.getName());
                    //can not assign final
                    String path = file.getPath();
                    bucket = bulkResponse.getResult().getBucketName();
                    if (partOfDirBuilder.build().size() == 0) {
                        path = file.getName();
                    }
                    final FileChannel channel = FileChannel.open(
                            localDirPath.resolve(localDirPath + "//" + path),
                            StandardOpenOption.WRITE,
                            StandardOpenOption.CREATE
                    );
                    // To handle the case where a file has been chunked we need to seek to the correct offset
                    // before we make the GetObject call so that when the request writes to the channel it is
                    // writing at the correct offset in the file.
                    channel.position(obj.getOffset());

                    workDone = workDone + obj.getLength();
                    updateMessage("Transferring file -> " + obj.getName() + " to " + localDirPath + " - " + FileSizeFormat.getFileSizeType(workDone) + "/" + FileSizeFormat.getFileSizeType(totalSize));
                    updateProgress(workDone, totalSize);

                    // Perform the operation to get the object from DS3.
                    GetObjectResponse response = ds3Client.getObject(new GetObjectRequest(
                            bucket,
                            obj.getName(),
                            channel,
                            list.getJobId(),
                            obj.getOffset()
                    ));
                }
            }

            Platform.runLater(() -> {
                deepStorageBrowserPresenter.logText("GET Job completed " + ds3Client.getConnectionDetails().getEndpoint() + " " + DateFormat.formatDate(new Date()), LogType.SUCCESS);
            });


        } catch (final Exception e) {
            //Could be permission issue. Need to handle
            LOG.info("Exception" + e.toString());
            Platform.runLater(() -> {
                deepStorageBrowserPresenter.logText("GET Job Failed" + ds3Client.getConnectionDetails().getEndpoint() + ". Reason: " + e.toString(), LogType.ERROR);
            });
        }
    }

    private void addAllDescendents(final Ds3TreeTableValue value, final ArrayList nodes) {

        try {
            partOfDirBuilder.add(value.getFullName());
            final GetBucketRequest request = new GetBucketRequest(value.getBucketName()).withDelimiter("/");
            // Don't include the prefix if the item we are looking up from is the base bucket
            if (value.getType() != Ds3TreeTableValue.Type.Bucket) {
                request.withPrefix(value.getFullName());
            }
            final GetBucketResponse bucketResponse = ds3Client.getBucket(request);
            final ImmutableList<Ds3TreeTableValue> files = bucketResponse.getListBucketResult()
                    .getObjects().stream()
                    .map(f -> new Ds3TreeTableValue(value.getBucketName(), f.getKey(), Ds3TreeTableValue.Type.File, FileSizeFormat.getFileSizeType(f.getSize()), "")).collect(GuavaCollectors.immutableList());

            files.stream().forEach(i -> {
                nodes.add(i);
            });

            final ImmutableList<Ds3TreeTableValue> directoryValues = bucketResponse.getListBucketResult()
                    .getCommonPrefixes().stream().map(CommonPrefixes::getPrefix)
                    .map(c -> new Ds3TreeTableValue(value.getBucketName(), c, Ds3TreeTableValue.Type.Directory, FileSizeFormat.getFileSizeType(0), ""))
                    .collect(GuavaCollectors.immutableList());

            directoryValues.stream().forEach(i -> {
                addAllDescendents(i, nodes);
            });

        } catch (final IOException e) {
            e.printStackTrace();
        }


    }
}
