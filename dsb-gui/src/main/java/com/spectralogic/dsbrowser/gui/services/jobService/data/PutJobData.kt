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
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.helpers.FileObjectPutter
import com.spectralogic.ds3client.helpers.options.WriteJobOptions
import com.spectralogic.ds3client.models.JobStatus
import com.spectralogic.ds3client.models.Priority
import com.spectralogic.ds3client.models.bulk.Ds3Object
import com.spectralogic.ds3client.utils.Guard
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
import java.nio.file.LinkOption
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.function.Supplier
import java.util.stream.Stream

data class PutJobData(
    private val items: List<Pair<String, Path>>,
    private val targetDir: String,
    override val bucket: String,
    private val jobTaskElement: JobTaskElement
) : JobData {

    override fun runningTitle(): String {
        val transferringPut = jobTaskElement.resourceBundle.getString("transferringPut")
        val jobId = job?.jobId
        val startedAt = jobTaskElement.resourceBundle.getString("startedAt")
        val started = jobTaskElement.dateTimeUtils.format(getStartTime())
        return "$transferringPut $jobId $startedAt $started"
    }

    override var jobId: UUID? = null
    public override var cancelled: Supplier<Boolean>? = null
    override fun client(): Ds3Client = jobTaskElement.client

    override public var lastFile = ""
    override fun internationalize(labelName: String): String = jobTaskElement.resourceBundle.getString(labelName)
    override fun modifyJob(job: Ds3ClientHelpers.Job) {
        job.withMaxParallelRequests(jobTaskElement.settingsStore.processSettings.maximumNumberOfParallelThreads)
    }

    override var job: Ds3ClientHelpers.Job? = null
        get() {
            if (field == null) {
                val objects = buildDs3Objects()
                if (objects.isEmpty()) {
                    loggingService().logMessage("File list is empty, cannot create job", LogType.ERROR)
                    throw RuntimeException("File list is empty")
                }
                field = if (writeJobOptions() == null) {
                    Ds3ClientHelpers.wrap(jobTaskElement.client).startWriteJob(bucket, objects)
                } else {
                    Ds3ClientHelpers.wrap(jobTaskElement.client).startWriteJob(bucket, objects, writeJobOptions())
                }

            }
            jobId = field?.jobId
            return field!!
        }
        set(value) {
            if (value != null) {
                field = value
            }
        }
    override var prefixMap: MutableMap<String, Path> = mutableMapOf()

    override fun showCachedJobProperty(): BooleanProperty =
        jobTaskElement.settingsStore.showCachedJobSettings.showCachedJobEnableProperty()

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

    private fun buildDs3Objects(): List<Ds3Object> = items.flatMap({ dataToDs3Objects(it) }).distinct()

    private fun dataToDs3Objects(item: Pair<String, Path>): Iterable<Ds3Object> {
        val localDelim = item.second.fileSystem.separator
        val parent = item.second.parent
        val paths = Files.walk(item.second).use { stream: Stream<Path> ->
            stream.iterator().asSequence()
                .takeWhile { !cancelled!!.get() }
                .filter { !(Files.isDirectory(it) && Files.list(it).use { f -> f.findAny().isPresent }) }
                .filter {
                    if (!Files.isSymbolicLink(it)) {
                        true
                    } else {
                        try {
                            it.toRealPath()
                            true
                        } catch (e: java.nio.file.NoSuchFileException) {
                            loggingService().logMessage("Could not resolve link " + it, LogType.ERROR)
                            false
                        }
                    }
                }
                .map {
                    Ds3Object(
                        if (Files.isDirectory(it)) {
                            targetDir + parent.relativize(it).toString().replace(localDelim, "/") + "/"
                        } else {
                            targetDir + parent.relativize(it).toString().replace(localDelim, "/")
                        }
                        , if (Files.isDirectory(it)) 0L else Files.size(it)
                    )
                }
                .toList()
        }
        paths.forEach { prefixMap.put(it.name, item.second) }
        return paths
    }

    override fun jobSize() =
        jobTaskElement.client.getActiveJobSpectraS3(GetActiveJobSpectraS3Request(job!!.jobId)).activeJobResult.originalSizeInBytes

    override fun shouldRestoreFileAttributes() =
        jobTaskElement.settingsStore.filePropertiesSettings.isFilePropertiesEnabled

    override fun isCompleted() =
        jobTaskElement.client.getJobSpectraS3(GetJobSpectraS3Request(job!!.jobId)).masterObjectListResult.status == JobStatus.COMPLETED

    override fun removeJob() {
        ParseJobInterruptionMap.removeJobIdFromFile(
            jobTaskElement.jobInterruptionStore,
            job!!.jobId.toString(),
            jobTaskElement.client.connectionDetails.endpoint
        )
    }

    override fun saveJob(jobSize: Long) {
        ParseJobInterruptionMap.saveValuesToFiles(
            jobTaskElement.jobInterruptionStore,
            prefixMap,
            mapOf(),
            jobTaskElement.client.connectionDetails.endpoint,
            job!!.jobId,
            jobSize,
            targetPath(),
            jobTaskElement.dateTimeUtils,
            "PUT",
            bucket
        )
    }

    private fun writeJobOptions(): WriteJobOptions? {
        val writeJobOptions = WriteJobOptions.create()
        val priority = jobTaskElement.savedJobPrioritiesStore.jobSettings.putJobPriority
        return if (Guard.isStringNullOrEmpty(priority) || priority.equals("Data Policy Default (no change)")) {
            null
        } else {
            writeJobOptions.withPriority(Priority.valueOf(priority))
        }
    }
}