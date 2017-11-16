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
import com.spectralogic.dsbrowser.gui.services.jobService.Util.OverWritingObjectChannelBuilder
import com.spectralogic.dsbrowser.gui.services.jobService.stage.PrepStage
import com.spectralogic.dsbrowser.gui.services.jobService.stage.TeardownStage
import com.spectralogic.dsbrowser.gui.services.jobService.stage.TransferStage
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.StringBuilderUtil
import io.reactivex.Completable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class GetJob(private val getJobData: GetJobData) : JobService(), PrepStage<GetJobData>, TransferStage, TeardownStage {
    private val log: Logger = LoggerFactory.getLogger(GetJob::class.java)

    override fun prepare(resources: GetJobData): Pair<Ds3ClientHelpers.Job, Ds3ClientHelpers.ObjectChannelBuilder> {
        title.set("Preparing Job")
        val job = getJobData.getJob()
        title.set("Transferring Job " + job.jobId)
        totalJob.set(getJobData.client.getActiveJobSpectraS3(GetActiveJobSpectraS3Request(job.jobId)).activeJobResult.originalSizeInBytes)
        if (getJobData.hasMetadata()) {
            job.attachMetadataReceivedListener { s, metadata -> MetadataReceivedListenerImpl(getJobData.localPath.toString()).metadataReceived(s, metadata) }
        }
        job.attachDataTransferredListener { sent.set(sent.get() + it) }
        job.attachObjectCompletedListener { updateStatistics(it) }
        job.attachWaitingForChunksListener { waitForChunks(it) }
        return Pair(job, getJobData.getObjectChannelBuilder())
    }

    private fun updateStatistics(s: String?) {
        val elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(Instant.now().toEpochMilli() - getJobData.startTime.epochSecond)
        val transferRate = if (elapsedSeconds != 0L) {
            (sent.get().toLong() / 2) / elapsedSeconds
        } else {
            0L
        }
        val timeRemaining = if (transferRate != 0L) {
            (totalJob.get() / 2) / transferRate
        } else {
            0L
        }
        message.set(StringBuilderUtil.getTransferRateString(transferRate, timeRemaining.toLong(), AtomicLong(sent.longValue()),
                totalJob.longValue(), s, getJobData.remotePrefix).toString())
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


    override fun transfer(job: Ds3ClientHelpers.Job, ocb: Ds3ClientHelpers.ObjectChannelBuilder) {
        job.transfer(ocb)
    }

    override fun tearDown() {
    }

    override fun finishedCompletable(): Completable {
        return Completable.fromAction {
            val prep = prepare(getJobData)
            transfer(prep.first, prep.second)
            tearDown()
        }
    }
}
