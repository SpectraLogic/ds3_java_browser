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

import com.spectralogic.ds3client.commands.spectrads3.GetActiveJobSpectraS3Request
import com.spectralogic.ds3client.helpers.*
import com.spectralogic.ds3client.metadata.MetadataReceivedListenerImpl
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.gui.services.jobService.stage.PrepStage
import com.spectralogic.dsbrowser.gui.services.jobService.stage.TeardownStage
import com.spectralogic.dsbrowser.gui.services.jobService.stage.TransferStage
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.StringBuilderUtil
import io.reactivex.Completable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class GetJob(private val getJobData: GetJobData) : JobService(), PrepStage<GetJobData>, TransferStage, TeardownStage {
    override fun jobUUID(): UUID? = job?.jobId

    private val log: Logger = LoggerFactory.getLogger(GetJob::class.java)

    private var job: Ds3ClientHelpers.Job? = null

    override fun prepare(resources: GetJobData): Ds3ClientHelpers.Job {
        title.set("Preparing Job")
        val job = getJobData.getJob()
        this.job = job
        title.set("Transferring Job " + job.jobId)
        val getActiveJobSpectraS3Request = GetActiveJobSpectraS3Request(job.jobId)
        totalJob.set(getJobData.client.getActiveJobSpectraS3(getActiveJobSpectraS3Request).activeJobResult.originalSizeInBytes)
        if (getJobData.hasMetadata()) {
            job.attachMetadataReceivedListener { s, metadata -> MetadataReceivedListenerImpl(getJobData.localPath.toString()).metadataReceived(s, metadata) }
        }
        job.attachDataTransferredListener(DataTransferredListener { sent.set(it + sent.get()) })
        job.attachObjectCompletedListener(ObjectCompletedListener{ updateStatistics(it) })
        job.attachWaitingForChunksListener(WaitingForChunksListener { waitForChunks(it) })
        job.attachFailureEventListener { getJobData.loggingService.logMessage(it.withObjectNamed(), LogType.ERROR) }
        return job
    }

    private fun updateStatistics(s: String?) {
        val elapsedSeconds = Instant.now().epochSecond - getJobData.getStartTime().epochSecond
        val sent = sent.get()
        val transferRate = sent / elapsedSeconds.toFloat()
        val timeRemaining: Float = if (transferRate != 0F) {
            totalJob.get().toFloat() / transferRate
        } else {
            0F
        }
        message.set(StringBuilderUtil.getTransferRateString(transferRate.toLong(), timeRemaining.toLong(), (sent),
                totalJob.longValue(), s, "").toString())
        getJobData.loggingService.logMessage(StringBuilderUtil.objectSuccessfullyTransferredString(s, getJobData.localPath.toString(), getJobData.dateTimeUtils.nowAsString(), null).toString(), LogType.SUCCESS)
    }

    private fun waitForChunks(s: Int) {
        try {
            getJobData.loggingService.logMessage("Waiting for chunks, will try again in " + DateTimeUtils.timeConversion(s.toLong()), LogType.INFO)
            Thread.sleep(s.toLong() * 1000)
        } catch (e: InterruptedException) {
            log.error("Did not receive chunks before timeout", e)
            getJobData.loggingService.logMessage("Did not receive chunks before timeout", LogType.ERROR)
        }
    }


    override fun transfer(job: Ds3ClientHelpers.Job) {
        getJobData.setStartTime()
        job.transfer {s: String? ->
            getJobData.getObjectChannelBuilder(getJobData.prefixMap().get(s)).buildChannel(s)
        }
    }

    override fun tearDown() {
    }

    override fun finishedCompletable(): Completable {
        return Completable.fromAction {
            val prep = prepare(getJobData)
            transfer(prep)
            tearDown()
        }
    }
}
