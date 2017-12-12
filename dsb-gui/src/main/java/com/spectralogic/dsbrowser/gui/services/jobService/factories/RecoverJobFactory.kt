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
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo
import com.spectralogic.dsbrowser.gui.services.JobWorkers
import com.spectralogic.dsbrowser.gui.services.jobService.JobTask
import com.spectralogic.dsbrowser.gui.services.jobService.JobTaskElement
import com.spectralogic.dsbrowser.util.andThen
import com.spectralogic.dsbrowser.gui.services.jobService.RecoverJob
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap
import javafx.application.Platform
import javafx.concurrent.WorkerStateEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import javax.inject.Inject

class RecoverJobFactory @Inject constructor(private val jobTaskElementFactory: JobTaskElement.JobTaskElementFactory,
                                            private val jobWorkers: JobWorkers,
                                            private val loggingService: LoggingService,
                                            private val jobInterruptionStore: JobInterruptionStore,
                                            private val deepStorageBrowserPresenter: DeepStorageBrowserPresenter) {
    private companion object {
        private val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    }

    public fun create(uuid: UUID, endpointInfo: EndpointInfo, refreshBehavior: () -> Unit) {
        val client = endpointInfo.client
        jobTaskElementFactory.create(client)
                .let { RecoverJob(uuid, endpointInfo, it, client) }
                .let { it.getTask() }
                .let { JobTask(it) }
                .apply {
                    setOnCancelled(onCancelled(client).andThen(refreshBehavior))
                    setOnFailed(onFailed(client).andThen(refreshBehavior))
                    setOnRunning(onRunning())
                    setOnScheduled(onScheduled())
                    setOnSucceeded(onSucceeded().andThen(refreshBehavior))
                }
                .also { jobWorkers.execute(it) }
    }

    private fun JobTask.onSucceeded(): (WorkerStateEvent) -> Unit = {
        LOG.info("Recovery Job completed successfully")
    }

    private fun JobTask.onCancelled(client: Ds3Client): (WorkerStateEvent) -> Unit = {
        val uuid: UUID? = this.jobId
        if (uuid != null) {
            try {
                Platform.runLater {
                    client.cancelJobSpectraS3(CancelJobSpectraS3Request(uuid))
                }
            } catch (e: IOException) {
                LOG.error("Failed to cancel job", e)
                loggingService.logMessage("Could not cancel job", LogType.ERROR)
            }
            LOG.info("Get Job cancelled")
            loggingService.logMessage("GET Job Cancelled", LogType.INFO)
            ParseJobInterruptionMap.removeJobID(jobInterruptionStore, uuid.toString(), client.connectionDetails.endpoint, deepStorageBrowserPresenter, loggingService)
        }
    }

    private fun JobTask.onFailed(client: Ds3Client): (WorkerStateEvent) -> Unit = {
        val uuid = jobId
        if (uuid != null) {
            ParseJobInterruptionMap.removeJobID(jobInterruptionStore, uuid.toString(), client.connectionDetails.endpoint, deepStorageBrowserPresenter, loggingService)
        }
        LOG.error("Failed to recover $uuid", it.source.exception)
        loggingService.logMessage("Failed to recover ${uuid.toString()} on ${client.connectionDetails.endpoint}", LogType.ERROR)

    }

    private fun JobTask.onRunning(): (WorkerStateEvent) -> Unit = {

    }

    private fun JobTask.onScheduled(): (WorkerStateEvent) -> Unit = {

    }
}