/*
 * ***************************************************************************
 *   Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *   this file except in compliance with the License. A copy of the License is located at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file.
 *   This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *   CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *   specific language governing permissions and limitations under the License.
 * ***************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.jobService.factories

import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter
import com.spectralogic.dsbrowser.gui.services.jobService.JobTask
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap
import com.spectralogic.dsbrowser.util.exists
import javafx.application.Platform
import javafx.concurrent.WorkerStateEvent
import org.slf4j.Logger
import java.io.IOException
import java.util.*

fun removeJob(uuid: UUID, client: Ds3Client, jobInterruptionStore: JobInterruptionStore, deepStorageBrowserPresenter: DeepStorageBrowserPresenter, loggingService: LoggingService) {
    ParseJobInterruptionMap.removeJobID(jobInterruptionStore, uuid.toString(), client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter, loggingService)
}
