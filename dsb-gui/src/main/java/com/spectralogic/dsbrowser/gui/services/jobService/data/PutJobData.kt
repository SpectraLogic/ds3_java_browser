/*
 * ***************************************************************************
 *   Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
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

import com.spectralogic.ds3client.commands.spectrads3.GetActiveJobSpectraS3Request
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.helpers.FileObjectPutter
import com.spectralogic.ds3client.models.JobStatus
import com.spectralogic.ds3client.models.Priority
import com.spectralogic.ds3client.models.bulk.Ds3Object
import com.spectralogic.ds3client.utils.Guard
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.services.jobService.JobTaskElement
import com.spectralogic.dsbrowser.gui.services.jobService.util.EmptyChannelBuilder
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap
import com.spectralogic.dsbrowser.util.GuavaCollectors
import javafx.beans.property.BooleanProperty
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

data class PutJobData(private val items: List<Pair<String, Path>>,
                      private val targetDir: String,
                      val bucket: String,
                      private val jobTaskElement: JobTaskElement) : JobData {

    override fun internationalize(labelName: String): String =  jobTaskElement.resourceBundle.getString(labelName)
    override fun modifyJob(job: Ds3ClientHelpers.Job) {
        job.withMaxParallelRequests(jobTaskElement.settingsStore.processSettings.maximumNumberOfParallelThreads)
        val putJobPriority = jobTaskElement.savedJobPrioritiesStore.jobSettings.putJobPriority
        if (Guard.isStringNullOrEmpty(putJobPriority)) {
            jobTaskElement.client.modifyJobSpectraS3(ModifyJobSpectraS3Request(job.jobId).withPriority(Priority.valueOf(putJobPriority)))
        }
    }

    override var job: Ds3ClientHelpers.Job? = null
        get() {
            if (field == null) {
                field = Ds3ClientHelpers.wrap(jobTaskElement.client).startWriteJob(bucket, buildDs3Objects())
            }
            return field!!
        }
        set(value) {
            if (value != null) { field = value }
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

    override fun getObjectChannelBuilder(prefix: String): Ds3ClientHelpers.ObjectChannelBuilder = EmptyChannelBuilder(FileObjectPutter(prefixMap.get(prefix)!!.parent), prefixMap.get(prefix)!!)

    private fun buildDs3Objects(): List<Ds3Object> = items.flatMap({ dataToDs3Objects(it) }).distinct()

    private fun dataToDs3Objects(item: Pair<String, Path>): Iterable<Ds3Object> {
        val localDelim = item.second.fileSystem.separator
        val parent = item.second.parent
        val ds3ObjectList = Files.walk(item.second)
                .filter { !(Files.isDirectory(it) && Files.list(it).findAny().isPresent) }
                .map {
                    Ds3Object(if (Files.isDirectory(it)) {
                        targetDir + parent.relativize(it).toString().replace(localDelim, "/") + "/"
                    } else {
                        targetDir + parent.relativize(it).toString().replace(localDelim, "/")
                    }
                            , if (Files.isDirectory(it)) 0L else Files.size(it))
                }.collect(GuavaCollectors.immutableList())
        ds3ObjectList.forEach({ prefixMap.put(it.name, item.second) })
        return ds3ObjectList
    }

    override fun jobSize() = jobTaskElement.client.getActiveJobSpectraS3(GetActiveJobSpectraS3Request(job!!.jobId)).activeJobResult.originalSizeInBytes

    override fun shouldRestoreFileAttributes() = jobTaskElement.settingsStore.filePropertiesSettings.isFilePropertiesEnabled

    override fun isCompleted() = jobTaskElement.client.getJobSpectraS3(GetJobSpectraS3Request(job!!.jobId)).masterObjectListResult.status == JobStatus.COMPLETED
    override fun removeJob() {
        ParseJobInterruptionMap.removeJobIdFromFile(jobTaskElement.jobInterruptionStore, job!!.jobId.toString(), jobTaskElement.client.connectionDetails.endpoint)
    }

    override fun saveJob(jobSize: Long) {
        ParseJobInterruptionMap.saveValuesToFiles(jobTaskElement.jobInterruptionStore, prefixMap, mapOf(), jobTaskElement.client.connectionDetails.endpoint, job!!.jobId, jobSize, targetPath(), jobTaskElement.dateTimeUtils, "PUT", bucket)
    }
}