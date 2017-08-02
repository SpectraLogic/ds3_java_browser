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
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.helpers.FileObjectGetter;
import com.spectralogic.ds3client.helpers.MetadataReceivedListener;
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder;
import com.spectralogic.ds3client.helpers.strategy.transferstrategy.TransferStrategyBuilder;
import com.spectralogic.ds3client.metadata.MetadataReceivedListenerImpl;
import com.spectralogic.ds3client.models.Contents;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.networking.Metadata;
import com.spectralogic.ds3client.utils.SeekableByteChannelInputStream;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("Guava")
public class Ds3GetJob extends Ds3JobTask {
    private static final int retryAfter = 100;
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


    public Ds3GetJob(final List<Ds3TreeTableValueCustom> selectedItems,
                     final Path fileTreePath,
                     final Ds3Client client,
                     final String jobPriority,
                     final int maximumNumberOfParallelThreads,
                     final JobInterruptionStore jobInterruptionStore,
                     final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
                     final ResourceBundle resourceBundle,
                     final LoggingService loggingService) {
        this.selectedItems = selectedItems;
        this.fileTreePath = fileTreePath;
        this.client = client;
        this.wrappedDs3Client = Ds3ClientHelpers.wrap(client, retryAfter);
        this.resourceBundle = resourceBundle;
        this.loggingService = loggingService;
        this.jobPriority = jobPriority;
        this.jobInterruptionStore = jobInterruptionStore;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
        this.metadataReceivedListener = new MetadataReceivedListenerImpl(fileTreePath.toString());
    }

    private static boolean isEmptyDirectory(final Ds3Object object) {
        return (object.getSize() == 0) && (object.getName().endsWith("/"));
    }

    @SuppressWarnings("Guava")
    @Override
    public void executeJob() throws Exception {
        if (!CheckNetwork.isReachable(client)) {
            hostNotAvaialble();
            return;
        }
        final String startJobDate = DateFormat.formatDate(new Date());
        updateTitle(getJobStart(startJobDate, client));
        loggingService.logMessage(getJobStart(startJobDate, client), LogType.INFO);
        updateMessage(resourceBundle.getString("transferring") + StringConstants.DOUBLE_DOTS);
        selectedItems.stream()
                .filter(Objects::nonNull)
                .map(Ds3TreeTableValueCustom::getBucketName).distinct()
                .forEach(bucketName -> selectedItems.stream()
                        .filter(item -> bucketName.equals(item.getBucketName()))
                        .forEach(selectedItem -> transferFromSelectedItem(bucketName, selectedItem)));
    }

    private void transferFromSelectedItem(final String bucketName, final Ds3TreeTableValueCustom selectedItem) {
        final Instant startTime = Instant.now();
        final String fileName = selectedItem.getName();
        final String prefix = getParent(selectedItem.getFullName());
        final FluentIterable<Ds3Object> ds3Objects = buildIteratorFromSelectedItems(bucketName, selectedItem);
        final long totalJobSize = getTotalJobSize(ds3Objects);
        final Ds3ClientHelpers.Job job;
        ds3Objects.filter(Ds3GetJob::isEmptyDirectory).forEach(ds3Object -> Ds3GetJob.buildEmptyDirectories(ds3Object, fileTreePath, loggingService));
        try {
            job = getJobFromIterator(bucketName, ds3Objects.filter(ds3Object -> !isEmptyDirectory(ds3Object)));
        } catch (final IOException e) {
            LOG.error("Unable to get Jobs", e);
            loggingService.logMessage("Unable to get jobs from Black Perl", LogType.ERROR);
            return;
        }
        if (job != null) {
            jobId = job.getJobId();
        }
        updateMessage(getTransferringMessage(totalJobSize));
        attachListenersToJob(startTime, totalJobSize, job);
        try {
//            final TransferStrategyBuilder transferStrategyBuilder = new TransferStrategyBuilder();
            notifyIfOverwriting(fileName);
            job.transfer(l -> getTransferJob(prefix,l).buildChannel(l));
        } catch (final IOException e) {
            loggingService.logMessage("Unable to transfer Job", LogType.ERROR);
            LOG.error("Unable to transfer Job", e);
        }
        updateUI(totalJobSize);
    }

    private void updateUI(final long totalJobSize) {
        updateProgress(totalJobSize, totalJobSize);
        updateMessage(getJobTransferred(totalJobSize));
        updateProgress(totalJobSize, totalJobSize);
        ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(),
                client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter, loggingService);
        loggingService.logMessage(StringBuilderUtil.getJobCompleted(totalJobSize, client).toString(), LogType.SUCCESS);
    }

    private void attachListenersToJob(final Instant startTime, final long totalJobSize, final Ds3ClientHelpers.Job job) {
        job.attachObjectCompletedListener(o -> setObjectCompleteListener(o, startTime, totalJobSize));
        job.attachMetadataReceivedListener(this::setMetadataReceivedListener);
        job.attachWaitingForChunksListener(this::setWaitingForChunksListener);
        job.attachDataTransferredListener(l -> setDataTransferredListener(l, totalJobSize));
    }

    private Ds3ClientHelpers.ObjectChannelBuilder getTransferJob(final String prefix, final String l) throws IOException {
        final Ds3ClientHelpers.ObjectChannelBuilder objectChannelBuilder;
        final FileObjectGetter fileObjectGetter = new FileObjectGetter(fileTreePath);
        if (prefix.isEmpty()) {
            objectChannelBuilder = fileObjectGetter;
        } else {
            objectChannelBuilder = new PrefixRemoverObjectChannelBuilder(fileObjectGetter, prefix + "/");
        }
        return objectChannelBuilder;
    }

    private void notifyIfOverwriting(final String name) {
        if (Files.exists(fileTreePath.resolve(name))) {
            loggingService.logMessage(resourceBundle.getString("fileOverridden")
                    + StringConstants.SPACE + name + StringConstants.SPACE
                    + resourceBundle.getString("to")
                    + StringConstants.SPACE + fileTreePath, LogType.SUCCESS);
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
                DateFormat.formatDate(new Date()), null, false).toString();
    }

    private static String getJobStart(final String startJobDate, final Ds3Client client) {
        return StringBuilderUtil.jobInitiatedString(JobRequestType.GET.toString(), startJobDate, client.getConnectionDetails().getEndpoint())
                .toString();
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

    private FluentIterable<Ds3Object> buildIteratorFromSelectedItems(final String bucketName, final Ds3TreeTableValueCustom selectedItem) {
        Iterable<Contents> c;
        try {
            c = wrappedDs3Client.listObjects(bucketName, selectedItem.getFullName());
        } catch (final IOException e) {
            c = FluentIterable.from(new Contents[0]);
        }
        return FluentIterable.from(c).transform(contents -> new Ds3Object(contents.getKey(), contents.getSize()));
    }

    @Override
    public UUID getJobId() {
        return jobId;
    }

    private void setWaitingForChunksListener(final int retryAfterSeconds) {
        try {
            loggingService.logMessage("Attempting Retry", LogType.INFO);
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
        loggingService.logMessage(StringBuilderUtil.objectSuccessfullyTransferredString(o, fileTreePath.toString(), DateFormat.formatDate(new Date()), null).toString(), LogType.SUCCESS);
    }

    private static String getParent(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        final int lastIndexOf = path.lastIndexOf('/');
        if (lastIndexOf < 1) {
            return "";
        } else {
            return path.substring(0, lastIndexOf);
        }
    }

    private static void buildEmptyDirectories(final Ds3Object emtpyDir, final Path fileTreePath, final LoggingService loggingService) {
        final Path directoryPath = fileTreePath.resolve(emtpyDir.getName());
        try {
            Files.createDirectories(directoryPath);
        } catch (final IOException e) {
            final String pathString = directoryPath.toString();
            LOG.error("Could not create " + pathString, e);
            loggingService.logMessage("Could not create " + pathString, LogType.ERROR);
        }
    }
}
