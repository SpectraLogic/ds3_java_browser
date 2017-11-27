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
import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.ds3client.commands.spectrads3.GetActiveJobSpectraS3Request
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.metadata.MetadataAccessImpl
import com.spectralogic.ds3client.models.JobStatus
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.gui.services.jobService.stage.PrepStage
import com.spectralogic.dsbrowser.gui.services.jobService.stage.TeardownStage
import com.spectralogic.dsbrowser.gui.services.jobService.stage.TransferStage
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.StringBuilderUtil
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

class PutJob(private val putJobData: PutJobData, private val client: Ds3Client, private val bucket: String) : JobService(), PrepStage<PutJobData>, TransferStage, TeardownStage {

    private var job: Ds3ClientHelpers.Job? = null
    private var startTime: Instant = Instant.now()
    private var log: Logger = LoggerFactory.getLogger(PutJob::class.java)

    override fun finishedCompletable(): Completable {
        return Completable.fromAction {
            val resources = prepare(putJobData)
            transfer(resources)
            tearDown()
        }
    }

    override fun jobUUID(): UUID = getJob().jobId

    private fun getJob(): Ds3ClientHelpers.Job {
        if (job == null) {
            job = Ds3ClientHelpers.wrap(client).startWriteJob(bucket, putJobData.buildDs3Objects())
        }
        return job!!
    }


    override fun prepare(resources: PutJobData): Ds3ClientHelpers.Job {
        title.set("Preparing")
        val job: Ds3ClientHelpers.Job = getJob()
        val getActiveJobSpectraS3Request = GetActiveJobSpectraS3Request(job.jobId)
        totalJob.set(client.getActiveJobSpectraS3(getActiveJobSpectraS3Request).activeJobResult.originalSizeInBytes)
        if (putJobData.hasMetadata()) {
            job.withMetadata(MetadataAccessImpl(putJobData.prefixMap()))
        }
        job.attachFailureEventListener { putJobData.loggingService.logMessage(it.withObjectNamed(), LogType.ERROR) }
        job.attachDataTransferredListener { sent.set(it + sent.get()) }
        job.attachWaitingForChunksListener { waitForChunks(it) }
        job.attachObjectCompletedListener { updateStatistics(it) }

        return job
    }

    override fun transfer(job: Ds3ClientHelpers.Job) {
        title.set("Transferring " + jobUUID())
        startTime = Instant.now()
        job.transfer { s: String? -> putJobData.getObjectChannelBuilder(s).buildChannel(s!!.removePrefix(putJobData.targetDir)) }
    }

    override fun tearDown() {
        message.set("In Cache, Waiting to complete")
        totalJob.set(1)
        sent.set(1)
        visible.bind(putJobData.settingsStore.showCachedJobSettings.showCachedJobEnableProperty())
        val disposable: Disposable = Observable.interval(60, TimeUnit.SECONDS)
                .takeUntil({ _ -> isCompleted() })
                .retry { throwable ->
                    putJobData.loggingService.logMessage("Error checking status of job " + jobUUID() + " will retry", LogType.ERROR)
                    log.error("Unable to check status of job " + jobUUID(), throwable)
                    true
                }
                .ignoreElements()
                .subscribe()
        while (!disposable.isDisposed) {
            Thread.sleep(1000)
        }
    }

    private fun waitForChunks(s: Int) {
        try {
            putJobData.loggingService.logMessage("Waiting for chunks, will try again in " + DateTimeUtils.timeConversion(s.toLong()), LogType.INFO)
            Thread.sleep(s.toLong() * 1000)
        } catch (e: InterruptedException) {
            log.error("Did not receive chunks before timeout", e)
            putJobData.loggingService.logMessage("Did not receive chunks before timeout", LogType.ERROR)
        }
    }

    private fun updateStatistics(s: String?) {
        val elapsedSeconds = Instant.now().epochSecond - putJobData.getStartTime().epochSecond
        val sent = sent.get()
        val transferRate = sent / elapsedSeconds.toFloat()
        val timeRemaining: Float = if (transferRate != 0F) {
            totalJob.get().toFloat() / transferRate
        } else {
            0F
        }
        message.set(StringBuilderUtil.getTransferRateString(transferRate.toLong(), timeRemaining.toLong(), (sent),
                totalJob.longValue(), s, "").toString())
        putJobData.loggingService.logMessage(StringBuilderUtil.objectSuccessfullyTransferredString(s, putJobData.remotePath, putJobData.dateTimeUtils.nowAsString(), putJobData.remotePath).toString(), LogType.SUCCESS)
    }

    private fun isCompleted(): Boolean = client.getJobSpectraS3(GetJobSpectraS3Request(jobUUID())).masterObjectListResult.status == JobStatus.COMPLETED
}