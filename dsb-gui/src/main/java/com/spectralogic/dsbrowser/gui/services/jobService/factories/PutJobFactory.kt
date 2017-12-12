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
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue
import com.spectralogic.dsbrowser.gui.services.JobWorkers
import com.spectralogic.dsbrowser.gui.services.jobService.GetJob
import com.spectralogic.dsbrowser.gui.services.jobService.JobTask
import com.spectralogic.dsbrowser.gui.services.jobService.JobTaskElement
import com.spectralogic.dsbrowser.gui.services.jobService.PutJob
import com.spectralogic.dsbrowser.gui.services.jobService.data.PutJobData
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap
import com.spectralogic.dsbrowser.util.andThen
import javafx.application.Platform
import javafx.concurrent.WorkerStateEvent
import javafx.scene.control.TreeItem
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import java.util.*
import javax.inject.Inject

class PutJobFactory @Inject constructor(private val loggingService: LoggingService,
                                        private val jobInterruptionStore: JobInterruptionStore,
                                        private val resourceBundle: ResourceBundle,
                                        private val deepStorageBrowserPresenter: DeepStorageBrowserPresenter,
                                        private val jobWorkers: JobWorkers,
                                        private val jobTaskElementFactory: JobTaskElement.JobTaskElementFactory) {

    private companion object {
        private val LOG = LoggerFactory.getLogger(PutJobFactory::class.java)
    }

    fun create(files: List<Pair<String, Path>>, bucket: String, targetDir: String, client: Ds3Client, refreshBehavior: () -> Unit = {}) {
        jobTaskElementFactory.create(client)
                .let { PutJobData(files, targetDir, bucket, it) }
                .let { PutJob(it) }
                .let { JobTask(it) }
                .apply { setOnCancelled(onCancelled(client).andThen(refreshBehavior)) }
                .apply { setOnRunning(onRunning()) }
                .apply { setOnFailed(onFailed(client).andThen(refreshBehavior)) }
                .apply { setOnScheduled(onScheduled()) }
                .apply { setOnSucceeded(onSuccess().andThen(refreshBehavior)) }
                .also { jobWorkers.execute(it) }
    }

    private fun onRunning() = { _: WorkerStateEvent -> LOG.info("BULK_PUT job now running.") }

    private fun onScheduled() = { _: WorkerStateEvent -> LOG.info("BULK_PUT job scheduled.") }

    private fun JobTask.onCancelled(client: Ds3Client): (WorkerStateEvent) -> Unit {
        return { _: WorkerStateEvent ->
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
                LOG.info("BULK_PUT job {} Canceled.", uuid)
                loggingService.logMessage(resourceBundle.getString("putJobCancelled"), LogType.SUCCESS)
                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, uuid.toString(), client.connectionDetails.endpoint, deepStorageBrowserPresenter, loggingService)
            }
        }
    }

    private fun JobTask.onSuccess(): (WorkerStateEvent) -> Unit {
        return { _: WorkerStateEvent ->
            LOG.info("BULK_PUT job {} Succeed.", this.jobId)
        }
    }

    private fun JobTask.onFailed(client: Ds3Client): (WorkerStateEvent) -> Unit {
        return { workerState: WorkerStateEvent ->
            val uuid: UUID? = this.jobId
            if (uuid != null) {
                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, uuid.toString(), client.connectionDetails.endpoint, deepStorageBrowserPresenter, loggingService)
            }
            val throwable: Throwable = workerState.source.exception
            LOG.error("Put Job Failed", throwable)
            loggingService.logMessage("Put Job Failed with message: " + throwable.javaClass.name + ": " + throwable.message, LogType.ERROR)
        }
    }
}