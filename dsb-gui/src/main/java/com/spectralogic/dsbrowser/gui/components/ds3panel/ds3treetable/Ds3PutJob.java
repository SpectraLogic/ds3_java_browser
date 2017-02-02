package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.*;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.metadata.MetadataAccessImpl;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
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
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
    private ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints;
    private UUID jobId;

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
        final Stage stage = (Stage) ALERT.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEPSTORAGEBROWSER));

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

    @Override
    public void executeJob() throws Exception {
        //job start time
        final Calendar jobStartTime = Calendar.getInstance();
        try {
            ALERT.setHeaderText(null);
            updateTitle("Checking BlackPearl's health");
            if (CheckNetwork.isReachable(client)) {
                final String date = DateFormat.formatDate(new Date());
                updateTitle("PUT Job " + client.getConnectionDetails().getEndpoint() + " " + date);
                Platform.runLater(() -> deepStorageBrowserPresenter.logText("PUT Job initiated " + client.getConnectionDetails().getEndpoint() + " at " + date, LogType.INFO));
                final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(client, 100);
                final ImmutableList<Path> paths = files.stream().map(File::toPath).collect(GuavaCollectors.immutableList());
                final ImmutableList<Path> directories = paths.stream().filter(path -> Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
                final ImmutableList<Path> files = paths.stream().filter(path -> !Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
                final ImmutableSet.Builder<Path> partOfDirBuilder = ImmutableSet.builder();
                final ImmutableMultimap.Builder<Path, Path> expandedPaths = ImmutableMultimap.builder();
                final ImmutableMap.Builder<String, Path> fileMap = ImmutableMap.builder();
                final ImmutableMap.Builder<String, Path> folderMap = ImmutableMap.builder();
                files.stream().forEach(path1 -> {
                    boolean isContains = false;
                    if (directories.size() != 0) {
                        final Path pathNew = path1.getParent();
                        partOfDirBuilder.add(pathNew);
                        isContains = true;
                    }
                    expandedPaths.put(path1.getParent(), path1);
                    final String ds3ObjPath = getDs3ObjectPath(path1.getParent(), path1, isContains, files.size(), directories.size());
                    final String ds3FileName = PathUtil.toDs3Path(targetDir, ds3ObjPath);
                    fileMap.put(ds3FileName, path1);
                });
                directories.stream().forEach(path -> {
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
                        LOG.error("Failed to get file size for: " + pair.getValue() + "---" + e);
                        return null;
                    }
                }).filter(item -> item != null).collect(GuavaCollectors.immutableList());
                final ImmutableMap<String, Path> fileMapper = fileMapBuilder.build();
                final long totalJobSize = objects.stream().mapToLong(Ds3Object::getSize).sum();
                updateMessage("Transferring " + FileSizeFormat.getFileSizeType(totalJobSize) + " in " + bucket + "\\" + targetDir);
                final Ds3ClientHelpers.Job job = helpers.startWriteJob(bucket, objects).withMaxParallelRequests(maximumNumberOfParallelThreads);
                jobId = job.getJobId();
                try {
                    final String targetLocation = PathUtil.toDs3Path(bucket, targetDir);
                    ParseJobInterruptionMap.saveValuesToFiles(jobInterruptionStore, fileMap.build(), folderMap.build(), client.getConnectionDetails().getEndpoint(), jobId, totalJobSize, targetLocation, "PUT", bucket);
                } catch (final Exception e) {
                    LOG.error("Failed to save job id", e);
                }
                if (jobPriority != null && !jobPriority.isEmpty()) {
                    client.modifyJobSpectraS3(new ModifyJobSpectraS3Request(job.getJobId()).withPriority(Priority.valueOf(jobPriority)));
                }
                final AtomicLong totalSent = new AtomicLong(0L);
                job.attachDataTransferredListener(l -> {
                    updateProgress(totalSent.getAndAdd(l) / 2, totalJobSize);
                    totalSent.addAndGet(l);
                });
                job.attachObjectCompletedListener(obj -> {
                    final Calendar currentTime = Calendar.getInstance();
                    final String newDate = DateFormat.formatDate(new Date());
                    if (obj.contains("/")) {
                        final int i = obj.lastIndexOf("/");
                        final long timeElapsedInSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTime.getTime().getTime() - jobStartTime.getTime().getTime());
                        final long transferRate = (totalSent.get() / 2) / timeElapsedInSeconds;
                        final long timeRemaining = (totalJobSize - (totalSent.get() / 2)) / transferRate;
                        updateMessage("  Transfer Rate " + FileSizeFormat.getFileSizeType(transferRate) + "/s" + "  Time remaining " + DateFormat.timeConversion(timeRemaining) + FileSizeFormat.getFileSizeType(totalSent.get() / 2) + " / " + FileSizeFormat.getFileSizeType(totalJobSize) + " Transferred file -> " + obj.substring(i, obj.length()) + " to " + bucket + "/" + targetDir);
                        Platform.runLater(() -> deepStorageBrowserPresenter.logText("Successfully transferred (BlackPearl cache): " + obj.substring(i, obj.length()) + " to " + bucket + "/" + targetDir + " at " + newDate, LogType.SUCCESS));
                    } else {
                        updateMessage(FileSizeFormat.getFileSizeType(totalSent.get() / 2) + " / " + FileSizeFormat.getFileSizeType(totalJobSize) + "Transferred file -> " + obj.substring(0, obj.length()) + " to " + bucket + "/" + targetDir);
                        Platform.runLater(() -> deepStorageBrowserPresenter.logText("Successfully transferred (BlackPearl cache): " + obj.substring(0, obj.length()) + " to " + bucket + "/" + targetDir + " at " + newDate, LogType.SUCCESS));
                    }
                });
                //store meta data to server
                final boolean isFilePropertiesEnable = settings.getFilePropertiesSettings().getFilePropertiesEnable();
                if (isFilePropertiesEnable) {
                    job.withMetadata(new MetadataAccessImpl(fileMapper));
                    // Path file = fileMapper.get(filename);
                    // check whether chunk are available
                    job.attachWaitingForChunksListener(retryAfterSeconds -> {
                        for (int retryTimeRemaining = retryAfterSeconds; retryTimeRemaining >= 0; retryTimeRemaining--) {
                            try {
                                updateMessage("No available chunks to transfer. Trying again in " + retryTimeRemaining + "seconds");
                                Thread.sleep(1000);
                            } catch (final Exception e) {
                                LOG.error("Exception in attachWaitingForChunksListener" + e.getMessage());
                            }
                        }
                        updateMessage("Transferring " + FileSizeFormat.getFileSizeType(totalJobSize) + " in " + bucket + "\\" + targetDir);
                    });
                    job.transfer(file -> FileChannel.open(PathUtil.resolveForSymbolic(fileMapper.get(file)), StandardOpenOption.READ));
                }
                final boolean isCacheJobEnable = settings.getShowCachedJobSettings().getShowCachedJob();
                final String dateOfTransfer = DateFormat.formatDate(new Date());
                if (isCacheJobEnable) {
                    updateProgress(totalJobSize, totalJobSize);
                    updateMessage("Files [Size: " + FileSizeFormat.getFileSizeType(totalJobSize) + "] transferred to" + " bucket " + bucket + " at location (BlackPearl cache)" + targetDir + " at " + dateOfTransfer + ". Waiting for job to complete...");
                    updateProgress(totalJobSize, totalJobSize);
                    Platform.runLater(() -> {
                        final String newDate = DateFormat.formatDate(new Date());
                        deepStorageBrowserPresenter.logText("PUT job [Size: " + FileSizeFormat.getFileSizeType(totalJobSize) + "] completed. Files transferred to" + " bucket " + bucket + " at location (BlackPearl cache) " + targetDir + " at " + newDate + ". Waiting for job to complete...", LogType.SUCCESS);
                    });
                    //Can not assign final.
                    GetJobSpectraS3Response response = client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
                    while (!response.getMasterObjectListResult().getStatus().toString().equals("COMPLETED")) {
                        Thread.sleep(60000);
                        response = client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
                    }
                    final String newDate = DateFormat.formatDate(new Date());
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("PUT job [Size: " + FileSizeFormat.getFileSizeType(totalJobSize) + "]  completed. File transferred to storage location" + " at " + newDate, LogType.SUCCESS));
                } else {
                    updateProgress(totalJobSize, totalJobSize);
                    updateMessage("Files [Size: " + FileSizeFormat.getFileSizeType(totalJobSize) + "] transferred to" + " bucket " + bucket + " at location (BlackPearl cache)" + targetDir + " at " + dateOfTransfer + ".");
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("PUT job [Size: " + FileSizeFormat.getFileSizeType(totalJobSize) + "]  completed. File transferred at location (BlackPearl cache)" + " at " + dateOfTransfer, LogType.SUCCESS));
                }
                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(), client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
            } else {
                Platform.runLater(() -> deepStorageBrowserPresenter.logText("Unable to reach network", LogType.ERROR));
                final String msg = "Host " + client.getConnectionDetails().getEndpoint() + "is unreachable. Please check your connection";
                BackgroundTask.dumpTheStack(msg);
                Platform.runLater(() -> ALERT.setTitle("Unavailable Network"));
                Platform.runLater(() -> ALERT.setContentText(msg));
                Platform.runLater(ALERT::showAndWait);
            }

        } catch (final RuntimeException rte) {
            //cancel the job if it is already running
            LOG.error("Encountered an error on a put job", rte);
            final String newDate = DateFormat.formatDate(new Date());
            ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(), client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
            Platform.runLater(() -> deepStorageBrowserPresenter.logTextForParagraph("PUT Job Failed " + client
                    .getConnectionDetails().getEndpoint() + ". Reason+" + rte + " at " + newDate, LogType.ERROR));

        } catch (final InterruptedException ie) {
            LOG.error("Encountered an error on a put job", ie);
            final String newDate = DateFormat.formatDate(new Date());
            Platform.runLater(() -> deepStorageBrowserPresenter.logText("PUT Job Cancelled (User Interruption)" + " at " + newDate, LogType.ERROR));
        } catch (final Exception e) {
            LOG.error("Encountered an error on a put job", e);
            final String newDate = DateFormat.formatDate(new Date());
            Platform.runLater(() -> deepStorageBrowserPresenter.logTextForParagraph("PUT Job Failed " + client.getConnectionDetails().getEndpoint() + ". Reason+" + e + " at " + newDate, LogType.ERROR));
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter.getJobProgressView(), jobId);
            final Session session = ds3Common.getCurrentSession().stream().findFirst().orElse(null);
            if (session != null) {
                final String currentSelectedEndpoint = session.getEndpoint() + ":" + session.getPortNo();
                if (currentSelectedEndpoint.equals(client.getConnectionDetails().getEndpoint())) {
                    ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);
                }
            }
        }

    }

    private String getDs3ObjectPath(final Path key, final Path value, final boolean contains, final int fileCount, final int dirCount) {
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

    public UUID getJobId() {
        return jobId;
    }
}
