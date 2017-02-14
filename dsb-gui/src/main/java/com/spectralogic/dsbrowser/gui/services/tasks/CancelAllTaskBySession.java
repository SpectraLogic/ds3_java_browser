package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CancelAllTaskBySession extends Task {
    private final static Logger LOG = LoggerFactory.getLogger(CancelAllTaskBySession.class);
    final ImmutableList<Ds3JobTask> tasks;
    final Session session;
    final JobInterruptionStore jobInterruptionStore;

    public CancelAllTaskBySession(final ImmutableList<Ds3JobTask> tasks, final Session session, final
    JobInterruptionStore jobInterruptionStore) {
        this.tasks = tasks;
        this.session = session;
        this.jobInterruptionStore = jobInterruptionStore;

    }

    @Override
    protected Object call() throws Exception {
        tasks.forEach(i -> {
            try {
                String jobId = StringConstants.EMPTY_STRING;
                Ds3Client ds3Client = null;
                if (i instanceof Ds3PutJob) {
                    final Ds3PutJob ds3PutJob = (Ds3PutJob) i;
                    if (ds3PutJob.getClient().getConnectionDetails().getCredentials()
                            .getClientId()
                            .equals(session.getClient().getConnectionDetails().getCredentials().getClientId())
                            && ds3PutJob.getClient().getConnectionDetails().getCredentials().getKey()
                            .equals(session.getClient().getConnectionDetails().getCredentials().getKey())) {
                        ds3PutJob.cancel();
                        if (ds3PutJob.getJobId() != null) {
                            jobId = ds3PutJob.getJobId().toString();
                            ds3Client = ds3PutJob.getClient();
                        }
                    }
                } else if (i instanceof Ds3GetJob) {
                    final Ds3GetJob ds3GetJob = (Ds3GetJob) i;
                    if (ds3GetJob.getDs3Client().getConnectionDetails().getCredentials().getClientId().equals(session.getClient().getConnectionDetails().getCredentials().getClientId()) && ds3GetJob.getDs3Client().getConnectionDetails().getCredentials().getKey().equals(session.getClient().getConnectionDetails().getCredentials().getKey())) {
                        ds3GetJob.cancel();
                        if (ds3GetJob.getJobId() != null) {
                            jobId = ds3GetJob.getJobId().toString();
                            ds3Client = ds3GetJob.getDs3Client();
                        }
                    }
                } else if (i instanceof RecoverInterruptedJob) {
                    final RecoverInterruptedJob recoverInterruptedJob = (RecoverInterruptedJob) i;
                    if (recoverInterruptedJob.getDs3Client().getConnectionDetails().getCredentials().getClientId().equals(session.getClient().getConnectionDetails().getCredentials().getClientId()) && recoverInterruptedJob.getDs3Client().getConnectionDetails().getCredentials().getKey().equals(session.getClient().getConnectionDetails().getCredentials().getKey())) {
                        recoverInterruptedJob.cancel();
                        jobId = recoverInterruptedJob.getUuid().toString();
                        ds3Client = recoverInterruptedJob.getDs3Client();
                    }
                }
                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId, ds3Client.getConnectionDetails()
                        .getEndpoint(), null);
                ds3Client.cancelJobSpectraS3(new CancelJobSpectraS3Request(jobId));
            } catch (final Exception e) {
                LOG.error("Failed to cancel job", e);
            }
        });
        return null;
    }
}
