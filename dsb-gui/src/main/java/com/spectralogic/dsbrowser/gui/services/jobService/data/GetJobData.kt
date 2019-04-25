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

import com.google.common.collect.ImmutableList
import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.ds3client.commands.spectrads3.GetActiveJobSpectraS3Request
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.helpers.FileObjectGetter
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder
import com.spectralogic.ds3client.helpers.options.ReadJobOptions
import com.spectralogic.ds3client.models.Priority
import com.spectralogic.ds3client.models.bulk.Ds3Object
import com.spectralogic.ds3client.utils.Guard
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.services.jobService.JobTaskElement
import com.spectralogic.dsbrowser.gui.services.jobService.util.DelimChannelBuilder
import com.spectralogic.dsbrowser.gui.services.jobService.util.DirectoryChannelBuilder
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap
import com.spectralogic.dsbrowser.util.GuavaCollectors
import javafx.beans.property.SimpleBooleanProperty
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.*

data class GetJobData(private val list: List<Pair<String, String>>,
                      private val localPath: Path,
                      override val bucket: String,
                      private val jobTaskElement: JobTaskElement,
                      private val versionId: String? = null) : JobData {

    override fun runningTitle(): String {
        val transferringGet = jobTaskElement.resourceBundle.getString("transferringGet")
        val jobId = job.jobId
        val startedAt = jobTaskElement.resourceBundle.getString("startedAt")
        val started = jobTaskElement.dateTimeUtils.format(getStartTime())
        return "$transferringGet $jobId $startedAt $started"
    }

    override val jobId: UUID by lazy { job.jobId }
    override fun client(): Ds3Client = jobTaskElement.client
    override fun internationalize(labelName: String): String = jobTaskElement.resourceBundle.getString(labelName)

    override val job: Ds3ClientHelpers.Job by lazy {
                if (readJobOptions() == null) {
                    Ds3ClientHelpers.wrap(jobTaskElement.client).startReadJob(bucket, buildDs3Objects())
                } else {
                    Ds3ClientHelpers.wrap(jobTaskElement.client).startReadJob(bucket, buildDs3Objects(), readJobOptions())
                }
            }

    override fun showCachedJobProperty(): SimpleBooleanProperty = SimpleBooleanProperty(true)
    override fun loggingService(): LoggingService = jobTaskElement.loggingService
    override fun targetPath(): String = localPath.toString()
    override fun dateTimeUtils(): DateTimeUtils = jobTaskElement.dateTimeUtils
    private var startTime = Instant.now()
    override var prefixMap: MutableMap<String, Path> = mutableMapOf()
        get() {
            if (field.isEmpty()) {
                list.forEach {
                    field[it.first] = Paths.get(it.second)
                }
            }
            return field
        }

    override fun getStartTime(): Instant = startTime
    override fun setStartTime(): Instant {
        startTime = Instant.now()
        return startTime
    }

    override fun getObjectChannelBuilder(prefix: String): Ds3ClientHelpers.ObjectChannelBuilder {
        val objectChannelBuilder: Ds3ClientHelpers.ObjectChannelBuilder = DelimChannelBuilder(DirectoryChannelBuilder(FileObjectGetter(localPath), localPath), localPath)
        return if (Guard.isStringNullOrEmpty(prefix)) {
            objectChannelBuilder
        } else {
            PrefixRemoverObjectChannelBuilder(objectChannelBuilder, prefix.replace(localPath.fileSystem.separator, "/"))
        }
    }

    private fun buildDs3Objects(): List<Ds3Object> = list.flatMap { dataToDs3Objects(it) }.distinct()

    private fun dataToDs3Objects(filePair: Pair<String, String>): Iterable<Ds3Object> = when (filePair.first.last()) {
        '/' -> {
            folderToObjects(filePair)
        }
        else -> {
            checkifOverWriting(filePair.first, filePair.second)
            if (versionId == null) {
                ImmutableList.of(Ds3Object(filePair.first))
            } else {
                ImmutableList.of(Ds3Object(filePair.first, versionId))
            }
        }
    }

    private fun folderToObjects(t: Pair<String, String>): Iterable<Ds3Object> {
        val list: ImmutableList<Ds3Object> = Ds3ClientHelpers.wrap(jobTaskElement.client).listObjects(bucket, t.first)
                .map { contents -> Ds3Object(contents.key) }
                .stream()
                .collect(GuavaCollectors.immutableList())
        list.forEach {
            prefixMap[it.name] = Paths.get(t.second)
            checkifOverWriting(it.name, t.second)
        }
        return list
    }

    override fun shouldRestoreFileAttributes(): Boolean = jobTaskElement.settingsStore.filePropertiesSettings.isFilePropertiesEnabled
    override fun jobSize(): Long {
        return jobTaskElement.client.getActiveJobSpectraS3(GetActiveJobSpectraS3Request(job.jobId)).activeJobResult.originalSizeInBytes
    }

    override fun isCompleted(): Boolean = true
    override fun removeJob() {
        ParseJobInterruptionMap.removeJobIdFromFile(jobTaskElement.jobInterruptionStore, job.jobId.toString(), jobTaskElement.client.connectionDetails.endpoint)
    }

    override fun saveJob(jobSize: Long) {
        ParseJobInterruptionMap.saveValuesToFiles(jobTaskElement.jobInterruptionStore, prefixMap, mapOf(), jobTaskElement.client.connectionDetails.endpoint, job.jobId, jobSize, targetPath(), jobTaskElement.dateTimeUtils, "GET", bucket)
    }

    private fun checkifOverWriting(name: String, path: String) {
        val filePath = Paths.get(targetPath(), name.removePrefix(path))
        if (Files.exists(filePath)) {
            loggingService().logMessage("Overwriting file $filePath", LogType.INFO)
        }
    }

    override fun modifyJob(job: Ds3ClientHelpers.Job) {
        job.withMaxParallelRequests(jobTaskElement.settingsStore.processSettings.maximumNumberOfParallelThreads)
    }

    private fun readJobOptions(): ReadJobOptions? {
        val readJobOptions = ReadJobOptions.create()
        val priority = jobTaskElement.savedJobPrioritiesStore.jobSettings.getJobPriority
        return if (Guard.isStringNullOrEmpty(priority) || priority.equals("Data Policy Default (no change)")) {
            null
        } else {
            readJobOptions.withPriority(Priority.valueOf(priority))
        }
    }
}
