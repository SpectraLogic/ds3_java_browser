package com.spectralogic.dsbrowser.gui.services.tasks;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Response;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.helpers.FileObjectGetter;
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder;
import com.spectralogic.ds3client.metadata.MetadataReceivedListenerImpl;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.models.common.CommonPrefixes;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.spectralogic.dsbrowser.gui.util.StringConstants.*;

public class Ds3GetJob extends Ds3JobTask {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3GetJob.class);
    private final Alert ALERT = new Alert(Alert.AlertType.INFORMATION);
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final List<Ds3TreeTableValueCustom> list;
    private final Path fileTreeModel;
    private final Ds3Client ds3Client;
    private final ArrayList<Ds3TreeTableValueCustom> nodes;
    private final String jobPriority;
    private final Map<Path, Path> map;
    private final int maximumNumberOfParallelThreads;
    private final JobInterruptionStore jobInterruptionStore;
    private final Ds3Common ds3Common;
    private final ResourceBundle resourceBundle;
    private final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints;
    private UUID jobId;

    public Ds3GetJob(final List<Ds3TreeTableValueCustom> list, final Path fileTreeModel, final Ds3Client client, final DeepStorageBrowserPresenter deepStorageBrowserPresenter, final String jobPriority, final int maximumNumberOfParallelThreads, final JobInterruptionStore jobInterruptionStore, final Ds3Common ds3Common) {
        this.list = list;
        this.fileTreeModel = fileTreeModel;
        this.ds3Client = client;
        nodes = new ArrayList<>();
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.jobPriority = jobPriority;
        this.map = new HashMap<>();
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
        this.jobInterruptionStore = jobInterruptionStore;
        this.ds3Common = ds3Common;
        this.endpoints = jobInterruptionStore.getJobIdsModel().getEndpoints();
        this.resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
        final Stage stage = (Stage) ALERT.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEP_STORAGE_BROWSER));
    }


    @Override
    public void executeJob() throws Exception {
        //Job start time
        final Instant jobStartTimeInstant = Instant.now();
        LOG.info("Get Job started");
        try {
            updateTitle(resourceBundle.getString("blackPearlHealth"));
            ALERT.setHeaderText(null);
            if (CheckNetwork.isReachable(ds3Client)) {
                updateTitle(resourceBundle.getString("getJob") + ds3Client.getConnectionDetails().getEndpoint() + " " + DateFormat.formatDate(new Date()));
                Platform.runLater(() -> deepStorageBrowserPresenter.logText(resourceBundle.getString("getJobInitiated") + SPACE + ds3Client.getConnectionDetails().getEndpoint(), LogType.INFO));
                updateMessage(resourceBundle.getString("transferring") + "..");
                final ImmutableList<Ds3TreeTableValueCustom> directories = list.stream().filter(value -> !value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());
                final ImmutableList<Ds3TreeTableValueCustom> files = list.stream().filter(value -> value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());
                final Map<Path, Boolean> duplicateFileMap = new HashMap<>();
                final ImmutableMap.Builder<String, Path> fileMap = createFileMap(files);
                final ImmutableMap.Builder<String, Path> folderMap = createFolderMap(directories);
                final List<Ds3TreeTableValueCustom> filteredNode = nodes.stream().filter(i -> i.getSize() != 0).collect(Collectors.toList());
                final ImmutableList<Ds3Object> objects = getDS3Object(filteredNode);
                final long totalJobSize = objects.stream().mapToLong(Ds3Object::getSize).sum();
                final long freeSpace = new File(fileTreeModel.toString()).getFreeSpace();
                if (totalJobSize > freeSpace) {
                    Platform.runLater(() -> {
                        LOG.info("GET Job failed:Restoration directory does not have enough space");
                        deepStorageBrowserPresenter.logText(getJobFailedMessage(resourceBundle.getString("getJobFailed"), resourceBundle.getString("notEnoughSpace"), null), LogType.ERROR);
                        ALERT.setContentText(resourceBundle.getString("notEnoughSpaceAlert"));
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
                    updateMessage(resourceBundle.getString("transferring") + SPACE + FileSizeFormat.getFileSizeType(totalJobSize) + SPACE + resourceBundle.getString("from") + SPACE + bucket + SPACE + resourceBundle.getString("to") + SPACE + fileTreeModel);
                    //Change Priority
                    if (Guard.isStringNullOrEmpty(jobPriority)) {
                        ds3Client.modifyJobSpectraS3(new ModifyJobSpectraS3Request(getJob.getJobId()).withPriority(Priority.valueOf(jobPriority)));
                    }
                    final AtomicLong totalSent = new AtomicLong(0L);
                    getJob.attachDataTransferredListener(l -> {
                        updateProgress(totalSent.getAndAdd(l) / 2, totalJobSize);
                        totalSent.addAndGet(l);
                    });
                    getJob.attachObjectCompletedListener(obj -> {
                        final Instant currentTime = Instant.now();
                        if (duplicateFileMap.get(Paths.get(fileTreeModel + FORWARD_SLASH + obj)) != null && duplicateFileMap.get(Paths.get(fileTreeModel + FORWARD_SLASH + obj)).equals(true)) {
                            Platform.runLater(() -> deepStorageBrowserPresenter.logText(resourceBundle.getString("fileOverridden") + SPACE + obj + SPACE + resourceBundle.getString("to") + SPACE + fileTreeModel, LogType.SUCCESS));
                        } else {
                            final long timeElapsedInSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTime.toEpochMilli() - jobStartTimeInstant.toEpochMilli());
                            long transferRate = 0;
                            if (timeElapsedInSeconds != 0) {
                                transferRate = (totalSent.get() / 2) / timeElapsedInSeconds;
                            }
                            if (transferRate != 0) {
                                final long timeRemaining = (totalJobSize - (totalSent.get() / 2)) / transferRate;
                                updateMessage(getTransferRateString(transferRate, timeRemaining, totalSent, totalJobSize, obj, fileTreeModel.toString()));
                            } else {
                                updateMessage(getTransferRateString(transferRate, 0, totalSent, totalJobSize, obj, fileTreeModel.toString()));
                            }
                            Platform.runLater(() -> deepStorageBrowserPresenter.logText(resourceBundle.getString("successfullyTransferred") + SPACE + obj + SPACE + resourceBundle.getString("to") + SPACE + fileTreeModel, LogType.SUCCESS));
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
                    // check whether chunk are available
                    getJob.attachWaitingForChunksListener(retryAfterSeconds -> {
                        for (int retryTimeRemaining = retryAfterSeconds; retryTimeRemaining >= 0; retryTimeRemaining--) {
                            try {
                                updateMessage(resourceBundle.getString("noAvailableChunks") + retryTimeRemaining + resourceBundle.getString("seconds"));
                                Thread.sleep(1000);
                            } catch (final Exception e) {
                                LOG.error("Exception in attachWaitingForChunksListener", e);
                            }
                        }
                        updateMessage(resourceBundle.getString("transferring") + "..");
                    });
                    getJob.transfer(l -> {
                        final File file = new File(l);
                        String skipPath = null;
                        if (new File(fileTreeModel + FORWARD_SLASH + l).exists()) {
                            duplicateFileMap.put(Paths.get(fileTreeModel + FORWARD_SLASH + l), true);
                        }
                        if (map.size() == 0 && file.getParent() != null) {
                            skipPath = file.getParent();
                        }
                        if (Guard.isNullOrEmpty(files)) {
                            final Path skipPath1 = map.get(file.toPath());
                            if (skipPath1 != null) {
                                skipPath = skipPath1.toString();
                            }
                        }
                        if (skipPath != null) {
                            return new PrefixRemoverObjectChannelBuilder(new FileObjectGetter(fileTreeModel), skipPath).buildChannel(l.substring((FORWARD_SLASH + skipPath).length()));
                        } else {
                            return new FileObjectGetter(fileTreeModel).buildChannel(l);
                        }
                    });
                    updateProgress(totalJobSize, totalJobSize);
                    updateMessage(resourceBundle.getString("filesSize") + SPACE + FileSizeFormat.getFileSizeType(totalJobSize) + resourceBundle.getString("transferredTo") + SPACE + fileTreeModel);
                    updateProgress(totalJobSize, totalJobSize);
                    //Can't assign final.
                    GetJobSpectraS3Response response = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
                    while (response.getMasterObjectListResult().getStatus().toString().equals("IN_PROGRESS")) {
                        updateMessage(resourceBundle.getString("transferredToCache") + SPACE);
                        response = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
                    }
                    if (response.getMasterObjectListResult().getStatus().toString().equals("COMPLETED")) {
                        ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(), ds3Client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
                    }
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText(resourceBundle.getString("getJobSize") + SPACE + FileSizeFormat.getFileSizeType(totalJobSize) + resourceBundle.getString("getCompleted") + SPACE + ds3Client.getConnectionDetails().getEndpoint() + " " + DateFormat.formatDate(new Date()), LogType.SUCCESS));
                }
            } else {
                final String msg = resourceBundle.getString("host") + SPACE + ds3Client.getConnectionDetails().getEndpoint() + resourceBundle.getString("unreachable");
                BackgroundTask.dumpTheStack(msg);
                Platform.runLater(() -> {
                    deepStorageBrowserPresenter.logText(resourceBundle.getString("unableToReachNetwork"), LogType.ERROR);
                    ALERT.setTitle(resourceBundle.getString("unavailableNetwork"));
                    ALERT.setContentText(msg);
                    ALERT.showAndWait();
                });
            }

        } catch (final NoSuchElementException e) {
            LOG.error("The job failed to process", e);
            Platform.runLater(() -> deepStorageBrowserPresenter.logTextForParagraph(getJobFailedMessage("GET Job Failed ", "can't transfer bucket/empty folder", e), LogType.ERROR));
        } catch (final RuntimeException e) {
            LOG.error("The job failed to process", e);
            Platform.runLater(() -> deepStorageBrowserPresenter.logTextForParagraph(getJobFailedMessage("GET Job Failed ", "", e), LogType.ERROR));
        } catch (final Throwable t) {
            LOG.error("The job failed to process", t);
            Platform.runLater(() -> deepStorageBrowserPresenter.logText(getJobFailedMessage("GET Job Failed ", "", t), LogType.ERROR));
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(endpoints, ds3Client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter.getJobProgressView(), jobId);
            final Session session = ds3Common.getCurrentSession();
            final String currentSelectedEndpoint = session.getEndpoint() + ":" + session.getPortNo();
            if (currentSelectedEndpoint.equals(ds3Client.getConnectionDetails().getEndpoint())) {
                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);
            }
        }
    }

    /**
     * Get Ds3Object from list
     *
     * @param filteredNode list of objects having size nonzero
     * @return Ds3Object list
     */
    public ImmutableList<Ds3Object> getDS3Object(final List<Ds3TreeTableValueCustom> filteredNode) {
        return filteredNode.stream().map(pair -> {
            try {
                return new Ds3Object(pair.getFullName(), pair.getSize());
            } catch (final SecurityException e) {
                LOG.error("Exception while creation directories ", e);
                Platform.runLater(() -> deepStorageBrowserPresenter.logText(getJobFailedMessage(resourceBundle.getString("getJobFailed"), "", e), LogType.ERROR));
                return null;
            }
        }).filter(item -> item != null).collect(GuavaCollectors.immutableList());
    }

    /**
     * create map of folders from list of objects
     *
     * @param directories list of objects
     * @return map of folders
     */
    public ImmutableMap.Builder<String, Path> createFolderMap(final ImmutableList<Ds3TreeTableValueCustom> directories) {
        final ImmutableMap.Builder<String, Path> folderMap = ImmutableMap.builder();
        directories.forEach(value -> {
            if (null != value && value.getType().equals(Ds3TreeTableValue.Type.Directory)) {
                if (Paths.get(value.getFullName()).getParent() != null) {
                    folderMap.put(value.getFullName(), Paths.get(value.getFullName()).getParent());
                    addAllDescendants(value, nodes, Paths.get(value.getFullName()).getParent());
                } else {
                    addAllDescendants(value, nodes, null);
                    folderMap.put(value.getFullName(), Paths.get(FORWARD_SLASH));
                }
            }
        });
        return folderMap;
    }

    /**
     * Create map of files from the selected objects
     *
     * @param files selected objects
     * @return map of files
     */
    public ImmutableMap.Builder<String, Path> createFileMap(final ImmutableList<Ds3TreeTableValueCustom> files) {
        final ImmutableMap.Builder<String, Path> fileMap = ImmutableMap.builder();
        files.forEach(file -> {
            fileMap.put(file.getFullName(), Paths.get(FORWARD_SLASH));
            nodes.add(file);
        });
        return fileMap;
    }

    /**
     * Get all descendants
     *
     * @param value item needs to be transferred
     * @param nodes filtered nodes
     * @param path  path
     */
    public Map<Path, Path> addAllDescendants(final Ds3TreeTableValueCustom value, final ArrayList nodes, final Path path) {
        try {
            LOG.info("adding up all descendants");
            final GetBucketRequest request = new GetBucketRequest(value.getBucketName()).withDelimiter("/").withMaxKeys(1000000);
            if (value.getType() != Ds3TreeTableValue.Type.Bucket) {
                request.withPrefix(value.getFullName());
            }
            final GetBucketResponse bucketResponse = ds3Client.getBucket(request);
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
            directoryValues.forEach(i -> addAllDescendants(i, nodes, path));

        } catch (final IOException e) {
            LOG.error("Unable to add descendants", e);
            Platform.runLater(() -> deepStorageBrowserPresenter.logText(getJobFailedMessage("GET Job Cancelled ", "", e), LogType.ERROR));
        }
        return map;
    }

    /**
     * show message in case job failure
     *
     * @param failureType whether cancelled or failed
     * @param reason      reason for failure
     * @param e           Exception
     * @return String of job failure/cancelled
     */
    private String getJobFailedMessage(final String failureType, final String reason, final Throwable e) {
        if (null != e) {
            return failureType + ds3Client.getConnectionDetails().getEndpoint() + SPACE + DateFormat.formatDate(new Date()) + ". Reason+ " + reason + " " + e;
        } else {
            return failureType + ds3Client.getConnectionDetails().getEndpoint() + SPACE + DateFormat.formatDate(new Date()) + ". Reason+ " + reason;
        }
    }

    /**
     * get String for transfer rate
     *
     * @param transferRate  transfer rate
     * @param timeRemaining time remaining
     * @param totalSent     total sent
     * @param totalJobSize  total job size
     * @param obj           object needs to be transferred
     * @param fileTreeModel base path of local
     * @return return transfer rate String
     */
    private String getTransferRateString(final long transferRate, final long timeRemaining, final AtomicLong totalSent, final long totalJobSize, final String obj, final String fileTreeModel) {
        if (transferRate != 0) {
            return SPACE + resourceBundle.getString("transferRate") + SPACE + FileSizeFormat.getFileSizeType(transferRate) + resourceBundle.getString("perSecond") + SPACE + resourceBundle.getString("timeRemaining") + SPACE + DateFormat.timeConversion(timeRemaining) + FileSizeFormat.getFileSizeType(totalSent.get() / 2) + FORWARD_SLASH + FileSizeFormat.getFileSizeType(totalJobSize) + SPACE + resourceBundle.getString("transferredFile") + " -> " + obj + SPACE + resourceBundle.getString("to") + SPACE + fileTreeModel;
        } else {
            return SPACE + resourceBundle.getString("transferRate") + SPACE + FileSizeFormat.getFileSizeType(transferRate) + resourceBundle.getString("perSecond") + SPACE + resourceBundle.getString("timeRemaining") + SPACE + COLON + resourceBundle.getString("calculating") + ".. " + FileSizeFormat.getFileSizeType(totalSent.get() / 2) + FORWARD_SLASH + FileSizeFormat.getFileSizeType(totalJobSize) + SPACE + resourceBundle.getString("transferredFile") + " -> " + obj + SPACE + resourceBundle.getString("to") + SPACE + fileTreeModel;
        }
    }

    public Ds3Client getDs3Client() {
        return ds3Client;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobID(final UUID jobID) {
        this.jobId = jobID;
    }
}
