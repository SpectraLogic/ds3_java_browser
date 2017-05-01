package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CancelAllRunningJobsTask extends Task {
    private final static Logger LOG = LoggerFactory.getLogger(CancelAllRunningJobsTask.class);

    private final JobWorkers jobWorkers;
    private final JobInterruptionStore jobInterruptionStore;
    private final LoggingService loggingService;

    public CancelAllRunningJobsTask(final JobWorkers jobWorkers,
                                    final JobInterruptionStore jobInterruptionStore,
                                    final LoggingService loggingService) {
        this.jobWorkers = jobWorkers;
        this.jobInterruptionStore = jobInterruptionStore;
        this.loggingService = loggingService;
    }

    @Override
    protected Object call() throws Exception {
        LOG.info("Starting cancel all the running jobs");
        if (jobWorkers != null && !Guard.isNullOrEmpty(jobWorkers.getTasks())) {
            jobWorkers.getTasks().forEach(job -> {
                try {
                    String jobId = StringConstants.EMPTY_STRING;
                    Ds3Client ds3Client = null;
                    if (job instanceof Ds3PutJob) {
                        final Ds3PutJob ds3PutJob = (Ds3PutJob) job;
                        ds3PutJob.cancel();
                        if (ds3PutJob.getJobId() != null) {
                            jobId = ds3PutJob.getJobId().toString();
                            ds3Client = ds3PutJob.getDs3Client();
                        }
                        LOG.info("Cancelled job:{} " , ds3PutJob.getJobId());
                    } else if (job instanceof Ds3GetJob) {
                        final Ds3GetJob ds3GetJob = (Ds3GetJob) job;
                        ds3GetJob.cancel();
                        if (ds3GetJob.getJobId() != null) {
                            jobId = ds3GetJob.getJobId().toString();
                            ds3Client = ds3GetJob.getDs3Client();
                        }
                        LOG.info("Cancelled job:{} " , ds3GetJob.getJobId());
                    } else if (job instanceof RecoverInterruptedJob) {
                        final RecoverInterruptedJob recoverInterruptedJob = (RecoverInterruptedJob) job;
                        recoverInterruptedJob.cancel();
                        jobId = recoverInterruptedJob.getUuid().toString();
                        ds3Client = recoverInterruptedJob.getDs3Client();
                        LOG.info("Cancelled job:{} " , recoverInterruptedJob.getUuid());
                    }
                    ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId, ds3Client.getConnectionDetails()
                            .getEndpoint(), null, loggingService);
                    ds3Client.cancelJobSpectraS3(new CancelJobSpectraS3Request(jobId));
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
