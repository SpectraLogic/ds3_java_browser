package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Response;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.helpers.FileObjectGetter;
import com.spectralogic.ds3client.helpers.FileObjectPutter;
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.spectralogic.ds3client.models.RequestType.GET;

public class RecoverInterruptedJob extends Ds3JobTask {

    private static final Logger LOG = LoggerFactory.getLogger(RecoverInterruptedJob.class);

    private final UUID uuid;
    private final EndpointInfo endpointInfo;
    private final JobInterruptionStore jobInterruptionStore;
    private final Ds3Client ds3Client;
    private final ResourceBundle resourceBundle;
    private final boolean isCacheJobEnable;
    private boolean isFailed = false;

    public RecoverInterruptedJob(final UUID uuid, final EndpointInfo endpointInfo, final JobInterruptionStore jobInterruptionStore, final boolean isCacheJobEnable) {
        this.uuid = uuid;
        this.endpointInfo = endpointInfo;
        this.jobInterruptionStore = jobInterruptionStore;
        ds3Client = endpointInfo.getClient();
        this.isCacheJobEnable = isCacheJobEnable;
        this.resourceBundle = ResourceBundleProperties.getResourceBundle();
    }

    @Override
    public void executeJob() throws Exception {
        //job start time
        final Instant jobStartInstant = Instant.now();
        LOG.info("Recover Interrupted Job started");
        try {
            final FilesAndFolderMap filesAndFolderMap = endpointInfo.getJobIdAndFilesFoldersMap().get(uuid.toString());
            final String date = DateFormat.formatDate(new Date());
            updateTitle(JobStatusStrings.getRecoverJobTransferringForTitle(resourceBundle,filesAndFolderMap.getType(),endpointInfo.getEndpoint(),date));
            Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText(JobStatusStrings.getRecoverJobTransferringForLogs(resourceBundle,filesAndFolderMap.getType(),filesAndFolderMap.getDate()), LogType.INFO));
            final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(ds3Client, 100);
            Ds3ClientHelpers.Job job = null;
            Path fileTreeModel = null;
            if (filesAndFolderMap != null) {
                final Map<String, Path> filesMap = filesAndFolderMap.getFiles();
                final Map<String, Path> foldersMap = filesAndFolderMap.getFolders();
                final long totalJobSize = filesAndFolderMap.getTotalJobSize();
                if (filesAndFolderMap.getType().equals(JobRequestType.PUT.toString())) {
                    job = helpers.recoverWriteJob(uuid);
                    updateMessage(JobStatusStrings.getRecoverJobInitiateTransferTo(resourceBundle,job.getBucketName()));

                } else if (filesAndFolderMap.getType().equals(JobRequestType.GET.toString())) {
                    job = helpers.recoverReadJob(uuid);
                    fileTreeModel = Paths.get(filesAndFolderMap.getTargetLocation());
                    updateMessage(JobStatusStrings.getRecoverJobInitiateTransferFrom(resourceBundle,job.getBucketName()));
                }
                final AtomicLong totalSent = new AtomicLong(0L);
                job.attachDataTransferredListener(l -> {
                    updateProgress(totalSent.getAndAdd(l) / 2, totalJobSize);
                    totalSent.addAndGet(l);
                });
                job.attachObjectCompletedListener(s -> {
                    final Instant currentTime = Instant.now();
                    LOG.info("Object Transfer Completed" + s);
                    Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("successfullyTransferred") + StringConstants.SPACE + s + StringConstants.SPACE + resourceBundle.getString("to") + StringConstants.SPACE + filesAndFolderMap.getTargetLocation(), LogType.SUCCESS));
                    final long timeElapsedInSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTime.toEpochMilli() - jobStartInstant.toEpochMilli());
                    long transferRate = 0;
                    if (timeElapsedInSeconds != 0) {
                        transferRate = (totalSent.get() / 2) / timeElapsedInSeconds;
                    }
                    if (transferRate != 0) {
                        final long timeRemaining = (totalJobSize - (totalSent.get() / 2)) / transferRate;
                        updateMessage(JobStatusStrings.getTransferRateString(resourceBundle ,transferRate, timeRemaining, totalSent, totalJobSize, s, filesAndFolderMap.getTargetLocation()).toString());
                    } else {
                        updateMessage(JobStatusStrings.getTransferRateString(resourceBundle , transferRate, 0, totalSent, totalJobSize, s, filesAndFolderMap.getTargetLocation()).toString());
                    }

                });
                final Path finalFileTreeModel = fileTreeModel;
                // check whether chunk are available
                job.attachWaitingForChunksListener(retryAfterSeconds -> {
                    LOG.info("Chunk is not available retry time:" + retryAfterSeconds);
                    for (int retryTimeRemaining = retryAfterSeconds; retryTimeRemaining >= 0; retryTimeRemaining--) {
                        try {
                            updateMessage(resourceBundle.getString("noAvailableChunks") + StringConstants.SPACE + retryTimeRemaining + StringConstants.SPACE + resourceBundle.getString("seconds"));
                            Thread.sleep(1000);
                        } catch (final Exception e) {
                            LOG.error("Exception in attachWaitingForChunksListener" + e);
                        }
                    }
                    updateMessage(resourceBundle.getString("transferring") + StringConstants.SPACE + filesAndFolderMap.getType() + StringConstants.SPACE + resourceBundle.getString("jobOf") + StringConstants.SPACE + endpointInfo.getEndpoint() + StringConstants.SPACE + date);
                });
                job.transfer(obj -> {
                            if (filesAndFolderMap.getType().equals(JobRequestType.PUT.toString())) {
                                if (!Guard.isMapNullOrEmpty(filesMap) && filesMap.containsKey(obj)) {
                                    return new FileObjectPutter(filesMap.get(obj)).buildChannel(StringConstants.EMPTY_STRING);
                                } else {
                                    final Map.Entry<String, Path> stringPathEntry = foldersMap.entrySet().stream().filter(value -> obj.contains(value.getKey())).findFirst().get();
                                    final String restOfThePath = obj.replaceFirst(stringPathEntry.getKey(), StringConstants.EMPTY_STRING);
                                    final Path finalPath = Paths.get(stringPathEntry.getValue().toString(), restOfThePath);
                                    return new FileObjectPutter(finalPath).buildChannel(StringConstants.EMPTY_STRING);
                                }

                            } else {
                                if (filesAndFolderMap.isNonAdjacent())
                                    return new FileObjectGetter(finalFileTreeModel).buildChannel(obj);
                                else {
                                    final String skipPath = getSkipPath(obj,foldersMap);
                                    if (Guard.isStringNullOrEmpty(skipPath)) {
                                        return new FileObjectGetter(finalFileTreeModel).buildChannel(obj);
                                    } else {
                                        return new PrefixRemoverObjectChannelBuilder(new FileObjectGetter(finalFileTreeModel), skipPath).buildChannel(obj.substring(("/" + skipPath).length()));
                                    }
                                }

                            }
                        }
                );
                Platform.runLater(() -> {
                    if(filesAndFolderMap.getType().equals(GET.toString())) {
                        updateMessage(resourceBundle.getString("recovering") + StringConstants.SPACE + filesAndFolderMap.getType() + StringConstants.SPACE + JobStatusStrings.jobSuccessfullyTransferredString(resourceBundle, GET.toString(), FileSizeFormat.getFileSizeType(totalJobSize), filesAndFolderMap.getTargetLocation(), DateFormat.formatDate(new Date()), null, false).toString());
                        endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("recovering") + StringConstants.SPACE + filesAndFolderMap.getType() + StringConstants.SPACE + JobStatusStrings.jobSuccessfullyTransferredString(resourceBundle, GET.toString(), FileSizeFormat.getFileSizeType(totalJobSize), filesAndFolderMap.getTargetLocation(), DateFormat.formatDate(new Date()), null, false).toString(), LogType.SUCCESS);
                    }
                    else {
                        updateMessage(resourceBundle.getString("recovering") + StringConstants.SPACE + filesAndFolderMap.getType() + StringConstants.SPACE + JobStatusStrings.transferringTotalJobString(resourceBundle, FileSizeFormat.getFileSizeType(totalJobSize), filesAndFolderMap.getTargetLocation()).toString());
                        endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("recovering") + StringConstants.SPACE + filesAndFolderMap.getType() + StringConstants.SPACE + JobStatusStrings.transferringTotalJobString(resourceBundle, FileSizeFormat.getFileSizeType(totalJobSize), filesAndFolderMap.getTargetLocation()).toString(), LogType.SUCCESS);
                    }
                    updateProgress(totalJobSize, totalJobSize);
                });
                //Can not assign final.
                GetJobSpectraS3Response response = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(job.getJobId()));
                if (isCacheJobEnable && filesAndFolderMap.getType().equals(JobRequestType.PUT.toString())) {
                    while (!response.getMasterObjectListResult().getStatus().toString().equals(StringConstants.JOB_COMPLETED)) {
                        Thread.sleep(60000);
                        response = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(job.getJobId()));
                    }
                }
                Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("jobSize") + StringConstants.SPACE + FileSizeFormat.getFileSizeType(totalJobSize) + StringConstants.SPACE + resourceBundle.getString("recoveryCompleted") + StringConstants.SPACE + filesAndFolderMap.getTargetLocation() + StringConstants.SPACE + resourceBundle.getString("storageLocation"), LogType.SUCCESS));
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, uuid.toString(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter());
                final Session session = endpointInfo.getDs3Common().getCurrentSession();
                final String currentSelectedEndpoint = session.getEndpoint() + StringConstants.COLON + session.getPortNo();
                if (currentSelectedEndpoint.equals(session.getClient().getConnectionDetails().getEndpoint())) {
                    ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
                }
            } else {
                LOG.info("There is no interrupted job to be recovered");
            }
        } catch (final FailedRequestException e) {
            isFailed = true;
            LOG.error("Request to black pearl failed", e);
            Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("jobNotfound"), LogType.INFO));
            cancel();
        } catch (final Exception e) {
            isFailed = true;
            LOG.error("Encountered an exception when executing a job", e);
            Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("encounteredException") + e + resourceBundle.getString("userInterruption"), LogType.ERROR));
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getClient().getConnectionDetails().getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), uuid);
            final Session session = endpointInfo.getDs3Common().getCurrentSession();
            final String currentSelectedEndpoint = session.getEndpoint() + StringConstants.COLON + session.getPortNo();
            if (currentSelectedEndpoint.equals(endpointInfo.getClient().getConnectionDetails().getEndpoint())) {
                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
            }
        }
    }

    public Ds3Client getDs3Client() {
        return ds3Client;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isFailed() {
        return isFailed;
    }

    /**
     * Get the path needs to be skipped
     * @param obj object
     * @param foldersMap folder directory map
     * @return skipped path
     */
    public String getSkipPath(final String obj, final Map<String, Path> foldersMap) {
        final File file = new File(obj);
        if (Guard.isMapNullOrEmpty(foldersMap)) {
            if (file.getParent() != null)
                return file.getParent();
        }
        return  StringConstants.EMPTY_STRING;
    }
}
