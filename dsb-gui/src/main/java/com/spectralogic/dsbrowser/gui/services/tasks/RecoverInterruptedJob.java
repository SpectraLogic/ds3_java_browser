/* ****************************************************************************
 *    Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *  **************************************************************************** */

package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Response;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.helpers.FileObjectGetter;
import com.spectralogic.ds3client.helpers.FileObjectPutter;
import com.spectralogic.ds3client.helpers.JobRecoveryException;
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder;
import com.spectralogic.ds3client.metadata.MetadataAccessImpl;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.JobStatus;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.gui.util.StringBuilderUtil;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import io.reactivex.Completable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.application.Platform;
import javafx.util.Pair;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.spectralogic.ds3client.models.JobRequestType.*;
import static com.spectralogic.dsbrowser.api.services.logging.LogType.*;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RecoverInterruptedJob extends Ds3JobTask {

    private final static Logger LOG = LoggerFactory.getLogger(RecoverInterruptedJob.class);

    private final UUID uuid;
    private final EndpointInfo endpointInfo;
    private final ResourceBundle resourceBundle;
    private final SettingsStore settingsStore;
    private final DateTimeUtils dateTimeUtils;

    @Inject
    public RecoverInterruptedJob(
            @Assisted final UUID uuid,
            @Assisted final EndpointInfo endpointInfo,
            final Ds3Client client,
            final LoggingService loggingService,
            final SettingsStore settingsStore,
            final DateTimeUtils dateTimeUtils,
            final ResourceBundle resourceBundle
    ) {
        this.uuid = uuid;
        this.endpointInfo = endpointInfo;
        this.ds3Client = client;
        this.loggingService = loggingService;
        this.dateTimeUtils = dateTimeUtils;
        this.resourceBundle = resourceBundle;
        this.settingsStore = settingsStore;
    }

    @Override
    public void executeJob() throws Exception {
        final FilesAndFolderMap filesAndFolderMap = FilesAndFolderMap.buildFromEndpoint(endpointInfo, uuid);
        final JobRequestType jobRequestType = JobRequestType.valueOf(filesAndFolderMap.getType());
        this.job = getJob(ds3Client, uuid, jobRequestType, loggingService);
        if (job == null) {
            loggingService.logMessage("Could not get job " + uuid, LogType.ERROR);
            LOG.error("Could not get job " + uuid);
            return;
        }
        final UUID jobId = job.getJobId();
        final String bucketName = job.getBucketName();
        final String date = dateTimeUtils.nowAsString();
        final Instant jobStartInstant = Instant.now();
        final String targetLocation = filesAndFolderMap.getTargetLocation();
        final String jobDate = filesAndFolderMap.getDate();
        final String endpointName = endpointInfo.getEndpoint();
        final String message = buildMessage(jobRequestType, bucketName);
        final String titleMessage = buildTitle(date, jobRequestType, endpointName);
        final String logMessage = buildLogMessage(jobRequestType, jobDate);
        final Map<String, Path> filesMap = filesAndFolderMap.getFiles();
        final Map<String, Path> foldersMap = filesAndFolderMap.getFolders();
        final long totalJobSize = filesAndFolderMap.getTotalJobSize();
        final AtomicLong totalSent = addDataTransferListener(totalJobSize);
        final boolean isFilePropertiesEnabled = settingsStore.getFilePropertiesSettings().isFilePropertiesEnabled();
        final boolean isCacheJobEnable = settingsStore.getShowCachedJobSettings().getShowCachedJob();
        final String buildGetRecoveringMessage = buildGetRecoveringMessage(targetLocation, totalJobSize, resourceBundle, dateTimeUtils);
        final String buildPutRecoveringMessage = buildPutRecoveringMessage(targetLocation, totalJobSize, isCacheJobEnable, resourceBundle, dateTimeUtils);
        final String buildFinalMessage = buildFinalMessage(targetLocation, totalJobSize, resourceBundle);

        updateTitle(titleMessage);
        loggingService.logMessage(logMessage, INFO);

        updateMessage(message);

        job.attachObjectCompletedListener(s -> onCompleteListener(jobStartInstant, targetLocation, totalJobSize, totalSent, s));
        addWaitingForChunkListener(totalJobSize, targetLocation);

        final ImmutableMap.Builder<String, Path> folderMapBuilder = new ImmutableMap.Builder<>();
        foldersMap.forEach((name, path) -> {
            try {
                Files.walk(path).filter(child -> !hasNestedItems(child)).map(p -> new Pair<>(targetLocation + name + "/" + path.relativize(p).toString() + appendSlashWhenDirectory(p), p))
                     .forEach(p -> folderMapBuilder.put(p.getKey(), p.getValue()));
            } catch (final SecurityException ex) {
                loggingService.logMessage("Unable to access path, please check permssions on " + path.toString(), LogType.ERROR);
                LOG.error("Unable to access path", ex);
            } catch (final IOException ex) {
                loggingService.logMessage("Unable to read path " + path.toString(), LogType.ERROR);
                LOG.error("Unable to read path", ex);
            }
        });

        folderMapBuilder.putAll(filesMap);
        final ImmutableMap<String, Path> allMap = folderMapBuilder.build();

        if (isFilePropertiesEnabled && !filesMap.isEmpty()) {
            LOG.info("Registering metadata access Implementation");
            job.withMetadata(new MetadataAccessImpl(allMap));
        }

        job.attachDataTransferredListener(l -> setDataTransferredListener(l, totalJobSize, totalSent));

        job.transfer(objectName -> buildTransfer(targetLocation, jobRequestType, filesMap, foldersMap, objectName));

        Platform.runLater(() -> {
            updateProgressAndLog(jobRequestType, totalJobSize, totalSent, buildGetRecoveringMessage, buildPutRecoveringMessage);
        });


        waitForTransfer(filesAndFolderMap, jobId, isCacheJobEnable, ds3Client)
                .observeOn(JavaFxScheduler.platform())
                .doOnError(throwable -> {
                    LOG.error("Encountered a failure while waiting for transfer", throwable);
                })
                .doOnComplete(() -> {
                    loggingService.logMessage(buildFinalMessage, SUCCESS);
                    //removeJobIdAndUpdateJobsBtn(jobInterruptionStore, uuid);
                })
                .subscribe();
    }

    private void setDataTransferredListener(final long l, final Long totalJobSize, final AtomicLong totalSent) {
        updateProgress(totalSent.getAndAdd(l) / 2, totalJobSize);
        totalSent.addAndGet(l);
    }

    private static Completable waitForTransfer(final FilesAndFolderMap filesAndFolderMap, final UUID jobId, final boolean isCacheJobEnable, final Ds3Client ds3Client) throws IOException, InterruptedException {
        final GetJobSpectraS3Response response = ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
        if (cacheIsEnabledAndIsPut(filesAndFolderMap, isCacheJobEnable)) {
            return retryJob(response, ds3Client, jobId);
        }
        return Completable.complete();
    }

    private void updateProgressAndLog(final JobRequestType jobRequestType, final long totalJobSize, final AtomicLong totalSent, final String buildGetRecoveringMessage, final String buildPutRecoveringMessage) {
        if (jobRequestType == GET) {
            updateMessage(buildGetRecoveringMessage);
            loggingService.logMessage(buildGetRecoveringMessage, SUCCESS);
        } else {
            updateMessage(buildPutRecoveringMessage);
            loggingService.logMessage(buildPutRecoveringMessage, SUCCESS);
        }
        updateProgress(totalSent.get(), totalJobSize);
    }

    private static String buildFinalMessage(final String targetLocation, final long totalJobSize, final ResourceBundle resourceBundle) {
        return resourceBundle.getString("jobSize")
                + StringConstants.SPACE + FileSizeFormat.getFileSizeType(totalJobSize) + StringConstants.SPACE
                + resourceBundle.getString("recoveryCompleted") + StringConstants.SPACE
                + targetLocation + StringConstants.SPACE
                + resourceBundle.getString("storageLocation");
    }

    private static Completable retryJob(final GetJobSpectraS3Response response, final Ds3Client ds3Client, final UUID jobId) throws InterruptedException, IOException {
        if (response.getMasterObjectListResult().getStatus() == JobStatus.COMPLETED) {
            return Completable.complete();
        }
        return Observable.interval(60, TimeUnit.SECONDS)
                .takeUntil(event -> ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(jobId)).getMasterObjectListResult().getStatus() == JobStatus.COMPLETED)
                .ignoreElements();
    }

    private static boolean cacheIsEnabledAndIsPut(final FilesAndFolderMap filesAndFolderMap, final boolean isCacheJobEnable) {
        return isCacheJobEnable && filesAndFolderMap.getType().equals(PUT.toString());
    }

    private static String buildPutRecoveringMessage(final String targetLocation, final long totalJobSize, final boolean isCacheJobEnable, final ResourceBundle resourceBundle, final DateTimeUtils dateTimeUtils) {
        return resourceBundle.getString("recovering") + StringConstants.SPACE
                + StringBuilderUtil.jobSuccessfullyTransferredString(PUT.toString(), FileSizeFormat.getFileSizeType(totalJobSize),
                targetLocation, dateTimeUtils.nowAsString(), resourceBundle.getString("blackPearlCache"), isCacheJobEnable);
    }

    private static String buildGetRecoveringMessage(final String targetLocation, final long totalJobSize, final ResourceBundle resourceBundle, final DateTimeUtils dateTimeUtils) {
        return resourceBundle.getString("recovering") + StringConstants.SPACE
                + StringBuilderUtil.jobSuccessfullyTransferredString(GET.toString(),
                FileSizeFormat.getFileSizeType(totalJobSize), targetLocation,
                dateTimeUtils.nowAsString(), null, false).toString();
    }

    private static SeekableByteChannel buildTransfer(final String targetLocation, final JobRequestType jobRequestType, final Map<String, Path> filesMap, final Map<String, Path> foldersMap, final String objectName) throws IOException {
        if (jobRequestType == PUT) {
            if (!Guard.isMapNullOrEmpty(filesMap) && filesMap.containsKey(objectName)) {
                return new FileObjectPutter(filesMap.get(objectName)).buildChannel(StringConstants.EMPTY_STRING);
            } else {
                return new FileObjectPutter(getFinalJobPath(foldersMap, objectName)).buildChannel(StringConstants.EMPTY_STRING);
            }
        } else {
            final String skipPath = getSkipPath(objectName, foldersMap);
            if (Guard.isStringNullOrEmpty(skipPath)) {
                return new FileObjectGetter(Paths.get(targetLocation)).buildChannel(objectName);
            } else {
                return new PrefixRemoverObjectChannelBuilder(
                        new FileObjectGetter(Paths.get(targetLocation)), skipPath)
                        .buildChannel(objectName.substring((StringConstants.FORWARD_SLASH + skipPath).length()));
            }
        }

    }

    private static String getSkipPath(final String obj, final Map<String, Path> foldersMap) {
        final File file = new File(obj);
        if (Guard.isMapNullOrEmpty(foldersMap)) {
            if (file.getParent() != null) {
                return file.getParent();
            }
        }
        return StringConstants.EMPTY_STRING;
    }

    private static Path getFinalJobPath(final Map<String, Path> foldersMap, final String obj) {
        final String startOfPath;
        if (foldersMap.isEmpty()) {
            startOfPath = "";
        } else {
            startOfPath = foldersMap.keySet().toArray(new String[1])[0];
        }

        final int last = startOfPath.lastIndexOf('/');
        final String result;
        if (last > 0) {
            result = startOfPath.substring(0, last);
        } else {
            result = "/";
        }
        final Path resultPath = Paths.get("/", result, obj);
        return resultPath;
    }

    private void onCompleteListener(final Instant jobStartInstant, final String targetLocation, final long totalJobSize, final AtomicLong totalSent, final String s) {
        LOG.info("Object Transfer Completed {}", s);
        getTransferRates(jobStartInstant, totalSent, totalJobSize, s, targetLocation);
        loggingService.logMessage(
                buildSuccessMessage(targetLocation, s, resourceBundle), SUCCESS);
    }

    private static String buildSuccessMessage(final String targetLocation, final String s, final ResourceBundle resourceBundle) {
        return resourceBundle.getString("successfullyTransferred")
                + StringConstants.SPACE + s + StringConstants.SPACE
                + resourceBundle.getString("to") + StringConstants.SPACE
                + targetLocation;
    }

    private static String buildLogMessage(final JobRequestType type, final String date) {
        return StringBuilderUtil.getRecoverJobTransferringForLogs(type.toString(), date).toString();
    }

    private static String buildTitle(final String date, final JobRequestType type, final String endpointName) {
        return StringBuilderUtil.getRecoverJobTransferringForTitle(type.toString(), endpointName, date).toString();
    }

    private static String buildMessage(final JobRequestType jobRequestType, final String bucketName) {
        switch (jobRequestType) {
            case PUT:
                return StringBuilderUtil.getRecoverJobInitiateTransferTo(bucketName).toString();
            case GET:
                return StringBuilderUtil.getRecoverJobInitiateTransferFrom(bucketName).toString();
            default:
                return "ERROR";
        }
    }

    private static Ds3ClientHelpers.Job getJob(final Ds3Client ds3Client, final UUID uuid, final JobRequestType jobRequestType, final LoggingService loggingService) {
        switch (jobRequestType) {
            case PUT:
                return buildWriteJob(ds3Client, uuid, loggingService);
            case GET:
                return buildReadJob(ds3Client, uuid);
            default:
                return null;
        }
    }

    private static Ds3ClientHelpers.Job buildWriteJob(final Ds3Client ds3Client, final UUID uuid, final LoggingService loggingService) {
        final String uuidText = uuid.toString();
        try {
            return Ds3ClientHelpers.wrap(ds3Client, 100).recoverWriteJob(uuid);
        } catch (final IOException e) {
            loggingService.logMessage("Unable to performe IO for " + uuidText, ERROR);
            LOG.error("Unable to build write job", e);
        } catch (final JobRecoveryException e) {
            loggingService.logMessage("Unable to recover job for " + uuidText, ERROR);
            LOG.error("Unable to build write job", e);
        }
        return null;
    }

    private static Ds3ClientHelpers.Job buildReadJob(final Ds3Client ds3Client, final UUID uuid) {
        try {
            return Ds3ClientHelpers.wrap(ds3Client, 100).recoverReadJob(uuid);
        } catch (final IOException|JobRecoveryException e) {
            LOG.error("Failed to create read job", e);
        }
        return null;
    }

    private static boolean hasNestedItems(final Path path) {
        final boolean isDirectory = Files.isDirectory(path);
        final boolean isDirEmpty = isDirEmpty(path);
        return isDirectory && !isDirEmpty;
    }

    private static boolean isDirEmpty(final Path directory) {
        try (final DirectoryStream<Path> dStream = Files.newDirectoryStream(directory)) {
            return !dStream.iterator().hasNext();
        } catch (final IOException e) {
            return false;
        }
    }

    private static String appendSlashWhenDirectory(final Path path) {
        return Files.isDirectory(path) ? "/" : "";
    }


    @Override
    public UUID getJobId() {
        return uuid;
    }

    public interface RecoverInterruptedJobFactory {
        RecoverInterruptedJob createRecoverInterruptedJob(final UUID uuid, final EndpointInfo endpointInfo);
    }
}
