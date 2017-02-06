package com.spectralogic.dsbrowser.gui.util;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.RecoverInterruptedJob;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.Ds3GetJob;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CancelRunningJobsTask extends Task {
    private final static Logger LOG = LoggerFactory.getLogger(CancelRunningJobsTask.class);

    private final JobWorkers jobWorkers;
    private final JobInterruptionStore jobInterruptionStore;

    public CancelRunningJobsTask(final JobWorkers jobWorkers, final JobInterruptionStore jobInterruptionStore) {
        this.jobWorkers = jobWorkers;
        this.jobInterruptionStore = jobInterruptionStore;
    }

    @Override
    protected Object call() throws Exception {
        LOG.info("Starting cancel all the running jobs");
        if (jobWorkers != null && !Guard.isNullOrEmpty(jobWorkers.getTasks())) {
            final ImmutableList<Ds3JobTask> ds3Jobs = jobWorkers.getTasks().stream().collect(GuavaCollectors.immutableList());
            ds3Jobs.forEach(job -> {
                try {
                    if (job instanceof Ds3PutJob) {
                        final Ds3PutJob ds3PutJob = (Ds3PutJob) job;
                        ds3PutJob.cancel();
                        if (ds3PutJob.getJobId() != null) {
                            ParseJobInterruptionMap.removeJobID(jobInterruptionStore, ds3PutJob.getJobId().toString(), ds3PutJob.getClient().getConnectionDetails().getEndpoint(), null);
                            ds3PutJob.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(ds3PutJob.getJobId()));
                        }
                        LOG.info("Cancelled job: " + ds3PutJob.getJobId());
                    } else if (job instanceof Ds3GetJob) {
                        final Ds3GetJob ds3GetJob = (Ds3GetJob) job;
                        ds3GetJob.cancel();
                        if (ds3GetJob.getJobId() != null) {
                            ParseJobInterruptionMap.removeJobID(jobInterruptionStore, ds3GetJob.getJobId().toString(), ds3GetJob.getDs3Client().getConnectionDetails().getEndpoint(), null);
                            ds3GetJob.getDs3Client().cancelJobSpectraS3(new CancelJobSpectraS3Request(ds3GetJob.getJobId()));
                        }
                        LOG.info("Cancelled job: " + ds3GetJob.getJobId());
                    } else if (job instanceof RecoverInterruptedJob) {
                        final RecoverInterruptedJob recoverInterruptedJob = (RecoverInterruptedJob) job;
                        recoverInterruptedJob.cancel();
                        ParseJobInterruptionMap.removeJobID(jobInterruptionStore, recoverInterruptedJob.getUuid().toString(), recoverInterruptedJob.getDs3Client().getConnectionDetails().getEndpoint(), null);
                        recoverInterruptedJob.getDs3Client().cancelJobSpectraS3(new CancelJobSpectraS3Request(recoverInterruptedJob.getUuid()));
                        LOG.info("Cancelled job: " + recoverInterruptedJob.getUuid());
                    }
                } catch (final Exception e1) {
                    LOG.error("Failed to cancel job", e1);
                }
            });
        } else {
            LOG.info("No jobs to cancel");
        }
        return null;
    }
}
