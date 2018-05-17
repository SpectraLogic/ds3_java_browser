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
        if (jobWorkers.getTasks().size() != 0) {
            final CancelAllRunningJobsTask cancelAllRunningJobsTask = cancelTasks(jobWorkers, jobInterruptionStore);
            cancelAllRunningJobsTask.setOnSucceeded(SafeHandler.logHandle(event -> {
                refreshCompleteTreeTableView.refreshCompleteTreeTableView();
                if (cancelAllRunningJobsTask.getValue() != null) {
                    LOG.info("Canceled job. {}", cancelAllRunningJobsTask.getValue());
                }
            }));

        }
    }

    public CancelAllRunningJobsTask cancelTasks(final JobWorkers jobWorkers, final JobInterruptionStore jobInterruptionStore) {
        final CancelAllRunningJobsTask cancelAllRunningJobsTask = new CancelAllRunningJobsTask(jobWorkers, jobInterruptionStore, loggingService);
        workers.execute(cancelAllRunningJobsTask);
        return cancelAllRunningJobsTask;
    }

    public CancelAllTaskBySession cancelAllRunningJobsBySession(final JobWorkers jobWorkers, final JobInterruptionStore jobInterruptionStore) {
        final ImmutableList<Ds3JobTask> tasks = jobWorkers.getTasks().stream().collect(GuavaCollectors.immutableList());
        if (tasks.size() != 0) {
            final CancelAllTaskBySession cancelAllRunningJobs = new CancelAllTaskBySession(tasks,
                    jobInterruptionStore, loggingService);
            cancelAllRunningJobs.setOnSucceeded(SafeHandler.logHandle(event -> {
                if (cancelAllRunningJobs.getValue() != null) {
                    LOG.info("Canceled job. {}", cancelAllRunningJobs.getValue());
                }
            }));
            workers.execute(cancelAllRunningJobs);
            return cancelAllRunningJobs;
        } else {
            return null;
        }
    }
}
