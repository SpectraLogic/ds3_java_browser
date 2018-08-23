/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
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

package com.spectralogic.dsbrowser.gui.util;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CancelAllTaskBySession;
import com.spectralogic.dsbrowser.gui.services.tasks.CancelAllRunningJobsTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class CancelJobsWorker {
    private static final Logger LOG = LoggerFactory.getLogger(CancelJobsWorker.class);

    private final Workers workers;
    private final RefreshCompleteViewWorker refreshCompleteTreeTableView;
    private final LoggingService loggingService;

    @Inject
    public CancelJobsWorker(
            final Workers workers,
            final RefreshCompleteViewWorker refreshCompleteTreeTableView,
            final LoggingService loggingService) {
       this.workers = workers;
       this.refreshCompleteTreeTableView = refreshCompleteTreeTableView;
       this.loggingService = loggingService;
    }

    public void cancelAllRunningJobs(final JobWorkers jobWorkers, final JobInterruptionStore jobInterruptionStore) {
        if (!jobWorkers.getTasks().isEmpty()) {
            final CancelAllRunningJobsTask cancelAllRunningJobsTask = new CancelAllRunningJobsTask(jobWorkers, jobInterruptionStore, loggingService);
            cancelAllRunningJobsTask.setOnSucceeded(SafeHandler.logHandle(event -> {
                refreshCompleteTreeTableView.refreshCompleteTreeTableView();
                if (cancelAllRunningJobsTask.getValue() != null) {
                    LOG.info("Canceled job. {}", cancelAllRunningJobsTask.getValue());
                }
            }));
            workers.execute(cancelAllRunningJobsTask);
        }
    }

    public void cancelAllRunningJobsBySession(final JobWorkers jobWorkers, final JobInterruptionStore jobInterruptionStore, final Session closedSession) {
        final ImmutableList<Ds3JobTask> tasks = jobWorkers
                .getTasks()
                .stream()
                .filter(ds3JobTask -> compareEndpoints(closedSession, ds3JobTask))
                .collect(GuavaCollectors.immutableList());
        if (!tasks.isEmpty()) {
            final CancelAllTaskBySession cancelAllRunningJobs = new CancelAllTaskBySession(tasks,
                    jobInterruptionStore, loggingService);
            cancelAllRunningJobs.setOnSucceeded(SafeHandler.logHandle(event -> {
                if (cancelAllRunningJobs.getValue() != null) {
                    LOG.info("Canceled job. {}", cancelAllRunningJobs.getValue());
                }
            }));
            workers.execute(cancelAllRunningJobs);
        }
    }

    private boolean compareEndpoints(final Session closedSession, final Ds3JobTask ds3JobTask) {
        final String taskEndpoint = ds3JobTask.getDs3Client().getConnectionDetails().getEndpoint();
        final String sessionEndpoint = closedSession.getEndpoint() + ":" + closedSession.getPortNo();
        return taskEndpoint.equals(sessionEndpoint);
    }
}
