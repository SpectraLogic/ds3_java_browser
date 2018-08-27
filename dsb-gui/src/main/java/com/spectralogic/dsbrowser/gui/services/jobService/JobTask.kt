/*
 * ****************************************************************************
 *    Copyright 2014-2018 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *  ****************************************************************************
 */
package com.spectralogic.dsbrowser.gui.services.jobService

import com.github.thomasnield.rxkotlinfx.observeOnFx
import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request
import com.spectralogic.ds3client.commands.spectrads3.GetActiveJobSpectraS3Request
import com.spectralogic.ds3client.commands.spectrads3.GetActiveJobsSpectraS3Request
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request
import com.spectralogic.ds3client.networking.FailedRequestException
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter
import com.spectralogic.dsbrowser.gui.services.Workers
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap
import com.spectralogic.dsbrowser.util.exists
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.concurrent.WorkerStateEvent
import org.slf4j.Logger
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import java.util.function.Supplier

class JobTask(private val wrappedJob: JobFacade) : Ds3JobTask() {
    @Throws(Throwable::class)
    override fun executeJob() {
        ds3Client = wrappedJob.getDs3Client()
        wrappedJob.titleObservable()
                .observeOn(JavaFxScheduler.platform())
                .doOnNext { title: String -> updateTitle(title) }
                .subscribe()

        wrappedJob.messageObservable()
                .observeOn(JavaFxScheduler.platform())
                .doOnNext { message: String -> updateMessage(message) }
                .subscribe()

        wrappedJob.jobSizeObservable()
                .observeOn(JavaFxScheduler.platform())
                .doOnNext { size: Number -> updateProgress(0L, size.toLong()) }
                .subscribe()

        wrappedJob.sentObservable()
                .observeOn(JavaFxScheduler.platform())
                .doOnNext { size: Number -> updateProgress(size.toLong(), wrappedJob.totalJobSizeAsProperty().get()) }
                .subscribe()

        wrappedJob.visabilityObservable()
                .observeOnFx()
                .doOnNext { visible: Boolean -> isVisible.set(visible) }
                .subscribe()

        wrappedJob.finishedCompletable(Supplier { isCancelled }).blockingAwait()
    }

    override fun getJobId(): UUID? = wrappedJob.jobUUID()

    public val isVisible: BooleanProperty = SimpleBooleanProperty(true)

    public fun onCancelled(client: Ds3Client,
                           type: String,
                           log: Logger,
                           loggingService: LoggingService,
                           jobInterruptionStore: JobInterruptionStore,
                           workers: Workers,
                           deepStorageBrowserPresenter: DeepStorageBrowserPresenter): (WorkerStateEvent) -> Unit = {
        jobId.exists { uuid ->
                workers.execute {
                    try {
                        client.cancelJobSpectraS3(CancelJobSpectraS3Request(uuid))
                        log.info("{} Job cancelled", type)
                        loggingService.logMessage("$type Job Cancelled", LogType.INFO)
                    } catch (e: Throwable) {
                        log.error("Failed to cancel $type job", e)
                        loggingService.logMessage("Could not cancel $type job", LogType.ERROR)
                    }
                }
            ParseJobInterruptionMap.removeJobID( jobInterruptionStore, uuid.toString(), client.connectionDetails.endpoint, deepStorageBrowserPresenter, loggingService )
            }
    }

    fun onFailed(client: Ds3Client,
                 jobInterruptionStore: JobInterruptionStore,
                 deepStorageBrowserPresenter: DeepStorageBrowserPresenter,
                 loggingService: LoggingService,
                 log: Logger,
                 workers: Workers,
                 type: String): (WorkerStateEvent) -> Unit = { worker: WorkerStateEvent ->
        val throwable: Throwable = worker.source.exception
        jobId.exists {
            workers.execute {
                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, it.toString(), client.connectionDetails.endpoint, deepStorageBrowserPresenter, loggingService)
            }
        }
        log.error("$type Job failed", throwable)
        loggingService.logMessage("$type Job failed with message: ${throwable.message}", LogType.ERROR)
    }

    fun JobTask.onSucceeded(type: String, log: Logger): (WorkerStateEvent) -> Unit = {
        log.info("$type Job completed successfully")
    }
    fun JobTask.onScheduled() = { _: WorkerStateEvent -> }

}