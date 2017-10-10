/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.tasks;


import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.helpers.FileObjectGetter;
import com.spectralogic.ds3client.helpers.MetadataReceivedListener;
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder;
import com.spectralogic.ds3client.metadata.MetadataReceivedListenerImpl;
import com.spectralogic.ds3client.models.Contents;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.networking.Metadata;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("Guava")
public class Ds3GetJob extends Ds3JobTask {
    private static final int RETRY_AFTER = 100;
    private final static String BP_DELIMITER = Constants.BP_DELIMITER;
    private final static Logger LOG = LoggerFactory.getLogger(Ds3GetJob.class);

    private final List<Ds3TreeTableValueCustom> selectedItems;
    private final Ds3ClientHelpers wrappedDs3Client;
    private final Path fileTreePath;
    private final Ds3Client client;
    private final AtomicLong totalSent = new AtomicLong(0L);
    private final MetadataReceivedListener metadataReceivedListener;
    private final JobInterruptionStore jobInterruptionStore;
    private final int maximumNumberOfParallelThreads;
    private final String jobPriority;
    private UUID jobId;
    private final DateTimeUtils dateTimeUtils;
    private final String delimiter;
    private final SettingsStore settingsStore;

    @Inject
    public Ds3GetJob(@Assisted final List<Ds3TreeTableValueCustom> selectedItems,
            @Assisted final Path fileTreePath,
            final Ds3Client client,
            @Nullable @Named("jobPriority") final String jobPriority,
            @Named("jobWorkerThreadCount") final int maximumNumberOfParallelThreads,
            final JobInterruptionStore jobInterruptionStore,
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
            final ResourceBundle resourceBundle,
            final DateTimeUtils dateTimeUtils,
            final SettingsStore settingsStore,
            final LoggingService loggingService) {
        this.delimiter = FileSystems.getDefault().getSeparator();
        this.selectedItems = selectedItems;
        this.fileTreePath = fileTreePath;
        this.client = client;
        this.wrappedDs3Client = Ds3ClientHelpers.wrap(client, RETRY_AFTER);
        this.resourceBundle = resourceBundle;
        this.loggingService = loggingService;
        this.jobPriority = jobPriority;
        this.jobInterruptionStore = jobInterruptionStore;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
        this.dateTimeUtils = dateTimeUtils;
        this.settingsStore = settingsStore;
        this.metadataReceivedListener = new MetadataReceivedListenerImpl(fileTreePath.toString());
    }

    private static boolean isEmptyDirectory(final Ds3Object object, final String delimiter) {
        return (object.getSize() == 0) && (object.getName().endsWith(delimiter));
    }

    @SuppressWarnings("Guava")
    @Override
    public void executeJob() throws Exception {
        if (!CheckNetwork.isReachable(client)) {
            hostNotAvailable();
            return;
        }
        final String startJobDate = dateTimeUtils.nowAsString();
        final ImmutableMap<String, Path> fileMap = getFileMap(selectedItems);
        final ImmutableMap<String, Path> folderMap = getFolderMap(selectedItems);
        updateTitle(getJobStart(startJobDate, client));
        loggingService.logMessage(getJobStart(startJobDate, client), LogType.INFO);
        updateMessage(resourceBundle.getString("transferringEllipsis"));
        selectedItems.stream()
                .filter(Objects::nonNull)
                .map(Ds3TreeTableValueCustom::getBucketName).distinct()
                .forEach(bucketName -> selectedItems.stream()
                        .filter(item -> bucketName.equals(item.getBucketName()))
                        .forEach(selectedItem -> transferFromSelectedItem(bucketName, selectedItem, fileMap, folderMap)));
    }

    private void transferFromSelectedItem(final String bucketName,
            final Ds3TreeTableValueCustom selectedItem,
            final ImmutableMap<String, Path> fileMap,
            final ImmutableMap<String, Path> folderMap) {
        final Instant startTime = Instant.now();
        final String fileName = selectedItem.getName();
        final String prefix = getParent(selectedItem.getFullName(), BP_DELIMITER);
        final FluentIterable<Ds3Object> ds3Objects = getDS3Objects(bucketName, selectedItem);
        final long totalJobSize = getTotalJobSize(ds3Objects);
        final Ds3ClientHelpers.Job job;
        if (ds3Objects.isEmpty()) {
            LOG.info("Did not create job because items were empty");
            return;
        }
        ds3Objects.filter(ds3 -> isEmptyDirectory(ds3, BP_DELIMITER)).forEach(ds3Object -> Ds3GetJob.buildEmptyDirectories(ds3Object, fileTreePath, loggingService));
        try {
            job = getJobFromIterator(bucketName, ds3Objects.filter(ds3Object -> !isEmptyDirectory(ds3Object, delimiter)));
        } catch (final IOException e) {
            LOG.error("Unable to get Jobs", e);
            loggingService.logMessage("Unable to get jobs from BlackPerl", LogType.ERROR);
            return;
        }
        if (job != null) {
            jobId = job.getJobId();
        } else {
            return;
        }
        ParseJobInterruptionMap.saveValuesToFiles(jobInterruptionStore, fileMap, folderMap,
                client.getConnectionDetails().getEndpoint(), jobId, totalJobSize, fileTreePath.toString(), dateTimeUtils,
                "GET", bucketName);
        updateMessage(getTransferringMessage(totalJobSize));
        attachListenersToJob(startTime, totalJobSize, job);
        try {
            notifyIfOverwriting(fileName);
            if (job != null) {
                job.transfer(getTransferJob(prefix));
            } else {
                throw new IOException("Something went wrong getting the job");
            }
        } catch (final InvalidPathException ipe) {
            loggingService.logMessage("File name " + fileName + "contains an illegal character", LogType.ERROR);
            LOG.error("File name " + fileName + " contains an illegal character", ipe);
        } catch (final IOException e) {
            loggingService.logMessage("Unable to transfer Job", LogType.ERROR);
            LOG.error("Unable to transfer Job", e);
        }
        updateUI(totalJobSize);
        ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(),
                client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter, loggingService);
    }

    private void updateUI(final long totalJobSize) {
        updateProgress(totalJobSize, totalJobSize);
        updateMessage(getJobTransferred(totalJobSize));
        updateProgress(totalJobSize, totalJobSize);
        loggingService.logMessage(StringBuilderUtil.getJobCompleted(totalJobSize, client, dateTimeUtils.nowAsString()).toString(), LogType.SUCCESS);
    }

    private void attachListenersToJob(final Instant startTime, final long totalJobSize, final Ds3ClientHelpers.Job job) {
        job.attachObjectCompletedListener(o -> setObjectCompleteListener(o, startTime, totalJobSize));
        if (settingsStore.getFilePropertiesSettings().isFilePropertiesEnabled()) {
            job.attachMetadataReceivedListener(this::setMetadataReceivedListener);
        }
        job.attachWaitingForChunksListener(this::setWaitingForChunksListener);
        job.attachDataTransferredListener(l -> setDataTransferredListener(l, totalJobSize));
    }

    private Ds3ClientHelpers.ObjectChannelBuilder getTransferJob(final String prefix) throws IOException {
        final Ds3ClientHelpers.ObjectChannelBuilder objectChannelBuilder;
        final FileObjectGetter fileObjectGetter = new FileObjectGetter(fileTreePath);
        if (prefix.isEmpty()) {
            objectChannelBuilder = fileObjectGetter;
        } else {
            objectChannelBuilder = new PrefixRemoverObjectChannelBuilder(fileObjectGetter, prefix + BP_DELIMITER);
        }
        return objectChannelBuilder;
    }

    private void notifyIfOverwriting(final String name) {
        try {
            if (Files.exists(fileTreePath.resolve(name))) {
                loggingService.logMessage(resourceBundle.getString("fileOverridden")
                        + StringConstants.SPACE + name + StringConstants.SPACE
                        + resourceBundle.getString("to")
                        + StringConstants.SPACE + fileTreePath, LogType.SUCCESS);
            }
        } catch (final InvalidPathException ipe) {
            LOG.error("Invalid character in path " + name, ipe);
            loggingService.logMessage("Invalid character in path " + name, LogType.ERROR);
        }
    }

    private Ds3ClientHelpers.Job getJobFromIterator(final String bucketName, final FluentIterable<Ds3Object> obj) throws IOException {
        if (obj.isEmpty()) {
            return null;
        }
        final Ds3ClientHelpers.Job job = wrappedDs3Client.startReadJob(bucketName, obj).withMaxParallelRequests(maximumNumberOfParallelThreads);
        if (Objects.nonNull(jobPriority)) {
            client.modifyJobSpectraS3(new ModifyJobSpectraS3Request(job.getJobId()).withPriority(com.spectralogic.ds3client.models.Priority.valueOf(jobPriority)));
        }
        return job;
    }

    private String getTransferringMessage(final long totalJobSize) {
        return StringBuilderUtil.transferringTotalJobString(FileSizeFormat.getFileSizeType(totalJobSize),
                fileTreePath.toString()).toString();
    }

    private String getJobTransferred(final long totalJobSize) {
        return StringBuilderUtil.jobSuccessfullyTransferredString(JobRequestType.GET.toString(),
                FileSizeFormat.getFileSizeType(totalJobSize), fileTreePath.toString(),
                dateTimeUtils.nowAsString(), null, false).toString();
    }

    private static String getJobStart(final String startJobDate, final Ds3Client client) {
        return StringBuilderUtil.jobInitiatedString(JobRequestType.GET.toString(), startJobDate, client.getConnectionDetails().getEndpoint()).toString();
    }

    private void setMetadataReceivedListener(final String string, final Metadata metadata) {
        LOG.info("Restoring metadata for {}", string);
        try {
            metadataReceivedListener.metadataReceived(string, metadata);
        } catch (final Exception e) {
            LOG.error("Error in metadata receiving", e);
        }
    }

    private static long getTotalJobSize(final FluentIterable<Ds3Object> obj) {
        return obj.stream()
                .mapToLong(Ds3Object::getSize)
                .sum();
    }

    public FluentIterable<Ds3Object> getDS3Objects(final String bucketName, final Ds3TreeTableValueCustom selectedItem) {
        if (selectedItem.getType() == Ds3TreeTableValue.Type.Directory) {
            Iterable<Contents> c;
            try {
                c = wrappedDs3Client.listObjects(bucketName, selectedItem.getFullName());
            } catch (final IOException e) {
                LOG.error("Failed to list objects", e);
                loggingService.logMessage("Failed to list objects for " + bucketName, LogType.ERROR);
                c = FluentIterable.from(Collections.emptyList());
            }
            return FluentIterable.from(c).transform(contents -> {
                if (contents != null) {
                    return new Ds3Object(contents.getKey(), contents.getSize());
                } else {
                    return null;
                }
            });
        } else {
            return FluentIterable.from(ImmutableList.of(new Ds3Object((selectedItem.getFullName()))));
        }
    }

    @Override
    public UUID getJobId() {
        return jobId;
    }

    private void setWaitingForChunksListener(final int retryAfterSeconds) {
        try {
            loggingService.logMessage("Waiting for chunks, will try again in " + DateTimeUtils.timeConversion(retryAfterSeconds), LogType.INFO);
            Thread.sleep(1000 * retryAfterSeconds);
        } catch (final InterruptedException e) {
            LOG.error("Did not receive chunks before timeout", e);
            loggingService.logMessage("Did not receive chunks before timeout", LogType.ERROR);
        }
    }

    private void setDataTransferredListener(final long l, final Long totalJobSize) {
        updateProgress(totalSent.getAndAdd(l) / 2, totalJobSize);
        totalSent.addAndGet(l);
    }

    private void setObjectCompleteListener(final String o, final Instant startTime, final long totalJobSize) {
        getTransferRates(startTime, totalSent, totalJobSize, o, fileTreePath.toString());
        loggingService.logMessage(StringBuilderUtil.objectSuccessfullyTransferredString(o, fileTreePath.toString(), dateTimeUtils.nowAsString(), null).toString(), LogType.SUCCESS);
    }

    private static String getParent(final String path, final String delimiter) {
        String newPath = path;
        if (newPath.endsWith(delimiter)) {
            newPath = newPath.substring(0, newPath.length() - 1);
        }
        final int lastIndexOf = newPath.lastIndexOf(delimiter);
        if (lastIndexOf < 1) {
            return "";
        } else {
            return newPath.substring(0, lastIndexOf);
        }
    }

    private static void buildEmptyDirectories(final Ds3Object emtpyDir, final Path fileTreePath, final LoggingService loggingService) {
        try {
            final Path directoryPath = fileTreePath.resolve(emtpyDir.getName());
            Files.createDirectories(directoryPath);
        } catch (final InvalidPathException ipe) {
            LOG.error("Invalid character in " + emtpyDir.getName(), ipe);
            loggingService.logMessage("Invalid character in" + emtpyDir.getName(), LogType.ERROR);
        } catch (final IOException e) {
            final String pathString = emtpyDir.getName();
            LOG.error("Could not create " + pathString, e);
            loggingService.logMessage("Could not create " + pathString, LogType.ERROR);
        }
    }

    public static ImmutableMap<String, Path> getFileMap(final List<Ds3TreeTableValueCustom> selectedItems) {
        final ImmutableList<Ds3TreeTableValueCustom> fileList = selectedItems.stream().filter(value ->
                value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());
        final ImmutableMap.Builder<String, Path> fileMap = ImmutableMap.builder();
        fileList.forEach(file -> {
            fileMap.put(file.getFullName(), Paths.get(StringConstants.FORWARD_SLASH));
        });
        return fileMap.build();
    }

    public static ImmutableMap<String, Path> getFolderMap(final List<Ds3TreeTableValueCustom> selectedItems) {
        final ImmutableList<Ds3TreeTableValueCustom> folderList = selectedItems.stream().filter(value ->
                !value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());
        final ImmutableMap.Builder<String, Path> fileMap = ImmutableMap.builder();
        folderList.forEach(folder -> {
            fileMap.put(folder.getFullName(), Paths.get(StringConstants.FORWARD_SLASH));
        });
        return fileMap.build();
    }

    public interface Ds3GetJobFactory {
        Ds3GetJob createDs3GetJob(final List<Ds3TreeTableValueCustom> selectedItems, final Path fileTreePath);
    }

}
