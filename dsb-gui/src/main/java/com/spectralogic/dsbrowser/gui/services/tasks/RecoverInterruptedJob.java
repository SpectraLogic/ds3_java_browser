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

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.helpers.JobRecoveryException;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.application.Platform;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.spectralogic.ds3client.models.JobRequestType.*;
import static com.spectralogic.dsbrowser.api.services.logging.LogType.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class RecoverInterruptedJob extends Ds3JobTask {

    private final static Logger LOG = LoggerFactory.getLogger(RecoverInterruptedJob.class);

    private final UUID uuid;
    private final EndpointInfo endpointInfo;
    private final Ds3PutJob.Ds3PutJobFactory ds3PutJobFactory;
    private final Ds3GetJob.Ds3GetJobFactory ds3GetJobFactory;
    private final Ds3Common ds3Common;
    private final JobWorkers jobWorkers;
    private final JobInterruptionStore jobInterruptionStore;

    @Inject
    public RecoverInterruptedJob(
            @Assisted final UUID uuid,
            @Assisted final EndpointInfo endpointInfo,
            final Ds3Client client,
            final LoggingService loggingService,
            final ResourceBundle resourceBundle,
            final Session currentSession,
            final Ds3GetJob.Ds3GetJobFactory ds3GetJobFactory,
            final Ds3PutJob.Ds3PutJobFactory ds3PutJobFactory,
            final Ds3Common ds3Common,
            final JobInterruptionStore jobInterruptionStore,
            @Assisted final JobWorkers jobWorkers
    ) {
        this.uuid = uuid;
        this.endpointInfo = endpointInfo;
        this.ds3Client = client;
        this.loggingService = loggingService;
        this.resourceBundle = resourceBundle;
        this.currentSession = currentSession;
        this.ds3GetJobFactory = ds3GetJobFactory;
        this.ds3PutJobFactory = ds3PutJobFactory;
        this.ds3Common = ds3Common;
        this.jobWorkers = jobWorkers;
        this.jobInterruptionStore = jobInterruptionStore;
    }

    @Override
    public void executeJob() throws Exception {
        final FilesAndFolderMap filesAndFolderMap = FilesAndFolderMap.buildFromEndpoint(endpointInfo, uuid);
        final JobRequestType jobRequestType = JobRequestType.valueOf(filesAndFolderMap.getType());
        job = getJob(ds3Client, uuid, jobRequestType, loggingService);
        if (job == null) {
            loggingService.logMessage("Could not get job " + uuid, LogType.ERROR);
            LOG.error("Could not get job " + uuid);
            return;
        }
        final String bucketName = job.getBucketName();
        final String targetLocation = filesAndFolderMap.getTargetLocation();
        final Map<String, Path> filesMap = filesAndFolderMap.getFiles();
        final Map<String, Path> foldersMap = filesAndFolderMap.getFolders();
        final Ds3JobTask task;
        if (jobRequestType == GET) {
            final ImmutableList.Builder<Ds3TreeTableValueCustom> builder = ImmutableList.builder();
            filesMap.entrySet().stream()
                    .map(es -> new Ds3TreeTableValueCustom(bucketName, es.getValue().toString(), BaseTreeModel.Type.File, 0, null, null, false ))
                    .forEach(builder::add);
            foldersMap.entrySet().stream()
                    .map(es -> new Ds3TreeTableValueCustom(bucketName, es.getValue().toString(), BaseTreeModel.Type.File, 0, null, null, false ))
                    .forEach(builder::add);
            task = ds3GetJobFactory.createDs3GetJob(builder.build(), Paths.get(targetLocation), job);
        } else {
            final ImmutableList.Builder<Pair<String,Path>> builder = ImmutableList.builder();
            filesMap.entrySet()
                    .stream()
                    .map(es -> new Pair<>(es.getKey(), es.getValue()))
                    .forEach(builder::add);
            foldersMap.entrySet()
                    .stream()
                    .map(es -> new Pair<>(es.getKey(), es.getValue()))
                    .forEach(builder::add);
            task = ds3PutJobFactory.createDs3PutJob(builder.build(), bucketName, targetLocation, ds3Common.getDs3TreeTableView().getRoot(), job);
        }
        task.setOnFailed(f -> {
            loggingService.logMessage("Recover Failed, see logs for details", LogType.ERROR);
            LOG.error(f.getSource().getException().getMessage(), f.getSource().getException());
            removeJobIdAndUpdateJobsBtn(jobInterruptionStore, job.getJobId());
            failed();
        });
        task.setOnSucceeded(v -> {
            removeJobIdAndUpdateJobsBtn(jobInterruptionStore, job.getJobId());
        });
        Platform.runLater(() -> {
            jobWorkers.execute(task);
        });

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
        } catch (final IOException | JobRecoveryException e) {
            LOG.error("Failed to create read job", e);
        }
        return null;
    }

    private void removeJobIdAndUpdateJobsBtn(final JobInterruptionStore jobInterruptionStore, final UUID jobId) {
        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(), ds3Client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter, loggingService);
        if (currentSession != null) {
            final String currentSelectedEndpoint = currentSession.getEndpoint() + ":" + currentSession.getPortNo();
            if (currentSelectedEndpoint.equals(ds3Client.getConnectionDetails().getEndpoint())) {
                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);
            }
        }
    }

    @Override
    public UUID getJobId() {
        return uuid;
    }

    public interface RecoverInterruptedJobFactory {
        RecoverInterruptedJob createRecoverInterruptedJob(final UUID uuid, final EndpointInfo endpointInfo, final JobWorkers jobWorkers);
    }

}
