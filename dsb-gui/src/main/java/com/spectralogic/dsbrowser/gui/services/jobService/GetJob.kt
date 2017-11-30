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

import com.spectralogic.ds3client.helpers.*
import com.spectralogic.ds3client.metadata.MetadataReceivedListenerImpl
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.gui.services.jobService.data.JobData
import com.spectralogic.dsbrowser.gui.services.jobService.stage.PrepStage
import com.spectralogic.dsbrowser.gui.services.jobService.stage.TeardownStage
import com.spectralogic.dsbrowser.gui.services.jobService.stage.TransferStage
import com.spectralogic.dsbrowser.gui.services.jobService.util.ChunkWaiter
import com.spectralogic.dsbrowser.gui.services.jobService.util.Stats
import io.reactivex.Completable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class GetJob(private val getJobData: JobData) : JobService(), PrepStage<JobData>, TransferStage, TeardownStage {
    private val chunkWaiter: ChunkWaiter = ChunkWaiter()
    private val stats: Stats = Stats()

    private companion object {
        private val LOG = LoggerFactory.getLogger(GetJob::class.java)
    }

    override fun jobUUID(): UUID = getJobData.job!!.jobId
    override fun prepare(resources: JobData): Ds3ClientHelpers.Job {
        title.set("Preparing Job")
        val job = getJobData.job!!
        title.set("Transferring GET Job ${job.jobId}")
        totalJob.set(getJobData.jobSize())
        if (getJobData.shouldRestoreFileAttributes()) {
            job.attachMetadataReceivedListener { s, metadata -> MetadataReceivedListenerImpl(getJobData.targetPath()).metadataReceived(s, metadata) }
        }
        job.attachDataTransferredListener(DataTransferredListener { sent.set(it + sent.get()) })
        job.attachObjectCompletedListener(ObjectCompletedListener { stats.updateStatistics(it, getJobData.getStartTime(), sent, totalJob, message, getJobData.loggingService(), getJobData.targetPath(), getJobData.dateTimeUtils(), getJobData.targetPath()) })
        job.attachWaitingForChunksListener(WaitingForChunksListener { chunkWaiter.waitForChunks(it, getJobData.loggingService(), LOG) })
        job.attachFailureEventListener { getJobData.loggingService().logMessage(it.withObjectNamed(), LogType.ERROR) }
        getJobData.saveJob(totalJob.get())
        return job
    }

    override fun transfer(job: Ds3ClientHelpers.Job) {
        getJobData.setStartTime()
        job.transfer { s: String -> getJobData.getObjectChannelBuilder(getJobData.prefixMap.get(s).toString() + "/").buildChannel(s)
        }
    }

    override fun tearDown() {
        getJobData.removeJob()
    }

    override fun finishedCompletable(): Completable {
        return Completable.fromAction {
            val prep = prepare(getJobData)
            transfer(prep)
            tearDown()
        }
    }
}
