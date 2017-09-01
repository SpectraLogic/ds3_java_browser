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
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.metadata.MetadataAccessImpl;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Ds3PutJobClean extends Ds3JobTask {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PutJob.class);
    private final Ds3Client client;
    private final ResourceBundle resourceBundle;
    private final List<Pair<String, Path>> files;
    private final SettingsStore settings;
    private final String bucket;
    private final String targetDir;
    private final String jobPriority;
    private final int maximumNumberOfParallelThreads;
    private final JobInterruptionStore jobInterruptionStore;
    private final LoggingService loggingService;
    private final String targetDirectory;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    public Ds3PutJobClean(final Ds3Client client,
                          final List<Pair<String, Path>> files,
                          final String bucket,
                          final String targetDir,
                          final JobInterruptionStore jobInterruptionStore,
                          final String jobPriority,
                          final int maximumNumberOfParallelThreads,
                          final ResourceBundle resourceBundle,
                          final SettingsStore settings,
                          final LoggingService loggingService,
                          final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        this.client = client;
        this.resourceBundle = resourceBundle;
        this.files = files;
        this.settings = settings;
        this.bucket = bucket;
        this.targetDir = targetDir;
        this.jobPriority = jobPriority;
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
        this.jobInterruptionStore = jobInterruptionStore;
        this.loggingService = loggingService;
        this.targetDirectory = bucket + "\\" + targetDir;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
    }

    @Override
    public void executeJob() throws Exception {
        final boolean metadata = settings.getFilePropertiesSettings().isFilePropertiesEnabled();
        final Instant jobStartInstant = Instant.now();
        //TODO Use Time API
        final String startJobDate = DateFormat.formatDate(new Date());
        final String jobInitiateTitleMessage = buildJobInitiatedTitleMessage(startJobDate, client);
        final String transfeerringMessage = buildTransferringMessage(resourceBundle);

        LOG.info(resourceBundle.getString("putJobStarted"));
        updateTitle(resourceBundle.getString("blackPearlHealth"));

        if (!CheckNetwork.isReachable(client)) {
            hostNotAvaialble();
            return;
        }
        updateTitle(jobInitiateTitleMessage);
        updateMessage(transfeerringMessage);

        final ImmutableMap.Builder<String, Path> fileMapBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, Path> folderMapBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, Path> fileMapperBuilder = ImmutableMap.builder();
        files.forEach(pair -> {
            final String name = pair.getKey();
            final Path path = pair.getValue();
            if (Files.isDirectory(pair.getValue())) {
                try {
                    Files.walk(path).filter(child -> !Files.isDirectory(child)).map(p -> new Pair<>(name + "/" + path.relativize(p).toString(), p))
                            .forEach(p -> folderMapBuilder.put(p.getKey(), p.getValue()));
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            } else {
                fileMapBuilder.put(name, path);
            }
        });

        final ImmutableMap<String, Path> fileMap = fileMapBuilder.build();
        final ImmutableMap<String, Path> folderMap = folderMapBuilder.build();
        fileMapperBuilder.putAll(fileMap);
        fileMapperBuilder.putAll(folderMap);
        final ImmutableMap<String, Path> fileMapper = fileMapperBuilder.build();
        final ImmutableList<Ds3Object> objects = fileMapper.entrySet().stream().map(Ds3PutJobClean::buildDs3Object).collect(GuavaCollectors.immutableList());


        this.job = Ds3ClientHelpers.wrap(client).startWriteJob(bucket, objects);
        final long totalJobSize = getTotalJobSize();

        job.withMaxParallelRequests(maximumNumberOfParallelThreads);

        ParseJobInterruptionMap.saveValuesToFiles(jobInterruptionStore, fileMap, folderMap,
                client.getConnectionDetails().getEndpoint(), this.getJobId(), totalJobSize, targetDir, JobRequestType.PUT.toString(), bucket);

        updateMessage(StringBuilderUtil.transferringTotalJobString(FileSizeFormat.getFileSizeType(totalJobSize), targetDirectory).toString());

        if (!Guard.isStringNullOrEmpty(jobPriority)) {
            client.modifyJobSpectraS3(new ModifyJobSpectraS3Request(job.getJobId()).withPriority(Priority.valueOf(jobPriority)));
        }

        final AtomicLong totalSent = addDataTransferListener(totalJobSize);

        if (metadata) {
            LOG.info("Registering metadata access Implementation");
            job.withMetadata(new MetadataAccessImpl(fileMapper));
        }
        addWaitingForChunkListener(totalJobSize, bucket + StringConstants.DOUBLE_SLASH + targetDir);

        job.attachObjectCompletedListener(obj -> updateGuiOnComplete(startJobDate, jobStartInstant, totalJobSize, totalSent, obj));

        job.transfer(file -> FileChannel.open(PathUtil.resolveForSymbolic(fileMapper.get(file)), StandardOpenOption.READ));
        waitForPermanentStorageTransfer(totalJobSize);
        ParseJobInterruptionMap.removeJobID(jobInterruptionStore, this.getJobId().toString(), client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter, loggingService);
    }

    private long getTotalJobSize() throws IOException {
        return client.getJobSpectraS3(new GetJobSpectraS3Request(this.getJobId())).getMasterObjectListResult().getOriginalSizeInBytes();
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
                        resourceBundle.getString("blackPearlCache")).toString(), LogType.SUCCESS);
    }

    private static Ds3Object buildDs3Object(final Map.Entry<String, Path> p) {
        try {
            return new Ds3Object(p.getKey(), Files.size(p.getValue()));
        } catch (final IOException e) {
            return null;
        }
    }

    private static String buildTransferringMessage(final ResourceBundle resourceBundle) {
        return resourceBundle.getString("transferringElipse");
    }

    private static String buildJobInitiatedTitleMessage(final String startJobDate, final Ds3Client client) {
        return StringBuilderUtil.jobInitiatedString(JobRequestType.PUT.toString(), startJobDate, client.getConnectionDetails().getEndpoint()).toString();
    }

    private void waitForPermanentStorageTransfer(final long totalJobSize) throws IOException, InterruptedException {
        final boolean isCacheJobEnable = settings.getShowCachedJobSettings().getShowCachedJob();
        final String dateOfTransfer = DateFormat.formatDate(new Date());
        final String finishedMessage = buildFinishedMessage(totalJobSize, isCacheJobEnable, dateOfTransfer, targetDirectory, resourceBundle);
        updateProgress(totalJobSize, totalJobSize);
        updateMessage(finishedMessage);
        loggingService.logMessage(finishedMessage, LogType.SUCCESS);

        if (isCacheJobEnable) {
            while (!client.getJobSpectraS3(new GetJobSpectraS3Request(this.getJobId())).getMasterObjectListResult().getStatus().toString().equals(StringConstants.JOB_COMPLETED)) {
                Thread.sleep(60000);
            }

            LOG.info("Job transferred to permanent storage location");
            final String newDate = DateFormat.formatDate(new Date());

            loggingService.logMessage(StringBuilderUtil.jobSuccessfullyTransferredString(JobRequestType.PUT.toString(),
                    FileSizeFormat.getFileSizeType(totalJobSize), targetDir, newDate,
                    resourceBundle.getString("permanentStorageLocation"), false).toString(), LogType.SUCCESS);
        }
    }

    private static String buildFinishedMessage(final long totalJobSize, final boolean isCacheJobEnable, final String dateOfTransfer, final String targetDir, final ResourceBundle resourceBundle) {
        return StringBuilderUtil.jobSuccessfullyTransferredString(JobRequestType.PUT.toString(),
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
}
