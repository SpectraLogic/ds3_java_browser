/*
 * ***************************************************************************
 *   Copyright 2014-2018 Spectra Logic Corporation. All Rights Reserved.
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

package com.spectralogic.dsbrowser.gui.services.jobService.data

import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.ds3client.commands.spectrads3.GetActiveJobSpectraS3Request
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request
import com.spectralogic.ds3client.commands.spectrads3.PutBulkJobSpectraS3Request
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.helpers.FileObjectPutter
import com.spectralogic.ds3client.helpers.WriteJobImpl
import com.spectralogic.ds3client.helpers.events.ConcurrentEventRunner
import com.spectralogic.ds3client.helpers.events.SameThreadEventRunner
import com.spectralogic.ds3client.helpers.options.WriteJobOptions
import com.spectralogic.ds3client.helpers.strategy.blobstrategy.BlackPearlChunkAttemptRetryDelayBehavior
import com.spectralogic.ds3client.helpers.strategy.blobstrategy.MaxChunkAttemptsRetryBehavior
import com.spectralogic.ds3client.helpers.strategy.blobstrategy.PutSequentialBlobStrategy
import com.spectralogic.ds3client.helpers.strategy.transferstrategy.EventDispatcherImpl
import com.spectralogic.ds3client.helpers.strategy.transferstrategy.TransferStrategyBuilder
import com.spectralogic.ds3client.models.JobStatus
import com.spectralogic.ds3client.models.Priority
import com.spectralogic.ds3client.models.bulk.Ds3Object
import com.spectralogic.ds3client.utils.Guard
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.services.jobService.JobTaskElement
import com.spectralogic.dsbrowser.gui.services.jobService.util.EmptyChannelBuilder
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap
import javafx.beans.property.BooleanProperty
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream
import kotlin.streams.toList

data class PutJobData(private val items: List<Pair<String, Path>>,
                      private val targetDir: String,
                      override val bucket: String,
                      private val jobTaskElement: JobTaskElement) : JobData {

    override fun runningTitle(): String {
        val transferringPut = jobTaskElement.resourceBundle.getString("transferringPut")
        val jobId = job.jobId
        val startedAt = jobTaskElement.resourceBundle.getString("startedAt")
        val started = jobTaskElement.dateTimeUtils.format(getStartTime())
        return "$transferringPut $jobId $startedAt $started"
    }


    override val jobId: UUID by lazy { job.jobId }
    override fun client(): Ds3Client = jobTaskElement.client

    override fun internationalize(labelName: String): String = jobTaskElement.resourceBundle.getString(labelName)
    override fun modifyJob(job: Ds3ClientHelpers.Job) {
        job.withMaxParallelRequests(jobTaskElement.settingsStore.processSettings.maximumNumberOfParallelThreads)
    }

    override val job: Ds3ClientHelpers.Job by lazy {
                val ds3Objects = items.map { dataToDs3Objects(it) }.flatMap { it.asIterable() }
                ds3Objects.map { pair: Pair<Ds3Object, Path> -> Pair<String, Path>(pair.first.name, pair.second) }
                    .forEach { prefixMap.put(it.first,it.second)}
                val priority =
                    if (jobTaskElement.savedJobPrioritiesStore.jobSettings.putJobPriority.equals("Data Policy Default (no change)")) {
                        null
                    } else {
                        Priority.valueOf(jobTaskElement.savedJobPrioritiesStore.jobSettings.putJobPriority)
                    }
                val request = PutBulkJobSpectraS3Request(
                    bucket,
                    ds3Objects.map { it.first }
                ).withPriority(priority)
                val response = jobTaskElement.client.putBulkJobSpectraS3(request)
                val eventDispatcher = EventDispatcherImpl(ConcurrentEventRunner())
                val blobStrategy = PutSequentialBlobStrategy(
                        jobTaskElement.client,
                        response.masterObjectList,
                        eventDispatcher,
                        MaxChunkAttemptsRetryBehavior(1),
                        BlackPearlChunkAttemptRetryDelayBehavior(eventDispatcher)
                    )
                val transferStrategyBuilder = TransferStrategyBuilder()
                    .withDs3Client(jobTaskElement.client)
                    .withMasterObjectList(response.masterObjectList)
                    .withBlobStrategy(blobStrategy)
                    .withChannelBuilder(null)
                    .withNumConcurrentTransferThreads(jobTaskElement.settingsStore.processSettings.maximumNumberOfParallelThreads)

                WriteJobImpl(transferStrategyBuilder)
            }

    override var prefixMap: MutableMap<String, Path> = mutableMapOf()

    override fun showCachedJobProperty(): BooleanProperty = jobTaskElement.settingsStore.showCachedJobSettings.showCachedJobEnableProperty()
    override fun loggingService(): LoggingService = jobTaskElement.loggingService
    override fun targetPath(): String = targetDir
    override fun dateTimeUtils(): DateTimeUtils = jobTaskElement.dateTimeUtils
    private var startTime: Instant = Instant.now()
    public override fun getStartTime(): Instant = startTime
    public override fun setStartTime(): Instant {
        startTime = Instant.now()
        return startTime
    }

    override fun getObjectChannelBuilder(prefix: String): Ds3ClientHelpers.ObjectChannelBuilder =
        EmptyChannelBuilder(FileObjectPutter(prefixMap.get(prefix)!!.parent), prefixMap.get(prefix)!!)

    private fun dataToDs3Objects(item: Pair<String, Path>): Sequence<Pair<Ds3Object, Path>> {
        val localDelim = item.second.fileSystem.separator
        val parent = item.second.parent
        val paths = item.second.toFile().walk(FileWalkDirection.TOP_DOWN)
                    .filter { !(it.isDirectory && Files.list(it.toPath()).use { f -> f.findAny().isPresent }) }
                    .map {
                        Ds3Object(if (it.isDirectory) {
                            targetDir + parent.relativize(it.toPath()).toString().replace(localDelim, "/") + "/"
                        } else {
                            targetDir + parent.relativize(it.toPath()).toString().replace(localDelim, "/")
                        }
                                , if (Files.isDirectory(it.toPath())) 0L else it.length())
                    }
                .map { Pair<Ds3Object, Path>(it, item.second) }
        return paths
    }

    override fun jobSize() = jobTaskElement.client.getActiveJobSpectraS3(GetActiveJobSpectraS3Request(job.jobId)).activeJobResult.originalSizeInBytes

    override fun shouldRestoreFileAttributes() = jobTaskElement.settingsStore.filePropertiesSettings.isFilePropertiesEnabled

    override fun isCompleted() = jobTaskElement.client.getJobSpectraS3(GetJobSpectraS3Request(job.jobId)).masterObjectListResult.status == JobStatus.COMPLETED
    override fun removeJob() {
        ParseJobInterruptionMap.removeJobIdFromFile(jobTaskElement.jobInterruptionStore, job.jobId.toString(), jobTaskElement.client.connectionDetails.endpoint)
    }

    override fun saveJob(jobSize: Long) {
        ParseJobInterruptionMap.saveValuesToFiles(jobTaskElement.jobInterruptionStore, prefixMap, mapOf(), jobTaskElement.client.connectionDetails.endpoint, job.jobId, jobSize, targetPath(), jobTaskElement.dateTimeUtils, "PUT", bucket)
    }

}