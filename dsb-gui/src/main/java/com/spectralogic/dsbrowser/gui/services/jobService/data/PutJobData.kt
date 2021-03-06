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
import com.spectralogic.ds3client.helpers.strategy.blobstrategy.BlackPearlChunkAttemptRetryDelayBehavior
import com.spectralogic.ds3client.helpers.strategy.blobstrategy.MaxChunkAttemptsRetryBehavior
import com.spectralogic.ds3client.helpers.strategy.blobstrategy.PutSequentialBlobStrategy
import com.spectralogic.ds3client.helpers.strategy.transferstrategy.EventDispatcherImpl
import com.spectralogic.ds3client.helpers.strategy.transferstrategy.TransferStrategyBuilder
import com.spectralogic.ds3client.models.JobStatus
import com.spectralogic.ds3client.models.Priority
import com.spectralogic.ds3client.models.bulk.Ds3Object
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.services.jobService.JobTaskElement
import com.spectralogic.dsbrowser.gui.services.jobService.util.EmptyChannelBuilder
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap
import javafx.beans.property.BooleanProperty
import java.io.File
import java.lang.RuntimeException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

data class PutJobData(
    private val items: List<Pair<String, Path>>,
    private val targetDir: String,
    override val bucket: String,
    private val jobTaskElement: JobTaskElement
) : JobData {

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
                val ds3Objects = items.map { pathToDs3Object(it.second) }.flatten()
                ds3Objects.map { pair: Pair<Ds3Object, Path> -> Pair<String, Path>(pair.first.name, pair.second) }
                    .forEach { prefixMap.put(it.first, it.second) }
                if (ds3Objects.isEmpty()) {
                    loggingService().logMessage("File list is empty, cannot create job", LogType.ERROR)
                    throw RuntimeException("File list is empty")
                }
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
    override fun getStartTime(): Instant = startTime
    override fun setStartTime(): Instant {
        startTime = Instant.now()
        return startTime
    }

    override fun getObjectChannelBuilder(prefix: String): Ds3ClientHelpers.ObjectChannelBuilder =
        EmptyChannelBuilder(FileObjectPutter(prefixMap.get(prefix)!!.parent), prefixMap.get(prefix)!!)

    private fun pathToDs3Object(path: Path): List<Pair<Ds3Object, Path>> {
        val parent = path.parent
        return path
            .toFile()
            .walk(FileWalkDirection.TOP_DOWN)
            .filter(::rejectEmptyDirectory)
            .filter(::rejectDeadLinks)
            .map(::addSize)
            .map { convertToDs3Object(it, parent) }
            .map { Pair(it, path) }
            .distinct()
            .toList()
    }

    private fun addSize(file: File): Pair<File, Long> =
        Pair(file, if (Files.isDirectory(file.toPath())) 0L else file.length())

    private fun rejectEmptyDirectory(file: File) =
        !(file.isDirectory && Files.list(file.toPath()).use { f -> f.findAny().isPresent })

    private fun convertToDs3Object(fileParts: Pair<File, Long>, parent: Path): Ds3Object {
        val (file, size) = fileParts
        val pathBuilder = StringBuilder(targetDir)
        val localDelim = file.toPath().fileSystem.separator
        pathBuilder.append(parent.relativize(file.toPath()).toString().replace(localDelim, "/"))
        if (file.isDirectory) { pathBuilder.append("/") }
        return Ds3Object(pathBuilder.toString(), size)
    }

    private fun rejectDeadLinks(file: File): Boolean {
        return try {
            file.toPath().toRealPath()
            true
        } catch (e: NoSuchFileException) {
            loggingService().logMessage("Could not resolve link " + file, LogType.ERROR)
            false
        }
    }

    override fun jobSize() = jobTaskElement.client.getActiveJobSpectraS3(GetActiveJobSpectraS3Request(job.jobId)).activeJobResult.originalSizeInBytes

    override fun shouldRestoreFileAttributes(): Boolean = jobTaskElement.settingsStore.filePropertiesSettings.isFilePropertiesEnabled

    override fun isCompleted() = jobTaskElement.client.getJobSpectraS3(GetJobSpectraS3Request(job.jobId)).masterObjectListResult.status == JobStatus.COMPLETED
    override fun removeJob() {
        ParseJobInterruptionMap.removeJobIdFromFile(jobTaskElement.jobInterruptionStore, job.jobId.toString(), jobTaskElement.client.connectionDetails.endpoint)
    }

    override fun saveJob(jobSize: Long) {
        ParseJobInterruptionMap.saveValuesToFiles(jobTaskElement.jobInterruptionStore, prefixMap, mapOf(), jobTaskElement.client.connectionDetails.endpoint, job.jobId, jobSize, targetPath(), jobTaskElement.dateTimeUtils, "PUT", bucket)
    }
}