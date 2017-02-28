package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Response;
import com.spectralogic.dsbrowser.gui.components.modifyjobpriority.ModifyJobPriorityModel;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;

import java.util.Optional;
import java.util.UUID;

public class GetJobPriorityTask extends Ds3Task {
    private final Session session;
    private final UUID jobId;

    public GetJobPriorityTask(final Session session, final UUID jobId) {
        this.session = session;
        this.jobId = jobId;
    }

    @Override
    protected Optional<Object> call() throws Exception {
        final Ds3Client client = session.getClient();
        final GetJobSpectraS3Response jobSpectraS3 = client.getJobSpectraS3(new GetJobSpectraS3Request(jobId));
        return Optional.of(new ModifyJobPriorityModel(jobId,
                jobSpectraS3.getMasterObjectListResult().getPriority().toString(), session));
    }
}
