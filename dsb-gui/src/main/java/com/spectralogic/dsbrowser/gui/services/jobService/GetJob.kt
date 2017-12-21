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

import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.ds3client.helpers.*
import com.spectralogic.ds3client.metadata.MetadataReceivedListenerImpl
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.gui.services.jobService.data.JobData
import com.spectralogic.dsbrowser.gui.services.jobService.stage.PrepStage
import com.spectralogic.dsbrowser.gui.services.jobService.stage.TeardownStage
import com.spectralogic.dsbrowser.gui.services.jobService.stage.TransferStage
import com.spectralogic.dsbrowser.gui.services.jobService.util.ChunkManagment
import com.spectralogic.dsbrowser.gui.services.jobService.util.Stats
import com.spectralogic.dsbrowser.gui.util.toByteRepresentation
import io.reactivex.Completable
import org.slf4j.LoggerFactory
import java.util.*

class GetJob(private val getJobData: JobData) : JobService(), PrepStage<JobData>, TransferStage, TeardownStage {
    override fun getDs3Client(): Ds3Client = getJobData.client()

    private val chunkManagment: ChunkManagment = ChunkManagment()
    private val stats: Stats = Stats(message, getJobData.loggingService(), getJobData.dateTimeUtils())

    private companion object {
        private val LOG = LoggerFactory.getLogger(GetJob::class.java)
    }

    override fun jobUUID(): UUID? = getJobData.jobId
    override fun prepare(resources: JobData): Ds3ClientHelpers.Job {
        title.set(getJobData.internationalize("preparingJob"))
        val job = getJobData.job!!
        totalJob.set(getJobData.jobSize())
        val totalJobSizeDisplay: String = getJobData.jobSize().toByteRepresentation()
        if (getJobData.shouldRestoreFileAttributes()) {
            job.attachMetadataReceivedListener { s, metadata ->
                var localFile = s.removePrefix(getJobData.prefixMap.get(s).toString())
                if (localFile.startsWith("/"))
                    localFile = localFile.removePrefix("/")
                MetadataReceivedListenerImpl(getJobData.targetPath()).metadataReceived(localFile, metadata)
            }
        }
        job.attachDataTransferredListener(DataTransferredListener {
            sent.set(it + sent.get())
            stats.updateStatistics(getJobData.lastFile, getJobData.getStartTime(), sent, totalJob, totalJobSizeDisplay, getJobData.targetPath(), getJobData.targetPath(), false)
        })
        job.attachObjectCompletedListener(ObjectCompletedListener {
            getJobData.lastFile = it
            stats.updateStatistics(getJobData.lastFile, getJobData.getStartTime(), sent, totalJob, totalJobSizeDisplay, getJobData.targetPath(), getJobData.targetPath(), true)
        })
        job.attachWaitingForChunksListener(WaitingForChunksListener { chunkManagment.waitForChunks(it, getJobData.loggingService(), LOG) })
        job.attachFailureEventListener { getJobData.loggingService().logMessage(it.toString(), LogType.ERROR) }
        getJobData.saveJob(totalJob.get())
        return job
    }

    override fun transfer(job: Ds3ClientHelpers.Job) {
        title.set("Transferring GET Job ${job.jobId} started at ${getJobData.dateTimeUtils().nowAsString()}")
        getJobData.loggingService().logMessage(getJobData.internationalize("starting") + " GET " + job.jobId, LogType.SUCCESS)
        getJobData.setStartTime()
        job.transfer { transferName ->
            getJobData.getObjectChannelBuilder(getJobData.prefixMap.get(transferName).toString() + "/").buildChannel(transferName)
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
