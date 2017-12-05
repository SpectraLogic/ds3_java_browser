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

package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


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
    protected Object call() {
        LOG.info("Starting cancel all the running jobs");
        if (jobWorkers != null && !Guard.isNullOrEmpty(jobWorkers.getTasks())) {
            jobWorkers.getTasks().forEach(job -> {
                try {
                    final Ds3Client ds3Client = job.getDs3Client();
                    final String jobId = job.getJobId().toString();
                    job.cancel();
                    LOG.info("Canceled job:{} " , jobId);
                    ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId,
                            ds3Client.getConnectionDetails().getEndpoint(), null, loggingService);
                    ds3Client.cancelJobSpectraS3(new CancelJobSpectraS3Request(jobId));
                } catch (final IOException e1) {
                    LOG.error("Failed to cancel job", e1);
                }
            });
        } else {
            LOG.info("No jobs to cancel");
        }
        return null;
    }


}
