package com.spectralogic.dsbrowser.gui.services.tasks;

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
import java.util.concurrent.atomic.AtomicLong;

import static com.spectralogic.ds3client.models.JobRequestType.PUT;
import static com.spectralogic.ds3client.models.RequestType.GET;

public class RecoverInterruptedJob extends Ds3JobTask {

    private static final Logger LOG = LoggerFactory.getLogger(RecoverInterruptedJob.class);

    private final UUID uuid;
    private final EndpointInfo endpointInfo;
    private final JobInterruptionStore jobInterruptionStore;
    private final ResourceBundle resourceBundle;
    private final boolean isCacheJobEnable;

    public RecoverInterruptedJob(final UUID uuid, final EndpointInfo endpointInfo, final JobInterruptionStore jobInterruptionStore, final boolean isCacheJobEnable) {
        this.uuid = uuid;
        this.endpointInfo = endpointInfo;
        this.ds3Common = endpointInfo.getDs3Common();
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
            updateTitle(StringBuilderUtil.getRecoverJobTransferringForTitle(filesAndFolderMap.getType(), endpointInfo
                    .getEndpoint(), date).toString());
            Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText(
                    StringBuilderUtil.getRecoverJobTransferringForLogs(filesAndFolderMap.getType(),
                            filesAndFolderMap.getDate()).toString(), LogType.INFO));

            if (filesAndFolderMap != null) {
                final Map<String, Path> filesMap = filesAndFolderMap.getFiles();
                final Map<String, Path> foldersMap = filesAndFolderMap.getFolders();
                final long totalJobSize = filesAndFolderMap.getTotalJobSize();

                getJob(filesAndFolderMap);
                final AtomicLong totalSent = addDataTransferListener(totalJobSize);

                job.attachObjectCompletedListener(s -> {
                    LOG.info("Object Transfer Completed{}", s);
                    getTransferRates(jobStartInstant, totalSent, totalJobSize, s, filesAndFolderMap.getTargetLocation());

                    Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText(
                            resourceBundle.getString("successfullyTransferred")
                                    + StringConstants.SPACE + s + StringConstants.SPACE
                                    + resourceBundle.getString("to") + StringConstants.SPACE
                                    + filesAndFolderMap.getTargetLocation(), LogType.SUCCESS));

                });
                // check whether chunk are available
                addWaitingForChunkListener(totalJobSize, filesAndFolderMap.getTargetLocation().toString());

                job.transfer(obj -> {
                            if (filesAndFolderMap.getType().equals(PUT.toString())) {
                                if (!Guard.isMapNullOrEmpty(filesMap) && filesMap.containsKey(obj)) {
                                    return new FileObjectPutter(filesMap.get(obj)).buildChannel(StringConstants.EMPTY_STRING);
                                } else {
                                    return new FileObjectPutter(getFinalJobPath(foldersMap, obj))
                                            .buildChannel(StringConstants.EMPTY_STRING);
                                }

                            } else {
                                if (filesAndFolderMap.isNonAdjacent()) {
                                    return new FileObjectGetter(Paths.get(filesAndFolderMap.getTargetLocation())).buildChannel(obj);
                                }
                                else {
                                    final String skipPath = getSkipPath(obj, foldersMap);
                                    if (Guard.isStringNullOrEmpty(skipPath)) {
                                        return new FileObjectGetter(Paths.get(filesAndFolderMap.getTargetLocation())).buildChannel(obj);
                                    } else {
                                        return new PrefixRemoverObjectChannelBuilder(
                                                new FileObjectGetter(Paths.get(filesAndFolderMap.getTargetLocation())), skipPath)
                                                .buildChannel(obj.substring((StringConstants.FORWARD_SLASH + skipPath).length()));
                                    }
                                }

                            }
                        }
                );

                Platform.runLater(() -> {
                    if (filesAndFolderMap.getType().equals(GET.toString())) {
                        updateMessage(resourceBundle.getString("recovering") + StringConstants.SPACE
                                + StringBuilderUtil.jobSuccessfullyTransferredString(GET.toString(),
                                FileSizeFormat.getFileSizeType(totalJobSize), filesAndFolderMap.getTargetLocation(),
                                DateFormat.formatDate(new Date()), null, false).toString());
                        endpointInfo.getDeepStorageBrowserPresenter().logText(
                                resourceBundle.getString("recovering") + StringConstants.SPACE
                                        + StringBuilderUtil.jobSuccessfullyTransferredString(GET.toString(),
                                        FileSizeFormat.getFileSizeType(totalJobSize), filesAndFolderMap.getTargetLocation(),
                                        DateFormat.formatDate(new Date()), null, false).toString(), LogType.SUCCESS);
                    } else {
                        updateMessage(resourceBundle.getString("recovering") + StringConstants.SPACE
                                + StringBuilderUtil.jobSuccessfullyTransferredString(PUT.toString(), FileSizeFormat.getFileSizeType(totalJobSize),
                                filesAndFolderMap.getTargetLocation(), DateFormat.formatDate(new Date()), resourceBundle.getString("blackPearlCache"), isCacheJobEnable));
                        endpointInfo.getDeepStorageBrowserPresenter().logText(
                                resourceBundle.getString("recovering") + StringConstants.SPACE
                                        + StringBuilderUtil.jobSuccessfullyTransferredString(PUT.toString(), FileSizeFormat.getFileSizeType(totalJobSize),
                                        filesAndFolderMap.getTargetLocation(), DateFormat.formatDate(new Date()), resourceBundle.getString("blackPearlCache"), isCacheJobEnable), LogType.SUCCESS);
                    }
                    updateProgress(totalJobSize, totalJobSize);
                });

                //Can not assign final.
                GetJobSpectraS3Response response = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(job.getJobId()));
                if (isCacheJobEnable && filesAndFolderMap.getType().equals(PUT.toString())) {
                    while (!response.getMasterObjectListResult().getStatus().toString().equals(StringConstants.JOB_COMPLETED)) {
                        Thread.sleep(60000);
                        response = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(job.getJobId()));
                    }
                }
                Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("jobSize")
                        + StringConstants.SPACE + FileSizeFormat.getFileSizeType(totalJobSize) + StringConstants.SPACE
                        + resourceBundle.getString("recoveryCompleted") + StringConstants.SPACE
                        + filesAndFolderMap.getTargetLocation() + StringConstants.SPACE
                        + resourceBundle.getString("storageLocation"), LogType.SUCCESS));
                removeJobIdAndUpdateJobsBtn(jobInterruptionStore , uuid);
            } else {
                LOG.info("There is no interrupted job to be recovered");
            }
        } catch (final FailedRequestException e) {
            isJobFailed = true;
            LOG.error("Request to black pearl failed", e);
            Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText(
                    resourceBundle.getString("jobNotfound"), LogType.INFO));
            cancel();
        } catch (final Exception e) {
            isJobFailed = true;
            LOG.error("Encountered an exception when executing a job", e);
            Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText(
                    resourceBundle.getString("encounteredException") + e
                            + resourceBundle.getString("userInterruption"), LogType.ERROR));
            updateInterruptedJobsBtn(jobInterruptionStore , uuid);
        }
    }

    private void getJob(final FilesAndFolderMap filesAndFolderMap) throws Exception {
        final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(ds3Client, 100);
        if (filesAndFolderMap.getType().equals(PUT.toString())) {
            job = helpers.recoverWriteJob(uuid);
            updateMessage(StringBuilderUtil.getRecoverJobInitiateTransferTo(job.getBucketName()).toString());

        } else if (filesAndFolderMap.getType().equals(JobRequestType.GET.toString())) {
            job = helpers.recoverReadJob(uuid);
            updateMessage(StringBuilderUtil.getRecoverJobInitiateTransferFrom(job.getBucketName()).toString());
        }
    }

    private Path getFinalJobPath(final Map<String, Path> foldersMap, final String obj) {
        final Map.Entry<String, Path> stringPathEntry = foldersMap.entrySet().stream().filter(value ->
                obj.contains(value.getKey())).findFirst().get();
        final String restOfThePath = obj.replaceFirst(stringPathEntry.getKey(), StringConstants.EMPTY_STRING);
        return Paths.get(stringPathEntry.getValue().toString(), restOfThePath);
    }

    public UUID getUuid() {
        return uuid;
    }

    /**
     * Get the path needs to be skipped
     *
     * @param obj        object
     * @param foldersMap folder directory map
     * @return skipped path
     */
    public String getSkipPath(final String obj, final Map<String, Path> foldersMap) {
        final File file = new File(obj);
        if (Guard.isMapNullOrEmpty(foldersMap)) {
            if (file.getParent() != null) {
                return file.getParent();
            }
        }
        return StringConstants.EMPTY_STRING;
    }
}
