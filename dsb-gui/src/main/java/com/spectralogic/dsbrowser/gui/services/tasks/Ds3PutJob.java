package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Response;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.metadata.MetadataAccessImpl;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
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
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.spectralogic.ds3client.models.RequestType.PUT;
import static com.spectralogic.dsbrowser.gui.util.Constants.RETRY_AFTER_COUNT;
import static com.spectralogic.dsbrowser.gui.util.StringConstants.*;

public class Ds3PutJob extends Ds3JobTask {
    private final static Logger LOG = LoggerFactory.getLogger(Ds3PutJob.class);
    private final Alert ALERT = new Alert(
            Alert.AlertType.INFORMATION
    );

    private final Ds3Client client;
    private final List<File> files;
    private final String bucket;
    private final String targetDir;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final String jobPriority;
    private final int maximumNumberOfParallelThreads;
    private final JobInterruptionStore jobInterruptionStore;
    private final Ds3Common ds3Common;
    private final SettingsStore settings;
    private final ResourceBundle resourceBundle;
    private UUID jobId;
    private boolean isJobFailed;

    public Ds3PutJob(final Ds3Client client, final List<File> files, final String bucket, final String targetDir, final DeepStorageBrowserPresenter deepStorageBrowserPresenter, final String jobPriority, final int maximumNumberOfParallelThreads, final JobInterruptionStore jobIdsModel, final Ds3Common ds3Common, final SettingsStore settings) {
        this.client = client;
        this.files = files;
        this.bucket = bucket;
        this.targetDir = targetDir;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.jobPriority = jobPriority;
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
        this.jobInterruptionStore = jobIdsModel;
        this.ds3Common = ds3Common;
        this.settings = settings;
        this.resourceBundle = ResourceBundleProperties.getResourceBundle();
        final Stage stage = (Stage) ALERT.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEP_STORAGE_BROWSER));
    }

    @Override
    public void executeJob() throws Exception {
        //job start time
        final Instant jobStartInstant = Instant.now();
        LOG.info("Put job Started");
        try {
            ALERT.setHeaderText(null);
            updateTitle(resourceBundle.getString("blackPearlHealth"));
            if (CheckNetwork.isReachable(client)) {
                final String startJobDate = DateFormat.formatDate(new Date());
                updateTitle(JobStatusStrings.jobInitiatedString(resourceBundle, PUT.toString(), startJobDate, client.getConnectionDetails().getEndpoint()).toString());
                Platform.runLater(() -> deepStorageBrowserPresenter.logText(JobStatusStrings.jobInitiatedString(resourceBundle, PUT.toString(), startJobDate, client.getConnectionDetails().getEndpoint()).toString(), LogType.INFO));
                updateMessage(resourceBundle.getString("transferring") + "..");
                final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(client, RETRY_AFTER_COUNT);
                final ImmutableList<Path> paths = files.stream().map(File::toPath).collect(GuavaCollectors.immutableList());
                final ImmutableList<Path> directories = paths.stream().filter(path -> Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
                final ImmutableList<Path> files = paths.stream().filter(path -> !Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
                final ImmutableSet.Builder<Path> partOfDirBuilder = ImmutableSet.builder();
                final ImmutableMultimap.Builder<Path, Path> expandedPaths = ImmutableMultimap.builder();
                final ImmutableMap.Builder<String, Path> fileMap = ImmutableMap.builder();
                final ImmutableMap.Builder<String, Path> folderMap = ImmutableMap.builder();
                createFileMap(files, directories, partOfDirBuilder, expandedPaths, fileMap);
                createFolderMap(directories, expandedPaths, folderMap, partOfDirBuilder);
                final ImmutableSet<Path> partOfDir = partOfDirBuilder.build();
                final ImmutableMap.Builder<String, Path> fileMapBuilder = ImmutableMap.builder();
                final ImmutableList<Ds3Object> objects = expandedPaths.build().entries().stream().map(pair -> {
                    try {
                        final long size = Files.size(pair.getValue());
                        final String ds3ObjPath = getDs3ObjectPath(pair.getKey(), pair.getValue(), partOfDir.contains(pair.getKey()), files.size(), directories.size());
                        final String ds3FileName = PathUtil.toDs3Path(targetDir, ds3ObjPath);
                        fileMapBuilder.put(ds3FileName, pair.getValue());
                        return new Ds3Object(ds3FileName, size);
                    } catch (final IOException e) {
                        LOG.error("Failed to get file size for: " + pair.getValue(), e);
                        return null;
                    }
                }).filter(item -> item != null).collect(GuavaCollectors.immutableList());
                final ImmutableMap<String, Path> fileMapper = fileMapBuilder.build();
                final long totalJobSize = objects.stream().mapToLong(Ds3Object::getSize).sum();
                updateMessage(JobStatusStrings.transferringTotalJobString(resourceBundle, FileSizeFormat.getFileSizeType(totalJobSize), bucket + "\\" + targetDir).toString());
                final Ds3ClientHelpers.Job job = helpers.startWriteJob(bucket, objects).withMaxParallelRequests(maximumNumberOfParallelThreads);
                jobId = job.getJobId();
                try {
                    final String targetLocation = PathUtil.toDs3Path(bucket, targetDir);
                    ParseJobInterruptionMap.saveValuesToFiles(jobInterruptionStore, fileMap.build(), folderMap.build(), client.getConnectionDetails().getEndpoint(), jobId, totalJobSize, targetLocation, PUT.toString(), bucket);
                } catch (final Exception e) {
                    LOG.error("Failed to save job id", e);
                }
                if (Guard.isStringNullOrEmpty(jobPriority)) {
                    client.modifyJobSpectraS3(new ModifyJobSpectraS3Request(job.getJobId()).withPriority(Priority.valueOf(jobPriority)));
                }
                final AtomicLong totalSent = new AtomicLong(0L);
                job.attachDataTransferredListener(l -> {
                    updateProgress(totalSent.getAndAdd(l) / 2, totalJobSize);
                    totalSent.addAndGet(l);
                });
                job.attachObjectCompletedListener(obj -> {
                    LOG.info("Object Transfer Completed");
                    final Instant nowInstant = Instant.now();
                    final String newDate = DateFormat.formatDate(new Date());
                    int index = 0;
                    if (obj.contains(FORWARD_SLASH)) {
                        index = obj.lastIndexOf(FORWARD_SLASH);
                    }
                    final long timeElapsedInSeconds = TimeUnit.MILLISECONDS.toSeconds(nowInstant.toEpochMilli() - jobStartInstant.toEpochMilli());
                    long transferRate = 0;
                    if (timeElapsedInSeconds != 0) {
                        transferRate = (totalSent.get() / 2) / timeElapsedInSeconds;
                    }
                    final String transferRateString;
                    if (transferRate != 0) {
                        final long timeRemaining = (totalJobSize - (totalSent.get() / 2)) / transferRate;
                        transferRateString = JobStatusStrings.getTransferRateString(resourceBundle, transferRate, timeRemaining, totalSent, totalJobSize, obj.substring(index, obj.length()), bucket + FORWARD_SLASH + targetDir).toString();
                    } else {
                        transferRateString = JobStatusStrings.getTransferRateString(resourceBundle, transferRate, 0, totalSent, totalJobSize, obj.substring(index, obj.length()), bucket + FORWARD_SLASH + targetDir).toString();
                    }
                    updateMessage(transferRateString);
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText(JobStatusStrings.objectSuccessfullyTransferredString(resourceBundle, obj.substring(0, obj.length()), bucket + FORWARD_SLASH + targetDir, newDate, resourceBundle.getString("blackPearlCache")).toString(), LogType.SUCCESS));

                });
                //store meta data to server
                final boolean isFilePropertiesEnable = settings.getFilePropertiesSettings().getFilePropertiesEnable();
                if (isFilePropertiesEnable) {
                    LOG.info("Registering metadata access Implementation");
                    job.withMetadata(new MetadataAccessImpl(fileMapper));
                    job.attachWaitingForChunksListener(retryAfterSeconds -> {
                        for (int retryTimeRemaining = retryAfterSeconds; retryTimeRemaining >= 0; retryTimeRemaining--) {
                            try {
                                updateMessage(resourceBundle.getString("noAvailableChunks") + SPACE + retryTimeRemaining + resourceBundle.getString("seconds"));
                                Thread.sleep(1000);
                            } catch (final Exception e) {
                                LOG.error("Exception in attachWaitingForChunksListener", e);
                            }
                        }
                        updateMessage(JobStatusStrings.transferringTotalJobString(resourceBundle, FileSizeFormat.getFileSizeType(totalJobSize), bucket + "\\" + targetDir).toString());
                    });
                    job.transfer(file -> FileChannel.open(PathUtil.resolveForSymbolic(fileMapper.get(file)), StandardOpenOption.READ));
                }
                final boolean isCacheJobEnable = settings.getShowCachedJobSettings().getShowCachedJob();
                final String dateOfTransfer = DateFormat.formatDate(new Date());
                if (isCacheJobEnable) {
                    updateProgress(totalJobSize, totalJobSize);
                    updateMessage(JobStatusStrings.jobSuccessfullyTransferredString(resourceBundle,PUT.toString(),FileSizeFormat.getFileSizeType(totalJobSize),bucket + "\\" + targetDir,dateOfTransfer , resourceBundle.getString("blackPearlCache") , isCacheJobEnable).toString());
                    updateProgress(totalJobSize, totalJobSize);
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText(JobStatusStrings.jobSuccessfullyTransferredString(resourceBundle,PUT.toString(),FileSizeFormat.getFileSizeType(totalJobSize),bucket + "\\" + targetDir,dateOfTransfer , resourceBundle.getString("blackPearlCache") , isCacheJobEnable).toString(), LogType.SUCCESS);
                    });
                    GetJobSpectraS3Response response = client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
                    while (!response.getMasterObjectListResult().getStatus().toString().equals(StringConstants.JOB_COMPLETED)) {
                        Thread.sleep(60000);
                        response = client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
                    }
                    LOG.info("Job transfered to permanent storage location");
                    final String newDate = DateFormat.formatDate(new Date());
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText(JobStatusStrings.jobSuccessfullyTransferredString(resourceBundle,PUT.toString(),FileSizeFormat.getFileSizeType(totalJobSize),bucket + "\\" + targetDir,newDate , resourceBundle.getString("permanentStorageLocation") , false).toString(), LogType.SUCCESS));
                } else {
                    updateProgress(totalJobSize, totalJobSize);
                    updateMessage(JobStatusStrings.jobSuccessfullyTransferredString(resourceBundle,PUT.toString(),FileSizeFormat.getFileSizeType(totalJobSize),bucket + "\\" + targetDir,dateOfTransfer , resourceBundle.getString("blackPearlCache"),false).toString());
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText(JobStatusStrings.jobSuccessfullyTransferredString(resourceBundle,PUT.toString(),FileSizeFormat.getFileSizeType(totalJobSize),bucket + "\\" + targetDir,dateOfTransfer , resourceBundle.getString("blackPearlCache"),false).toString(), LogType.SUCCESS));
                }
                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(), client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
            } else {
                final String msg = resourceBundle.getString("host") + SPACE + client.getConnectionDetails().getEndpoint() + resourceBundle.getString("unreachable");
                BackgroundTask.dumpTheStack(msg);
                Platform.runLater(() -> {
                    deepStorageBrowserPresenter.logText(resourceBundle.getString("unableToReachNetwork"), LogType.ERROR);
                    ALERT.setTitle(resourceBundle.getString("unavailableNetwork"));
                    ALERT.setContentText(msg);
                    ALERT.showAndWait();
                });
            }
        } catch (final RuntimeException rte) {
            //cancel the job if it is already running
            LOG.error("Encountered an error on a put job", rte);
            isJobFailed = true;
            final String newDate = DateFormat.formatDate(new Date());
            ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(), client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
            Platform.runLater(() -> deepStorageBrowserPresenter.logText(resourceBundle.getString("putJobFailed") + SPACE + client
                    .getConnectionDetails().getEndpoint() + resourceBundle.getString("reason") + rte + SPACE + resourceBundle.getString("at") + SPACE + newDate, LogType.ERROR));

        } catch (final InterruptedException ie) {
            isJobFailed = true;
            LOG.error("Encountered an error on a put job", ie);
            final String newDate = DateFormat.formatDate(new Date());
            Platform.runLater(() -> deepStorageBrowserPresenter.logText(resourceBundle.getString("putJobCancelled") + SPACE + resourceBundle.getString("at") + SPACE + newDate, LogType.ERROR));
        } catch (final Exception e) {
            isJobFailed = true;
            LOG.error("Encountered an error on a put job", e);
            final String newDate = DateFormat.formatDate(new Date());
            Platform.runLater(() -> deepStorageBrowserPresenter.logText(resourceBundle.getString("putJobFailed") + SPACE + client.getConnectionDetails().getEndpoint() + resourceBundle.getString("reason") + e + SPACE + resourceBundle.getString("at") + SPACE + newDate, LogType.ERROR));
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter.getJobProgressView(), jobId);
            final Session session = ds3Common.getCurrentSession();
            if (session != null) {
                final String currentSelectedEndpoint = session.getEndpoint() + COLON + session.getPortNo();
                if (currentSelectedEndpoint.equals(client.getConnectionDetails().getEndpoint())) {
                    ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);
                }
            }
        }
    }

    /**
     * Create map of folders from the selected list
     *
     * @param directories      selected directories
     * @param expandedPaths    expandedPaths
     * @param folderMap        map of folders
     * @param partOfDirBuilder partOfDirBuilder
     */
    public void createFolderMap(final ImmutableList<Path> directories, final ImmutableMultimap.Builder<Path, Path> expandedPaths, final ImmutableMap.Builder<String, Path> folderMap, final ImmutableSet.Builder<Path> partOfDirBuilder) {
        directories.forEach(path -> {
            try {
                partOfDirBuilder.add(path);
                expandedPaths.putAll(path, Files.walk(path).filter(child -> !Files.isDirectory(child)).collect(GuavaCollectors.immutableList()));
                final String ds3ObjPath = getDs3ObjectPath(path, path, true, files.size(), directories.size());
                final String ds3FileName = PathUtil.toDs3Path(targetDir, ds3ObjPath);
                folderMap.put(ds3FileName, path);
            } catch (final IOException e) {
                LOG.error("Failed to list files for directory: " + path.toString(), e);
            }
        });
    }

    /**
     * return ds3 object path
     *
     * @param key       key
     * @param value     value
     * @param contains  selected files contains directory or not
     * @param fileCount count of files
     * @param dirCount  count of directories
     * @return String path
     */
    public String getDs3ObjectPath(final Path key, final Path value, final boolean contains, final int fileCount, final int dirCount) {
        final String ds3ObjPath;
        if (fileCount == 0) {
            ds3ObjPath = PathUtil.toDs3Obj(key, value, contains);
        } else {
            if (dirCount == 0) {
                ds3ObjPath = PathUtil.toDs3Obj(key, value, contains);
            } else {
                ds3ObjPath = PathUtil.toDs3ObjWithFiles(key.getRoot(), value);
            }
        }
        return ds3ObjPath;
    }

    /**
     * Create Map of files needs to be transfered
     *
     * @param files            selected list of files
     * @param directories      selected list of directories
     * @param partOfDirBuilder partOfDirBuilder
     * @param expandedPaths    expandedPaths
     * @param fileMap          map of files
     */
    public void createFileMap(final ImmutableList<Path> files, final ImmutableList<Path> directories, final ImmutableSet.Builder<Path> partOfDirBuilder, final ImmutableMultimap.Builder<Path, Path> expandedPaths, final ImmutableMap.Builder<String, Path> fileMap) {
        files.forEach(filePath -> {
            boolean isContains = false;
            if (directories.size() != 0) {
                final Path parentPath = filePath.getParent();
                partOfDirBuilder.add(parentPath);
                isContains = true;
            }
            expandedPaths.put(filePath.getParent(), filePath);
            final String ds3ObjPath = getDs3ObjectPath(filePath.getParent(), filePath, isContains, files.size(), directories.size());
            final String ds3FileName = PathUtil.toDs3Path(targetDir, ds3ObjPath);
            fileMap.put(ds3FileName, filePath);
        });
    }

    public SettingsStore getSettings() {
        return settings;
    }

    public Ds3Client getClient() {
        return client;
    }

    public DeepStorageBrowserPresenter getDeepStorageBrowserPresenter() {
        return deepStorageBrowserPresenter;
    }

    public UUID getJobId() {
        return jobId;
    }

    public boolean isJobFailed() {
        return isJobFailed;
    }


}
