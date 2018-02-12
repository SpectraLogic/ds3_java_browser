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

package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


public class CancelAllTaskBySession extends Task {
    private final static Logger LOG = LoggerFactory.getLogger(CancelAllTaskBySession.class);
    final ImmutableList<Ds3JobTask> tasks;
    private final JobInterruptionStore jobInterruptionStore;
    private final LoggingService loggingService;

    public CancelAllTaskBySession(final ImmutableList<Ds3JobTask> tasks,
                                  final JobInterruptionStore jobInterruptionStore,
                                  final LoggingService loggingService) {
        this.tasks = tasks;
        this.jobInterruptionStore = jobInterruptionStore;
        this.loggingService = loggingService;
    }

    @Override
    protected Optional<Object> call() throws Exception {
        tasks.forEach(task-> {
            try {
                final String jobId = task.getJobId().toString();
                final Ds3Client ds3Client = task.getDs3Client();
                task.cancel();
                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId, ds3Client.getConnectionDetails()
                        .getEndpoint(), null, loggingService);
                ds3Client.cancelJobSpectraS3(new CancelJobSpectraS3Request(jobId));
            } catch (final Exception e) {
                LOG.error("Failed to cancel job", e);
            }
        });
        return Optional.empty();
    }
}
