/*
 * ****************************************************************************
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
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Response;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.metadata.MetadataAccessImpl;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Ds3PutJob extends Ds3JobTask {

    private final Ds3Client client;
    private final List<File> files;
    private final String bucket;
    private final String targetDir;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final String jobPriority;
    private UUID jobId;
    private final int maximumNumberOfParallelThreads;
    private final JobInterruptionStore jobInterruptionStore;
    private final Ds3Common ds3Common;
    private final SettingsStore settings;
    private final LoggingService loggingService;
    private final Workers workers;

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PutJob.class);

    @AssistedInject
    public Ds3PutJob(
            @Assisted final Ds3PutJob.JobDetails jobDetails,
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
            final JobInterruptionStore jobIdsModel,
            final Ds3Common ds3Common,
            final SettingsStore settings,
            final LoggingService loggingService,
            final Workers workers
    ) {
        this.client = jobDetails.getClient();
        this.files = jobDetails.getFiles();
        this.bucket = jobDetails.getBucket();
        this.targetDir = jobDetails.targetDir;
        this.jobPriority = jobDetails.jobPriority;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.maximumNumberOfParallelThreads = settings.getProcessSettings().getMaximumNumberOfParallelThreads();
        this.jobInterruptionStore = jobIdsModel;
        this.ds3Common = ds3Common;
        this.settings = settings;
        this.loggingService = loggingService;
        this.workers = workers;
    }

    @Override
    public void executeJob() throws Exception {
        try {
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
            files.forEach(path1 -> {
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
            directories.forEach(path -> {
                try {
                    partOfDirBuilder.add(path);
                    expandedPaths.putAll(path, Files.walk(path).filter(child -> !Files.isDirectory(child)).collect(GuavaCollectors.immutableList()));
                    final String ds3ObjPath = getDs3ObjectPath(path, path, true, files.size(), directories.size()); // TODO what is this code doing exactly
                    final String ds3FileName = PathUtil.toDs3Path(targetDir, ds3ObjPath); // TODO why are we appending the result of the above to the targetDir and calling that the Ds3FileName
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
            }).filter(Objects::nonNull).collect(GuavaCollectors.immutableList());
            final ImmutableMap<String, Path> fileMapper = fileMapBuilder.build();
            final long totalJobSize = objects.stream().mapToLong(Ds3Object::getSize).sum();
            updateMessage("Transferring " + FileSizeFormat.getFileSizeType(totalJobSize) + " in " + bucket + "\\" + targetDir);
            final Ds3ClientHelpers.Job job = helpers.startWriteJob(bucket, objects).withMaxParallelRequests(maximumNumberOfParallelThreads);
            jobId = job.getJobId();

            // I don't like this, but without exposing callbacks and heavily refactoring this code we need to manually refresh the ds3 panel
            Platform.runLater(() -> ParseJobInterruptionMap.refreshCompleteTreeTableView(ds3Common, workers, loggingService));
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
                final String newDate = DateFormat.formatDate(new Date());
                if (obj.contains("/")) {
                    final int i = obj.lastIndexOf("/");
                    updateMessage(FileSizeFormat.getFileSizeType(totalSent.get() / 2) + " / " + FileSizeFormat.getFileSizeType(totalJobSize) + " Transferred file -> " + obj.substring(i, obj.length()) + " to " + bucket + "/" + targetDir);
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Successfully transferred (BlackPearl cache): " + obj.substring(i, obj.length()) + " to " + bucket + "/" + targetDir + " at " + newDate, LogType.SUCCESS));
                } else {
                    updateMessage(FileSizeFormat.getFileSizeType(totalSent.get() / 2) + " / " + FileSizeFormat.getFileSizeType(totalJobSize) + "Transferred file -> " + obj.substring(0, obj.length()) + " to " + bucket + "/" + targetDir);
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Successfully transferred (BlackPearl cache): " + obj.substring(0, obj.length()) + " to " + bucket + "/" + targetDir + " at " + newDate, LogType.SUCCESS));
                }
            });
            //store meta data to server
            final boolean isFilePropertiesEnable = settings.getFilePropertiesSettings().isFilePropertiesEnabled();
            if (isFilePropertiesEnable) {
                job.withMetadata(new MetadataAccessImpl(fileMapper));
            }
            job.transfer(file -> FileChannel.open(PathUtil.resolveForSymbolic(fileMapper.get(file)), StandardOpenOption.READ));
            updateProgress(totalJobSize, totalJobSize);
            updateMessage("Files [Size: " + FileSizeFormat.getFileSizeType(totalJobSize) + "] transferred to" + " bucket -" + bucket + " at location - " + targetDir + ". (BlackPearl cache). Waiting for the storage target allocation.");
            updateProgress(totalJobSize, totalJobSize);
            Platform.runLater(() -> {
                final String newDate = DateFormat.formatDate(new Date());
                deepStorageBrowserPresenter.logText("PUT job [Size: " + FileSizeFormat.getFileSizeType(totalJobSize) + "] completed. Files transferred to" + " bucket -" + bucket + " at location - " + targetDir + ". (BlackPearl cache). Waiting for the storage target allocation." + " at " + newDate, LogType.SUCCESS);
            });
            //Can not assign final.
            GetJobSpectraS3Response response = client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
            while (!response.getMasterObjectListResult().getStatus().toString().equals("COMPLETED")) {
                Thread.sleep(60000);
                response = client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
            }
            final String newDate = DateFormat.formatDate(new Date());
            Platform.runLater(() -> deepStorageBrowserPresenter.logText("PUT job [Size: " + FileSizeFormat.getFileSizeType(totalJobSize) + "]  completed. File transferred to storage location" + " at " + newDate, LogType.SUCCESS));

            ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(), client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
        } catch (final Exception e) {
            LOG.error("Encountered an error on a put job", e);
            final String newDate = DateFormat.formatDate(new Date());
            if (e instanceof InterruptedException) {
                Platform.runLater(() -> deepStorageBrowserPresenter.logText("PUT Job Cancelled (User Interruption)" + " at " + newDate, LogType.ERROR));
            } else if (e instanceof RuntimeException) {
                //cancel the job if it is already running
                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(), client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
                Platform.runLater(() -> deepStorageBrowserPresenter.logTextForParagraph("PUT Job Failed " + client.getConnectionDetails().getEndpoint() + ". Reason+" + e.toString() + " at " + newDate, LogType.ERROR));
            } else {
                Platform.runLater(() -> deepStorageBrowserPresenter.logTextForParagraph("PUT Job Failed " + client.getConnectionDetails().getEndpoint() + ". Reason+" + e.toString() + " at " + newDate, LogType.ERROR));
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter.getJobProgressView(), jobId);
                final Session session = ds3Common.getCurrentSessions().stream().findFirst().orElse(null);
                if (session != null) {
                    final String currentSelectedEndpoint = session.getEndpoint() + ":" + session.getPortNo();
                    if (currentSelectedEndpoint.equals(client.getConnectionDetails().getEndpoint())) {
                        ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);
                    }
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

    public Ds3Client getClient() {
        return client;
    }

    public static class JobDetails {
        private final Ds3Client client;
        private final List<File> files;
        private final String bucket;
        private final String targetDir;
        private final String jobPriority;

        public JobDetails(final Ds3Client client, final List<File> files, final String bucket, final String targetDir, final String jobPriority) {
            this.client = client;
            this.files = files;
            this.bucket = bucket;
            this.targetDir = targetDir;
            this.jobPriority = jobPriority;
        }

        public Ds3Client getClient() {
            return client;
        }

        public List<File> getFiles() {
            return files;
        }

        public String getBucket() {
            return bucket;
        }

        public String getTargetDir() {
            return targetDir;
        }

        public String getJobPriority() {
            return jobPriority;
        }
    }
}
