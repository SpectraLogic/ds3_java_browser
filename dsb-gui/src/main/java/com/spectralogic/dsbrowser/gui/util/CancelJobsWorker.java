package com.spectralogic.dsbrowser.gui.util;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CancelAllTaskBySession;
import com.spectralogic.dsbrowser.gui.services.tasks.CancelRunningJobsTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import org.slf4j.Logger;

import static com.spectralogic.dsbrowser.gui.util.RefreshCompleteViewWorker.refreshCompleteTreeTableView;

public final class CancelJobsWorker {

    public static void cancelAllRunningJobs(final JobWorkers jobWorkers, final JobInterruptionStore jobInterruptionStore, final Logger LOG, final Workers workers, final Ds3Common ds3Common) {
        if (jobWorkers.getTasks().size() != 0) {
            final CancelRunningJobsTask cancelRunningJobsTask = cancelTasks(jobWorkers, jobInterruptionStore, workers);
            cancelRunningJobsTask.setOnSucceeded(event -> {
                refreshCompleteTreeTableView(ds3Common, workers);
                if (cancelRunningJobsTask.getValue() != null) {
                    LOG.info("Cancelled job. {}", cancelRunningJobsTask.getValue());
                }
            });

        }
    }

    public static CancelRunningJobsTask cancelTasks(final JobWorkers jobWorkers, final JobInterruptionStore
            jobInterruptionStore, final Workers workers) {
        final CancelRunningJobsTask cancelRunningJobsTask = new CancelRunningJobsTask(jobWorkers, jobInterruptionStore);
        workers.execute(cancelRunningJobsTask);
        return cancelRunningJobsTask;
    }

    public static CancelAllTaskBySession cancelAllRunningJobsBySession(final JobWorkers jobWorkers, final JobInterruptionStore
            jobInterruptionStore, final Logger LOG, final Workers workers, final Session session) {
        final ImmutableList<Ds3JobTask> tasks = jobWorkers.getTasks().stream().collect(GuavaCollectors.immutableList());
        if (tasks.size() != 0) {
            final CancelAllTaskBySession cancelAllRunningJobs = new CancelAllTaskBySession(tasks, session,
                    jobInterruptionStore);
            workers.execute(cancelAllRunningJobs);
            cancelAllRunningJobs.setOnSucceeded(event -> {
                if (cancelAllRunningJobs.getValue() != null) {
                    LOG.info("Cancelled job. {}", cancelAllRunningJobs.getValue());
                }
            });
            return cancelAllRunningJobs;
        } else {
            return null;
        }
    }
}
