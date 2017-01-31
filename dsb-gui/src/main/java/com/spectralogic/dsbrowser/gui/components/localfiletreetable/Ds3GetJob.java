package com.spectralogic.dsbrowser.gui.components.localfiletreetable;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Response;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.helpers.FileObjectGetter;
import com.spectralogic.ds3client.helpers.MetadataReceivedListener;
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder;

import com.spectralogic.ds3client.metadata.MetadataReceivedListenerImpl;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.models.common.CommonPrefixes;
import com.spectralogic.ds3client.networking.Metadata;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
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

    private final Alert ALERT = new Alert(Alert.AlertType.INFORMATION);
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
    private final JobInterruptionStore jobInterruptionStore;
    private ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints;
    private final Ds3Common ds3Common;

    public Ds3GetJob(final List<Ds3TreeTableValueCustom> list, final Path fileTreeModel, final Ds3Client client, final DeepStorageBrowserPresenter deepStorageBrowserPresenter, final String jobPriority, final int maximumNumberOfParallelThreads, final JobInterruptionStore jobInterruptionStore, final Ds3Common ds3Common) {
        this.list = list;
        this.fileTreeModel = fileTreeModel;
        this.ds3Client = client;
        partOfDirBuilder = ImmutableSet.builder();
        nodes = new ArrayList<>();
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.jobPriority = jobPriority;
        this.map = new HashMap<>();
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
        this.jobInterruptionStore = jobInterruptionStore;
        this.ds3Common = ds3Common;
        final Stage stage = (Stage) ALERT.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEPSTORAGEBROWSER));
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobID(final UUID jobID) {
        this.jobId = jobID;
    }

    @Override
    public void executeJob() throws Exception {
        try {
            updateTitle("Checking BlackPearl's health");
            ALERT.setHeaderText(null);
            if (CheckNetwork.isReachable(ds3Client)) {
                updateTitle("GET Job" + ds3Client.getConnectionDetails().getEndpoint() + " " + DateFormat.formatDate(new Date()));
                Platform.runLater(() -> deepStorageBrowserPresenter.logText("Get Job initiated " + ds3Client.getConnectionDetails().getEndpoint(), LogType.INFO));
                updateMessage("Transferring..");
                final ImmutableList<Ds3TreeTableValueCustom> directories = list.stream().filter(value -> !value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());
                final ImmutableList<Ds3TreeTableValueCustom> files = list.stream().filter(value -> value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());
                final ImmutableMap.Builder<String, Path> fileMap = ImmutableMap.builder();
                final ImmutableMap.Builder<String, Path> folderMap = ImmutableMap.builder();
                final Map<Path, Boolean> duplicateFileMap = new HashMap<>();
                files.forEach(i -> {
                    fileMap.put(i.getFullName(), Paths.get("/"));
                    nodes.add(i);
                });
                directories.forEach(value -> {
                    if (value.getType().equals(Ds3TreeTableValue.Type.Directory)) {
                        if (Paths.get(value.getFullName()).getParent() != null) {
                            folderMap.put(value.getFullName(), Paths.get(value.getFullName()).getParent());
                            addAllDescendents(value, nodes, Paths.get(value.getFullName()).getParent());
                        } else {
                            addAllDescendents(value, nodes, null);
                            folderMap.put(value.getFullName(), Paths.get("/"));
                        }
                    }
                });
                final List<Ds3TreeTableValueCustom> filteredNode = nodes.stream().filter(i -> i.getSize() != 0).collect(Collectors.toList());
                final ImmutableList<Ds3Object> objects = filteredNode.stream().map(pair -> {
                    try {
                        return new Ds3Object(pair.getFullName(), pair.getSize());
                    } catch (final SecurityException e) {
                        LOG.error("Exception while creation directories ", e);
                        Platform.runLater(() -> deepStorageBrowserPresenter.logText("GET Job failed: " + ds3Client.getConnectionDetails().getEndpoint() + " " + DateFormat.formatDate(new Date()) + " " + e.toString(), LogType.SUCCESS));
                        return null;
                    }
                }).filter(item -> item != null).collect(GuavaCollectors.immutableList());
                final long totalJobSize = objects.stream().mapToLong(Ds3Object::getSize).sum();
                final long freeSpace = new File(fileTreeModel.toString()).getFreeSpace();
                if (totalJobSize > freeSpace) {
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("GET Job failed:Restoration directory does not have enough space ", LogType.ERROR);
                        ALERT.setContentText("GET Job failed:Restoration directory does not have enough space");
                        ALERT.showAndWait();
                    });

                } else {
                    final ImmutableList<String> buckets = filteredNode.stream().map(Ds3TreeTableValueCustom::getBucketName).distinct().collect(GuavaCollectors.immutableList());
                    final String bucket = buckets.stream().findFirst().get();
                    final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(ds3Client, 100);
                    final Ds3ClientHelpers.Job getJob = helpers.startReadJob(bucket, objects).withMaxParallelRequests(maximumNumberOfParallelThreads);
                    if (getJobId() == null) {
                        setJobID(getJob.getJobId());
                    }
                    try {
                        ParseJobInterruptionMap.saveValuesToFiles(jobInterruptionStore, fileMap.build(), folderMap.build(), ds3Client.getConnectionDetails().getEndpoint(), jobId, totalJobSize, fileTreeModel.toString(), "GET", bucket);
                    } catch (final Exception e) {
                        LOG.error("Failed to save job id", e);
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
                        if (duplicateFileMap.get(Paths.get(fileTreeModel + "/" + obj)) != null && duplicateFileMap.get(Paths.get(fileTreeModel + "/" + obj)).equals(true)) {
                            Platform.runLater(() -> deepStorageBrowserPresenter.logText("File has overridden successfully: " + obj + " to " + fileTreeModel, LogType.SUCCESS));
                        } else {
                            Platform.runLater(() -> deepStorageBrowserPresenter.logText("Successfully transferred: " + obj + " to " + fileTreeModel, LogType.SUCCESS));
                            updateMessage(FileSizeFormat.getFileSizeType(totalSent.get() / 2) + "/" + FileSizeFormat.getFileSizeType(totalJobSize) + " Transferring file -> " + obj + " to " + fileTreeModel);
                            updateProgress(totalSent.get(), totalJobSize);
                            // Platform.runLater(() -> deepStorageBrowserPresenter.logText("Successfully transferred: " + obj + " to " + fileTreeModel, LogType.SUCCESS));
                        }
                    });

                    //get meta data saved on server
                    LOG.info("Registering metadata receiver");
                    final MetadataReceivedListenerImpl metadataReceivedListener = new MetadataReceivedListenerImpl(fileTreeModel.toString());
                    getJob.attachMetadataReceivedListener((s, metadata) -> {
                        LOG.info("Restoring metadata for {}", s);
                        metadataReceivedListener.metadataReceived(s, metadata);
                    });

                    getJob.transfer(l -> {
                        final File file = new File(l);
                        String skipPath = null;
                        if (new File(fileTreeModel + "/" + l).exists()) {
                            duplicateFileMap.put(Paths.get(fileTreeModel + "/" + l), true);
                        }
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
                    updateProgress(totalJobSize, totalJobSize);
                    updateMessage("Files [Size: " + FileSizeFormat.getFileSizeType(totalJobSize) + "] transferred to " + fileTreeModel);
                    updateProgress(totalJobSize, totalJobSize);
                    //Can't assign final.
                    GetJobSpectraS3Response response = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
                    while (response.getMasterObjectListResult().getStatus().toString().equals("IN_PROGRESS")) {
                        updateMessage("Transferred to blackpearl cache ");
                        response = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
                    }
                    if (response.getMasterObjectListResult().getStatus().toString().equals("COMPLETED")) {
                        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(), ds3Client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
                    }
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("GET Job [size: " + FileSizeFormat.getFileSizeType(totalJobSize) + "] completed " + ds3Client.getConnectionDetails().getEndpoint() + " " + DateFormat.formatDate(new Date()), LogType.SUCCESS));
                }
            } else {
                BackgroundTask.dumpTheStack("Host " + ds3Client.getConnectionDetails().getEndpoint() + " is unreachable. Please check your connection");

                Platform.runLater(() -> {
                    deepStorageBrowserPresenter.logText("Unable to reach network", LogType.ERROR);
                    ALERT.setTitle("Unavailable Network");
                    ALERT.setContentText("Host " + ds3Client.getConnectionDetails().getEndpoint() + "is unreachable. Please check your connection");
                    ALERT.showAndWait();
                });
            }

        } catch (final NoSuchElementException e) {
            LOG.error("The job failed to process", e);
            Platform.runLater(() -> deepStorageBrowserPresenter.logTextForParagraph("GET Job Failed " + ds3Client.getConnectionDetails().getEndpoint() + ". Reason+ can't transfer bucket/empty folder", LogType.ERROR));
        } catch (final RuntimeException e) {
            LOG.error("The job failed to process", e);
            Platform.runLater(() -> deepStorageBrowserPresenter.logTextForParagraph("GET Job Failed " + ds3Client.getConnectionDetails().getEndpoint() + ". Reason+" + e.toString(), LogType.ERROR));
        } catch (final Throwable t) {
            LOG.error("The job failed to process", t);
            Platform.runLater(() -> deepStorageBrowserPresenter.logText("GET Job Failed " + ds3Client.getConnectionDetails().getEndpoint() + ". Reason+" + t.toString(), LogType.ERROR));
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(endpoints, ds3Client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter.getJobProgressView(), jobId);
            final Session session = ds3Common.getCurrentSession().stream().findFirst().get();
            final String currentSelectedEndpoint = session.getEndpoint() + ":" + session.getPortNo();
            if (currentSelectedEndpoint.equals(ds3Client.getConnectionDetails().getEndpoint())) {
                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);
            }
        }
    }

    private void addAllDescendents(final Ds3TreeTableValueCustom value, final ArrayList nodes, final Path path) {
        try {
            final GetBucketRequest request = new GetBucketRequest(value.getBucketName()).withDelimiter("/");
            // Don't include the prefix if the item we are looking up from is the base bucket
            if (value.getType() != Ds3TreeTableValue.Type.Bucket) {
                request.withPrefix(value.getFullName());
            }
            final GetBucketResponse bucketResponse = ds3Client.getBucket(request.withMaxKeys(10000000));
            final ImmutableList<Ds3TreeTableValueCustom> files = bucketResponse.getListBucketResult()
                    .getObjects().stream()
                    .map(f -> new Ds3TreeTableValueCustom(value.getBucketName(), f.getKey(), Ds3TreeTableValue.Type.File, f.getSize(), "", f.getOwner().getDisplayName(), false)
                    ).collect(GuavaCollectors.immutableList());
            files.forEach(i -> {
                nodes.add(i);
                map.put(Paths.get(i.getFullName()), path);
            });
            final ImmutableList<Ds3TreeTableValueCustom> directoryValues = bucketResponse.getListBucketResult()
                    .getCommonPrefixes().stream().map(CommonPrefixes::getPrefix)
                    .map(c -> new Ds3TreeTableValueCustom(value.getBucketName(), c, Ds3TreeTableValue.Type.Directory, 0, "", "--", false))
                    .collect(GuavaCollectors.immutableList());
            directoryValues.forEach(i -> addAllDescendents(i, nodes, path));

        } catch (final IOException e) {
            Platform.runLater(() -> deepStorageBrowserPresenter.logText("GET Job Cancelled. Response code:" + ds3Client.getConnectionDetails().getEndpoint() + ". Reason+" + e.toString(), LogType.ERROR));
        }
    }

    public Ds3Client getDs3Client() {
        return ds3Client;
    }
}
