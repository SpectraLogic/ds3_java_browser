/*
 * ****************************************************************************
 *    Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
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

import com.google.common.collect.ImmutableMap
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.metadata.MetadataAccessImpl
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.gui.services.jobService.data.JobData
import com.spectralogic.dsbrowser.gui.services.jobService.stage.PrepStage
import com.spectralogic.dsbrowser.gui.services.jobService.stage.TeardownStage
import com.spectralogic.dsbrowser.gui.services.jobService.stage.TransferStage
import com.spectralogic.dsbrowser.gui.services.jobService.util.ChunkWaiter
import com.spectralogic.dsbrowser.gui.services.jobService.util.Stats
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

class PutJob(private val putJobData: JobData) : JobService(), PrepStage<JobData>, TransferStage, TeardownStage {

    private var job: Ds3ClientHelpers.Job? = null
    private val chunkWaiter: ChunkWaiter = ChunkWaiter()
    private val stats: Stats = Stats()
    private companion object {
        private val LOG = LoggerFactory.getLogger(GetJob::class.java)
    }

    override fun jobUUID(): UUID = putJobData.job!!.jobId

    override fun finishedCompletable(): Completable {
        return Completable.fromAction {
            val resources = prepare(putJobData)
            transfer(resources)
            tearDown()
        }
    }

    override fun prepare(resources: JobData): Ds3ClientHelpers.Job {
        title.set(putJobData.internationalize("preparingJob"))
        val job: Ds3ClientHelpers.Job = putJobData.job!!
        totalJob.set(putJobData.jobSize())
        if (putJobData.shouldRestoreFileAttributes()) {
            job.withMetadata(MetadataAccessImpl(ImmutableMap.copyOf(putJobData.prefixMap)))
        }
        job.attachFailureEventListener { event ->
            putJobData.loggingService().logMessage(event.toString(), LogType.ERROR)
        }
        job.attachDataTransferredListener {
            sent.set(it + sent.get())
            stats.updateStatistics(putJobData.lastFile, putJobData.getStartTime(), sent, totalJob, message, putJobData.loggingService(), putJobData.targetPath(), putJobData.dateTimeUtils(), putJobData.bucket, false)
        }
        job.attachWaitingForChunksListener { chunkWaiter.waitForChunks(it, putJobData.loggingService(), LOG) }
        job.attachObjectCompletedListener {
            putJobData.lastFile = it
            stats.updateStatistics(putJobData.lastFile, putJobData.getStartTime(), sent, totalJob, message, putJobData.loggingService(), putJobData.targetPath(), putJobData.dateTimeUtils(), putJobData.bucket, true)
        }
        putJobData.saveJob(totalJob.get())
        return job
    }

    override fun transfer(job: Ds3ClientHelpers.Job) {
        title.set("Transferring PUT ${jobUUID()}")
        putJobData.loggingService().logMessage(putJobData.internationalize("starting") + " PUT " + job.jobId, LogType.SUCCESS)
        putJobData.setStartTime()
        job.transfer { transferName: String ->
            putJobData.loggingService().logMessage(putJobData.internationalize("starting") + transferName, LogType.SUCCESS)
            putJobData.getObjectChannelBuilder(transferName).buildChannel(transferName.removePrefix(putJobData.targetPath()))
        }
    }

    override fun tearDown() {
        message.set("In Cache, Waiting to complete")
        totalJob.set(1)
        sent.set(1)
        visible.bind(putJobData.showCachedJobProperty())
        val disposable: Disposable = Observable.interval(60, TimeUnit.SECONDS)
                .takeUntil({ _ -> putJobData.isCompleted() })
                .retry { throwable ->
                    putJobData.loggingService().logMessage("Error checking status of job " + jobUUID() + " will retry", LogType.ERROR)
                    LOG.error("Unable to check status of job " + jobUUID(), throwable)
                    true
                }
                .ignoreElements()
                .subscribe()
        while (!disposable.isDisposed) {
            Thread.sleep(1000)
        }
        totalJob.set(1)
        sent.set(1)
        putJobData.removeJob()
    }
}