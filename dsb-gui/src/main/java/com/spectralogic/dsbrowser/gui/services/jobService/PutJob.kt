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

import com.google.common.collect.ImmutableMap
import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.metadata.MetadataAccessImpl
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.gui.services.jobService.data.JobData
import com.spectralogic.dsbrowser.gui.services.jobService.util.ChunkManagment
import com.spectralogic.dsbrowser.gui.services.jobService.util.Stats
import com.spectralogic.dsbrowser.gui.util.toByteRepresentation
import io.reactivex.Completable
import io.reactivex.Observable
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

class PutJob(private val putJobData: JobData) : JobService() {
    override fun getDs3Client(): Ds3Client = putJobData.client()

    private val chunkManagement: ChunkManagment = ChunkManagment()
    private val stats: Stats = Stats(message, putJobData.loggingService(), putJobData.dateTimeUtils())
    private var wasCancelled = false

    private companion object {
        private val LOG = LoggerFactory.getLogger(GetJob::class.java)
    }

    override fun jobUUID(): UUID = putJobData.jobId

    override fun finishedCompletable(): Completable {

        return Completable.fromAction {
            val resources = prepare()
            transfer(resources)
            if (!wasCancelled) {
                tearDown()
            }
        }
    }

    private fun prepare(): Ds3ClientHelpers.Job {
        title.set(putJobData.internationalize("preparingJob"))
        val job: Ds3ClientHelpers.Job = putJobData.job
        LOG.info("Job ID is {}", job.jobId)
        totalJob.set(putJobData.jobSize())
        val totalJobMessage: String = putJobData.jobSize().toByteRepresentation()
        if (putJobData.shouldRestoreFileAttributes()) {
            job.withMetadata(MetadataAccessImpl(ImmutableMap.copyOf(putJobData.prefixMap)))
        }
        job.attachFailureEventListener { event ->
            putJobData.loggingService().logMessage(event.toString(), LogType.ERROR)
        }
        job.attachDataTransferredListener {
            sent.set(it + sent.get())
            stats.updateStatistics(putJobData.getStartTime(), sent, totalJob, totalJobMessage, putJobData.targetPath(), putJobData.bucket, false)
        }
        job.attachWaitingForChunksListener { chunkManagement.waitForChunks(it, putJobData.loggingService(), LOG) }
        job.attachObjectCompletedListener {
            stats.updateStatistics(putJobData.getStartTime(), sent, totalJob, totalJobMessage, putJobData.targetPath(), putJobData.bucket, true, it)
        }
        putJobData.saveJob(totalJob.get())
        return job
    }

    private fun transfer(job: Ds3ClientHelpers.Job) {
        putJobData.setStartTime()
        title.set(putJobData.runningTitle())
        putJobData.loggingService().logMessage(putJobData.internationalize("starting") + " PUT " + job.jobId, LogType.SUCCESS)
        job.transfer { transferName: String ->
            putJobData.getObjectChannelBuilder(transferName).buildChannel(transferName.removePrefix(putJobData.targetPath()))
        }
    }

    private fun tearDown() {
        if (totalJob.get() < 1) {
            totalJob.set(1L)
            sent.set(1L)
        }
        LOG.info("Job {} is in cache, waiting to complete",  putJobData.job.jobId)
        message.set("In Cache, Waiting to complete")
        sent.set(totalJob.value)
        visible.bind(putJobData.showCachedJobProperty())
        Observable.interval(60, TimeUnit.SECONDS)
                .takeUntil({ _ -> wasCancelled || putJobData.isCompleted()})
                .retry { throwable ->
                    putJobData.loggingService().logMessage("Error checking status of job " + jobUUID() + " will retry", LogType.ERROR)
                    LOG.error("Unable to check status of job " + jobUUID(), throwable)
                    when (throwable) {
                        is IllegalStateException -> false
                        else -> true
                    }
                }
                .ignoreElements()
                .blockingAwait()
        sent.set(totalJob.value)
        putJobData.removeJob()
    }

    override fun cancel() {
        wasCancelled = true
        putJobData.job.cancel()
    }
}