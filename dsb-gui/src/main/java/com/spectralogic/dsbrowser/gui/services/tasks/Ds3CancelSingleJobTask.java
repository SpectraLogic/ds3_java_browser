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

import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Response;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import com.spectralogic.dsbrowser.gui.util.ResourceBundleProperties;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.ResourceBundle;

import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class Ds3CancelSingleJobTask extends Task {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3CancelSingleJobTask.class);
    private final ResourceBundle resourceBundle;
    private final String jobId;
    private final EndpointInfo endpointInfo;
    private final JobInterruptionStore jobInterruptionStore;
    private final String jobType;
    private final LoggingService loggingService;

    public Ds3CancelSingleJobTask(final String jobId,
                                  final EndpointInfo endpointInfo,
                                  final JobInterruptionStore jobInterruptionStore,
                                  final String jobType,
                                  final LoggingService loggingService) {
        this.jobType = jobType;
        this.resourceBundle = ResourceBundleProperties.getResourceBundle();
        this.jobId = jobId;
        this.endpointInfo = endpointInfo;
        this.jobInterruptionStore = jobInterruptionStore;
        this.loggingService = loggingService;
    }

    @Override
    protected CancelJobSpectraS3Response call() {
        try {
            final CancelJobSpectraS3Response cancelJobSpectraS3Response = endpointInfo.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(jobId));
            loggingService.logMessage(resourceBundle.getString("cancelJobStatus") + StringConstants.SPACE + cancelJobSpectraS3Response, LogType.SUCCESS);
        } catch (final IOException e) {
            LOG.error("Unable to cancel " + jobType + "  job " + jobId, e);
            loggingService.logMessage(resourceBundle.getString("failedCancelJob") + StringConstants.SPACE + e, LogType.ERROR);
            fireEvent(new Event(WorkerStateEvent.WORKER_STATE_FAILED));
        } finally {
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId, endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter(), loggingService);
            ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
        }
        return null;
    }
}
