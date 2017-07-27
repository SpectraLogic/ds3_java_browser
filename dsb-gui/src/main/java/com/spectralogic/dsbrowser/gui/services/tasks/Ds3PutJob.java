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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
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
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class Ds3PutJob extends Ds3JobTask {
    private final static Logger LOG = LoggerFactory.getLogger(Ds3PutJob.class);
    private final List<File> files;
    private final String bucket;
    private final String targetDir;
    private final String jobPriority;
    private final int maximumNumberOfParallelThreads;
    private final JobInterruptionStore jobInterruptionStore;
    private final SettingsStore settings;
    private final LoggingService loggingService;
    private UUID jobId;

    public Ds3PutJob(final Ds3Client ds3Client,
                     final List<File> files,
                     final String bucket,
                     final String targetDir,
                     final String jobPriority,
                     final int maximumNumberOfParallelThreads,
                     final JobInterruptionStore jobIdsModel,
                     final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
                     final Session currentSession,
                     final SettingsStore settings,
                     final LoggingService loggingService,
                     final ResourceBundle resourceBundle) {
        this.ds3Client = ds3Client;
        this.files = files;
        this.bucket = bucket;
        this.targetDir = targetDir;
        this.jobPriority = jobPriority;
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
        this.jobInterruptionStore = jobIdsModel;
        this.settings = settings;
        this.loggingService = loggingService;
        this.resourceBundle = resourceBundle;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.currentSession = currentSession;
    }

    @Override
    public void executeJob() throws Exception {
        //job start time
        final Instant jobStartInstant = Instant.now();
        LOG.info("Put job Started");
        try {
            updateTitle(resourceBundle.getString("blackPearlHealth"));
            if (CheckNetwork.isReachable(ds3Client)) {
                final String startJobDate = DateFormat.formatDate(new Date());
                updateTitle(StringBuilderUtil.jobInitiatedString(JobRequestType.PUT.toString(), startJobDate, ds3Client.getConnectionDetails().getEndpoint()).toString());
                loggingService.logMessage(StringBuilderUtil.jobInitiatedString(JobRequestType.PUT.toString(), startJobDate, ds3Client.getConnectionDetails().getEndpoint()).toString(), LogType.INFO);
                updateMessage(resourceBundle.getString("transferring") + "..");

                final ImmutableList<Path> directories = getDirectoriesOrFiles(false);
                final ImmutableList<Path> files = getDirectoriesOrFiles(true);

                final ImmutableSet.Builder<Path> partOfDirBuilder = ImmutableSet.builder();
                final ImmutableMultimap.Builder<Path, Path> expandedPaths = ImmutableMultimap.builder();

                final ImmutableMap.Builder<String, Path> fileMap = createFileMap(files, directories, partOfDirBuilder, expandedPaths);
                final ImmutableMap.Builder<String, Path> folderMap = createFolderMap(directories, expandedPaths, partOfDirBuilder);

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

                job = getJob(objects, fileMap, folderMap, totalJobSize);

                updateMessage(StringBuilderUtil.transferringTotalJobString(FileSizeFormat.getFileSizeType(totalJobSize), bucket + "\\" + targetDir).toString());
                if (!Guard.isStringNullOrEmpty(jobPriority)) {
                    ds3Client.modifyJobSpectraS3(new ModifyJobSpectraS3Request(job.getJobId()).withPriority(Priority.valueOf(jobPriority)));
                }
                final AtomicLong totalSent = addDataTransferListener(totalJobSize);

                job.attachObjectCompletedListener(obj -> {
                    LOG.info("Object Transfer Completed");
                    final String newDate = DateFormat.formatDate(new Date());

                    int index = 0;
                    if (obj.contains(StringConstants.FORWARD_SLASH)) {
                        index = obj.lastIndexOf(StringConstants.FORWARD_SLASH);
                    }

                    getTransferRates(jobStartInstant, totalSent, totalJobSize, obj.substring(index, obj.length()), bucket
                            + StringConstants.FORWARD_SLASH + targetDir);

                    final int finalIndex = index;
                    loggingService.logMessage(
                            StringBuilderUtil.objectSuccessfullyTransferredString(
                                    obj.substring(finalIndex, obj.length()),
                                    bucket + StringConstants.FORWARD_SLASH + targetDir, newDate,
                                    resourceBundle.getString("blackPearlCache")).toString(), LogType.SUCCESS);
                });
                //store meta data to server
                final boolean isFilePropertiesEnable = settings.getFilePropertiesSettings().isFilePropertiesEnabled();
                if (isFilePropertiesEnable) {
                    LOG.info("Registering metadata access Implementation");
                    job.withMetadata(new MetadataAccessImpl(fileMapper));
                }

                addWaitingForChunkListener(totalJobSize, bucket + StringConstants.DOUBLE_SLASH + targetDir);
                job.transfer(file -> FileChannel.open(PathUtil.resolveForSymbolic(fileMapper.get(file)), StandardOpenOption.READ));

                waitForPermanentStorageTransfer(totalJobSize);
                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(), ds3Client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter, loggingService);
            } else {
                hostNotAvaialble();
            }
        } catch (final RuntimeException rte) {
            //cancel the job if it is already running
            isJobFailed = true;
            LOG.error("Encountered an error on a put job: " + rte.getMessage(), rte);

            removeJobIdAndUpdateJobsBtn(jobInterruptionStore, jobId);
            loggingService.logMessage(StringBuilderUtil.jobFailed(JobRequestType.PUT.toString(), ds3Client.getConnectionDetails().getEndpoint(), rte).toString(), LogType.ERROR);
        } catch (final InterruptedException ie) {
            isJobFailed = true;
            LOG.error("Encountered an error on a put job: " + ie.getMessage(), ie);

            loggingService.logMessage(StringBuilderUtil.jobCancelled(JobRequestType.PUT.toString()).toString(), LogType.ERROR);
        } catch (final Exception e) {
            isJobFailed = true;
            LOG.error("Encountered an error on a put job: " + e.getMessage(), e);

            loggingService.logMessage(StringBuilderUtil.jobFailed(JobRequestType.PUT.toString(), ds3Client.getConnectionDetails().getEndpoint(), e).toString(), LogType.ERROR);
            updateInterruptedJobsBtn(jobInterruptionStore, jobId);
        }
    }


    private Ds3ClientHelpers.Job getJob(final ImmutableList<Ds3Object> objects,
                                        final ImmutableMap.Builder<String, Path> fileMap,
                                        final ImmutableMap.Builder<String, Path> folderMap,
                                        final long totalJobSize) throws Exception {
        final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(ds3Client, Constants.RETRY_AFTER_COUNT);
        final Ds3ClientHelpers.Job job = helpers.startWriteJob(bucket, objects).withMaxParallelRequests(maximumNumberOfParallelThreads);
        jobId = job.getJobId();
        try {
            final String targetLocation = PathUtil.toDs3Path(bucket, targetDir);
            ParseJobInterruptionMap.saveValuesToFiles(jobInterruptionStore, fileMap.build(), folderMap.build(),
                    ds3Client.getConnectionDetails().getEndpoint(), jobId, totalJobSize, targetLocation, JobRequestType.PUT.toString(), bucket);
        } catch (final Exception e) {
            LOG.error("Failed to save job id: ", e);
        }
        return job;
    }

    private ImmutableList<Path> getDirectoriesOrFiles(final boolean isFiles) {
        final ImmutableList<Path> paths = files.stream().map(File::toPath).collect(GuavaCollectors.immutableList());
        if (isFiles) {
            return paths.stream().filter(path -> !Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
        } else {
            return paths.stream().filter(path -> Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
        }
    }

    /**
     * Create map of folders from the selected list
     *
     * @param directories      selected directories
     * @param expandedPaths    expandedPaths
     * @param partOfDirBuilder partOfDirBuilder
     */
    public ImmutableMap.Builder<String, Path> createFolderMap(final ImmutableList<Path> directories, final ImmutableMultimap.Builder<Path, Path> expandedPaths, final ImmutableSet.Builder<Path> partOfDirBuilder) {
        final ImmutableMap.Builder<String, Path> folderMap = ImmutableMap.builder();
        directories.forEach(path -> {
            try {
                partOfDirBuilder.add(path);
                expandedPaths.putAll(path, Files.walk(path).filter(child -> !Files.isDirectory(child)).collect(GuavaCollectors.immutableList()));
                final String ds3ObjPath = getDs3ObjectPath(path, path, true, files.size(), directories.size());
                final String ds3FileName = PathUtil.toDs3Path(targetDir, ds3ObjPath);
                folderMap.put(ds3FileName, path);
            } catch (final IOException e) {
                LOG.error("Failed to list files for directory : " + path.toString(), e);
            }
        });
        return folderMap;
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
     */
    public ImmutableMap.Builder<String, Path> createFileMap(final ImmutableList<Path> files, final ImmutableList<Path> directories, final ImmutableSet.Builder<Path> partOfDirBuilder, final ImmutableMultimap.Builder<Path, Path> expandedPaths) {
        final ImmutableMap.Builder<String, Path> fileMap = ImmutableMap.builder();
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
        return fileMap;
    }

    private void waitForPermanentStorageTransfer(final long totalJobSize) throws Exception {
        final boolean isCacheJobEnable = settings.getShowCachedJobSettings().getShowCachedJob();
        final String dateOfTransfer = DateFormat.formatDate(new Date());

        updateProgress(totalJobSize, totalJobSize);
        updateMessage(StringBuilderUtil.jobSuccessfullyTransferredString(JobRequestType.PUT.toString(),
                FileSizeFormat.getFileSizeType(totalJobSize), bucket + "\\" + targetDir, dateOfTransfer,
                resourceBundle.getString("blackPearlCache"), isCacheJobEnable).toString());

        loggingService.logMessage(StringBuilderUtil.jobSuccessfullyTransferredString(JobRequestType.PUT.toString(),
                FileSizeFormat.getFileSizeType(totalJobSize), bucket + "\\" + targetDir, dateOfTransfer,
                resourceBundle.getString("blackPearlCache"), isCacheJobEnable).toString(), LogType.SUCCESS);

        if (isCacheJobEnable) {
            while (!ds3Client.getJobSpectraS3(new GetJobSpectraS3Request(jobId)).getMasterObjectListResult().getStatus().toString().equals(StringConstants.JOB_COMPLETED)) {
                Thread.sleep(60000);
            }

            LOG.info("Job transferred to permanent storage location");
            final String newDate = DateFormat.formatDate(new Date());

            loggingService.logMessage(StringBuilderUtil.jobSuccessfullyTransferredString(JobRequestType.PUT.toString(),
                    FileSizeFormat.getFileSizeType(totalJobSize), bucket + "\\" + targetDir, newDate,
                    resourceBundle.getString("permanentStorageLocation"), false).toString(), LogType.SUCCESS);
        }
    }

    @Override
    public UUID getJobId() {
        return jobId;
    }
}
