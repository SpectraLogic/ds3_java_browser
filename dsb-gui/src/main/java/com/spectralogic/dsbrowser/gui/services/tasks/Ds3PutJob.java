/*
 * ****************************************************************************
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
 *  ****************************************************************************
 */
package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.metadata.MetadataAccessImpl;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.JobStatus;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.utils.FileUtils;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.Ds3PanelService;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TreeItem;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import static com.spectralogic.dsbrowser.api.services.logging.LogType.*;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Ds3PutJob extends Ds3JobTask {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PutJob.class);
    private static final String PUT = JobRequestType.PUT.toString();
    private final List<Pair<String, Path>> files;
    private final SettingsStore settings;
    private final String bucket;
    private final String targetDir;
    private final String jobPriority;
    private final int maximumNumberOfParallelThreads;
    private final JobInterruptionStore jobInterruptionStore;
    private final String targetDirectory;
    private final TreeItem<Ds3TreeTableValue> remoteDestination;
    private final DateTimeUtils dateTimeUtils;
    private final String delimiter;
    private final static String BP_DELIMITER = Constants.BP_DELIMITER;
    private final BooleanProperty isInCache = new SimpleBooleanProperty(false);
    public final BooleanProperty isVisible = new SimpleBooleanProperty(true);

    @Inject
    public Ds3PutJob(final Ds3Client client,
            @Assisted final List<Pair<String, Path>> files,
            @Assisted("bucket") final String bucket,
            @Assisted("targetDir") final String targetDir,
            @Assisted @Nullable final Ds3ClientHelpers.Job job,
            final JobInterruptionStore jobInterruptionStore,
            @Nullable @Named("jobPriority") final String jobPriority,
            @Named("jobWorkerThreadCount") final int maximumNumberOfParallelThreads,
            final ResourceBundle resourceBundle,
            final SettingsStore settings,
            final LoggingService loggingService,
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
            final DateTimeUtils dateTImeUtils,
            @Assisted final TreeItem<Ds3TreeTableValue> remoteDestination) {
        this.delimiter = FileSystems.getDefault().getSeparator();
        this.ds3Client = client;
        this.resourceBundle = resourceBundle;
        this.files = files;
        this.settings = settings;
        this.bucket = bucket;
        this.targetDir = targetDir;
        this.jobPriority = jobPriority;
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
        this.jobInterruptionStore = jobInterruptionStore;
        this.loggingService = loggingService;
        this.targetDirectory = bucket + BP_DELIMITER + targetDir;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.remoteDestination = remoteDestination;
        this.dateTimeUtils = dateTImeUtils;
        this.job = job;
        settings.getShowCachedJobSettings().showCachedJobEnableProperty().addListener((observable, oldValue, newValue) -> {
            if (isInCache.get()) {
                isVisible.set(newValue);
            } else {
                isVisible.set(true);
            }
        });
    }

    @Override
    public void executeJob() throws Exception {
        final boolean metadata = settings.getFilePropertiesSettings().isFilePropertiesEnabled();
        final boolean hasPriority = !Guard.isStringNullOrEmpty(jobPriority);
        final Instant jobStartInstant = Instant.now();
        final String startJobDate = dateTimeUtils.nowAsString();
        final String jobInitiateTitleMessage = buildJobInitiatedTitleMessage(startJobDate, ds3Client);
        final String transferringMessage = buildTransferringMessage(resourceBundle);

        LOG.info(resourceBundle.getString("putJobStarted"));
        updateTitle(resourceBundle.getString("blackPearlHealth"));

        if (!CheckNetwork.isReachable(ds3Client)) {
            hostNotAvailable();
            return;
        }
        updateTitle(jobInitiateTitleMessage);
        updateMessage(transferringMessage);

        final ImmutableMap.Builder<String, Path> fileMapBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, Path> folderMapBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, Path> fileMapperBuilder = ImmutableMap.builder();
        files.stream()
                .map(p -> {
                    try {
                        return new Pair<>(p.getKey().replace(delimiter, BP_DELIMITER), FileUtils.resolveForSymbolic(p.getValue()));
                    } catch (final IOException e) {
                        loggingService.logMessage("Could not read from filesystem, skipping item " + p.getValue().toString(), LogType.ERROR);
                        LOG.error("Could not read from filesystem", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(pair -> buildMaps(fileMapBuilder, folderMapBuilder, pair, loggingService, targetDir, delimiter));
        final ImmutableMap<String, Path> fileMap = fileMapBuilder.build();
        final ImmutableMap<String, Path> folderMap = folderMapBuilder.build();
        fileMapperBuilder.putAll(fileMap);
        fileMapperBuilder.putAll(folderMap);
        final ImmutableMap<String, Path> fileMapper = fileMapperBuilder.build();
        final ImmutableList<Ds3Object> objects = fileMapper.entrySet()
                .stream()
                .map(this::buildDs3Object)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(GuavaCollectors.immutableList());
        if (objects.isEmpty()) {
            loggingService.logMessage("Job was empty, not sending", LogType.INFO);
            return;
        }
        if (job == null) {
            this.job = Ds3ClientHelpers.wrap(ds3Client).startWriteJob(bucket, objects);
        }
        final long totalJobSize = getTotalJobSize();

        job.withMaxParallelRequests(maximumNumberOfParallelThreads);

        ParseJobInterruptionMap.saveValuesToFiles(jobInterruptionStore, fileMap, folderMap,
                ds3Client.getConnectionDetails().getEndpoint(), this.getJobId(), totalJobSize, targetDir, dateTimeUtils, PUT, bucket);

        updateMessage(StringBuilderUtil.transferringTotalJobString(FileSizeFormat.getFileSizeType(totalJobSize), targetDirectory).toString());

        if (hasPriority) {
            ds3Client.modifyJobSpectraS3(new ModifyJobSpectraS3Request(job.getJobId()).withPriority(Priority.valueOf(jobPriority)));
        }

        if (metadata) {
            LOG.info("Registering metadata access Implementation");
            job.withMetadata(new MetadataAccessImpl(fileMapper));
        }

        addWaitingForChunkListener(totalJobSize, bucket + StringConstants.DOUBLE_SLASH + targetDir);
        final AtomicLong totalSent = addDataTransferListener(totalJobSize);
        job.attachObjectCompletedListener(obj -> updateGuiOnComplete(startJobDate, jobStartInstant, totalJobSize, totalSent, obj));

        job.transfer(file -> FileChannel.open(PathUtil.resolveForSymbolic(fileMapper.get(file)), StandardOpenOption.READ));

        final Disposable d = waitForPermanentStorageTransfer(totalJobSize)
                .observeOn(JavaFxScheduler.platform())
                .doOnComplete(() -> {
                    LOG.info("Job transferred to permanent storage location");
                    loggingService.logMessage(resourceBundle.getString("jobFinished"), LogType.SUCCESS);
                    Ds3PanelService.throttledRefresh(remoteDestination);
                    ParseJobInterruptionMap.removeJobID(jobInterruptionStore, this.getJobId().toString(), ds3Client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter, loggingService);
                }).subscribe();
        while (!d.isDisposed()) {
            Thread.sleep(1000);
        }
    }

    private static void buildMaps(final ImmutableMap.Builder<String, Path> fileMapBuilder, final ImmutableMap.Builder<String, Path> folderMapBuilder, final Pair<String, Path> pair, final LoggingService loggingService, final String targetDir, final String delimiter) {
        final String name = pair.getKey();
        final Path path = pair.getValue();
        if (hasNestedItems(path)) {
            try {
                Files.walk(path).filter(child -> !hasNestedItems(child)).map(p -> new Pair<>((targetDir + name + delimiter + path.relativize(p).toString() + appendSlashWhenDirectory(p, delimiter)).replace(delimiter, BP_DELIMITER), p))
                        .forEach(p -> folderMapBuilder.put(p.getKey(), p.getValue()));
            } catch (final AccessDeniedException ae) {
                LOG.error("Access was denied to", ae);
                loggingService.logMessage("Access was denied while attempting to access " + path.toString(), LogType.ERROR);
            } catch (final SecurityException e) {
                LOG.error("Permission denied while accessing path", e);
                loggingService.logMessage("Tried to walk " + path.toString() + " but did not have permission", LogType.ERROR);
            } catch (final IOException e) {
                LOG.error("Unable to traverse provided path", e);
                loggingService.logMessage("Tried to walk " + path.toString() + " but could not", LogType.ERROR);
            }
        } else {
            fileMapBuilder.put(targetDir + name + appendSlashWhenDirectory(path, delimiter).replace(delimiter, BP_DELIMITER), path);
        }
    }

    @NotNull
    private static String appendSlashWhenDirectory(final Path path, final String delimiter) {
        return Files.isDirectory(path) ? delimiter : "";
    }

    private static boolean hasNestedItems(final Path path) {
        final boolean isDirectory = Files.isDirectory(path);
        final boolean isDirEmpty = isDirEmpty(path);
        return isDirectory && !isDirEmpty;
    }

    private long getTotalJobSize() throws IOException {
        return ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(this.getJobId())).getMasterObjectListResult().getOriginalSizeInBytes();
    }

    private void updateGuiOnComplete(final String newDate, final Instant jobStartInstant, final long totalJobSize, final AtomicLong totalSent, final String obj) {
        LOG.info("Object Transfer Completed");

        final int index = obj.lastIndexOf(StringConstants.FORWARD_SLASH);
        final String objectName = (index > 0) ? obj.substring(index, obj.length()) : obj;
        final String locationName = bucket + StringConstants.FORWARD_SLASH + targetDir;

        getTransferRates(jobStartInstant, totalSent, totalJobSize, objectName, locationName);

        loggingService.logMessage(
                StringBuilderUtil.objectSuccessfullyTransferredString(
                        objectName,
                        locationName, newDate,
                        resourceBundle.getString("blackPearlCache")).toString(), SUCCESS);
        Platform.runLater(() -> Ds3PanelService.throttledRefresh(remoteDestination));

    }

    private Optional<Ds3Object> buildDs3Object(final Map.Entry<String, Path> p) {
        try {
            return Optional.of(new Ds3Object(p.getKey(), Files.isDirectory(p.getValue()) ? 0 : Files.size(p.getValue())));
        } catch (final IOException e) {
            LOG.error("Unable to build Ds3Object", e);
            return Optional.empty();
        }
    }

    private static String buildTransferringMessage(final ResourceBundle resourceBundle) {
        return resourceBundle.getString("transferringEllipsis");
    }

    private static String buildJobInitiatedTitleMessage(final String startJobDate, final Ds3Client client) {
        return StringBuilderUtil.jobInitiatedString(PUT, startJobDate, client.getConnectionDetails().getEndpoint()).toString();
    }

    private Completable waitForPermanentStorageTransfer(final long totalJobSize) throws IOException, InterruptedException {
        final boolean isCacheJobEnable = settings.getShowCachedJobSettings().getShowCachedJob();
        final String dateOfTransfer = dateTimeUtils.nowAsString();
        final String finishedMessage = buildFinishedMessage(totalJobSize, isCacheJobEnable, dateOfTransfer, targetDirectory, resourceBundle);
        updateProgress(1, 1);
        updateMessage(finishedMessage);
        loggingService.logMessage(finishedMessage, SUCCESS);
        isInCache.set(true);

        return Observable.interval(60, TimeUnit.SECONDS)
                .takeUntil(event -> ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(this.job.getJobId())).getMasterObjectListResult().getStatus() == JobStatus.COMPLETED)
                .retry(throwable -> {
                    loggingService.logMessage("Error checking status of job " + getJobId() + " will retry", LogType.ERROR);
                    LOG.error("Unable to check status of job " + getJobId(), throwable);
                    return true;
                })
                .ignoreElements();
    }

    private static String buildFinishedMessage(final long totalJobSize, final boolean isCacheJobEnable, final String dateOfTransfer, final String targetDir, final ResourceBundle resourceBundle) {
        return StringBuilderUtil.jobSuccessfullyTransferredString(PUT,
                FileSizeFormat.getFileSizeType(totalJobSize),
                targetDir,
                dateOfTransfer,
                resourceBundle.getString("blackPearlCache"),
                isCacheJobEnable).toString();
    }

    @Override
    public UUID getJobId() {
        return job.getJobId();
    }

    private static boolean isDirEmpty(final Path directory) {
        try (DirectoryStream<Path> dStream = Files.newDirectoryStream(directory)) {
            return !dStream.iterator().hasNext();
        } catch (final IOException e) {
            return false;
        }
    }

    public interface Ds3PutJobFactory {
        Ds3PutJob createDs3PutJob(final List<Pair<String, Path>> pairs,
                @Assisted("bucket") final String bucket,
                @Assisted("targetDir") final String targetDir,
                final TreeItem<Ds3TreeTableValue> treeItem,
                final Ds3ClientHelpers.Job job);
    }
}
