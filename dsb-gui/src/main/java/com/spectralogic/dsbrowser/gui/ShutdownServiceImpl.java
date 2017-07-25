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

package com.spectralogic.dsbrowser.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AtomicDouble;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.dsbrowser.api.services.ShutdownService;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Performs all tasks related to shutting down the application
 */
public class ShutdownServiceImpl implements ShutdownService {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownServiceImpl.class);

    private final SavedSessionStore savedSessionStore;
    private final SavedJobPrioritiesStore savedJobPrioritiesStore;
    private final JobInterruptionStore jobInterruptionStore;
    private final SettingsStore settings;
    private final JobWorkers jobWorkers;
    private final Workers workers;

    @Inject
    public ShutdownServiceImpl(final SavedSessionStore savedSessionStore,
                               final SavedJobPrioritiesStore savedJobPrioritiesStore,
                               final JobInterruptionStore jobInterruptionStore,
                               final SettingsStore settings,
                               final JobWorkers jobWorkers,
                               final Workers workers) {
        this.savedSessionStore = savedSessionStore;
        this.savedJobPrioritiesStore = savedJobPrioritiesStore;
        this.jobInterruptionStore = jobInterruptionStore;
        this.settings = settings;
        this.jobWorkers = jobWorkers;
        this.workers = workers;
    }

    @Override
    public void shutdown() {
        closeApplication();
    }

    private void closeApplication() {
        if (savedSessionStore != null) {
            try {
                SavedSessionStore.saveSavedSessionStore(savedSessionStore);
            } catch (final IOException e) {
                LOG.error("Failed to save session information to the local filesystem", e);
            }
        }
        if (savedJobPrioritiesStore != null) {
            try {
                SavedJobPrioritiesStore.saveSavedJobPriorties(savedJobPrioritiesStore);
            } catch (final IOException e) {
                LOG.error("Failed to save job settings information to the local filesystem", e);
            }
        }

        if (jobInterruptionStore != null) {
            try {
                JobInterruptionStore.saveJobInterruptionStore(jobInterruptionStore);
            } catch (final Exception e) {
                LOG.error("Failed to save job ids", e);
            }
        }

        if (settings != null) {
            try {
                SettingsStore.saveSettingsStore(settings);
            } catch (final IOException e) {
                LOG.error("Failed to save settings information to the local filesystem", e);
            }
        }
        if (jobWorkers.getTasks().size() != 0) {

            final ImmutableList<Ds3JobTask> outstandingJobs = jobWorkers.getTasks().stream().collect(GuavaCollectors.immutableList());

            final ShutdownTask task = new ShutdownTask(outstandingJobs);

            workers.execute(task);
            task.setOnSucceeded(event -> finalShutdown());
        } else {
            finalShutdown();
        }
    }

    private void finalShutdown() {
        workers.shutdown();
        jobWorkers.shutdown();
        jobWorkers.shutdownNow();
        LOG.info("Finished shutting down");
        Platform.exit();
        System.exit(0);
    }

    private class ShutdownTask extends Task {

        private final ImmutableList<Ds3JobTask> outstandingJobs;

        public ShutdownTask(final ImmutableList<Ds3JobTask> outstandingJobs) {
            this.outstandingJobs = outstandingJobs;
        }


        @Override
        public Object call() {
            outstandingJobs.forEach(job -> {
                final CountDownLatch latch = new CountDownLatch(1);
                try {

                    String jobId = "";
                    final Ds3Client ds3Client = job.getDs3Client();
                    LOG.info("Cancelled job:{} ", job.getJobId());

                    final AtomicDouble progress = new AtomicDouble();
                    Platform.runLater(() -> {
                        progress.set(job.getProgress());
                        latch.countDown();
                    });
                    latch.await();

                    // check to see if the progress is at 1.0, need to take the difference and then compare
                    // against a delta since doubles can sometimes report back values close to 1.0, but not
                    // exactly 1.0
                    final double difference = progress.get() - 1.0;
                    if (difference > 0.0001 || difference < -0.0001) {
                        job.cancel();
                        ds3Client.cancelJobSpectraS3(new CancelJobSpectraS3Request(jobId));
                    }
                    ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId, ds3Client.getConnectionDetails()
                            .getEndpoint(), null, null);
                } catch (final Exception e1) {
                    LOG.error("Failed to cancel job", e1);
                }
            });
            return null;
        }
    }
}
