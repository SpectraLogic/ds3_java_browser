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
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.helpers.FileObjectPutter
import com.spectralogic.ds3client.models.JobStatus
import com.spectralogic.ds3client.models.bulk.Ds3Object
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.services.jobService.JobTaskElement
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
                      private val jte: JobTaskElement) : JobData {
    override var job: Ds3ClientHelpers.Job? = null
        get() {
            if (field == null) {
                field = Ds3ClientHelpers.wrap(jte.client).startWriteJob(bucket, buildDs3Objects())
            }
            return field!!
        }
        set(value) {
            if (value != null) field = value
        }
    override var prefixMap: MutableMap<String, Path> = mutableMapOf()

    override fun showCachedJobProperty(): BooleanProperty = jte.settingsStore.showCachedJobSettings.showCachedJobEnableProperty()
    override fun loggingService(): LoggingService = jte.loggingService
    override fun targetPath(): String = targetDir
    override fun dateTimeUtils(): DateTimeUtils = jte.dateTimeUtils
    private var startTime: Instant = Instant.now()
    public override fun getStartTime(): Instant = startTime
    public override fun setStartTime(): Instant {
        startTime = Instant.now()
        return startTime
    }

    override fun getObjectChannelBuilder(prefix: String?): FileObjectPutter = FileObjectPutter(prefixMap.get(prefix)!!.parent)


    public fun buildDs3Objects(): List<Ds3Object> = items.flatMap({ dataToDs3Objects(it) }).distinct()

    private fun dataToDs3Objects(item: Pair<String, Path>): Iterable<Ds3Object> {
        val parent = item.second.parent
        var x = Files.walk(item.second)
                .filter { !(Files.isDirectory(it) && Files.list(it).findAny().isPresent) }
                .map { Ds3Object(targetDir + parent.relativize(it).toString(), if (Files.isDirectory(it)) 0L else Files.size(it)) }.collect(GuavaCollectors.immutableList())
        x.forEach({ prefixMap.put(it.name, item.second) })
        return x
    }

    override fun jobSize() = jte.client.getActiveJobSpectraS3(GetActiveJobSpectraS3Request(job!!.jobId)).activeJobResult.originalSizeInBytes

    override fun shouldRestoreFileAttributes() = jte.settingsStore.filePropertiesSettings.isFilePropertiesEnabled

    override fun isCompleted() = jte.client.getJobSpectraS3(GetJobSpectraS3Request(job!!.jobId)).masterObjectListResult.status == JobStatus.COMPLETED
    override fun removeJob() {
        ParseJobInterruptionMap.removeJobIdFromFile(jte.jobInterruptionStore, job!!.jobId.toString(), jte.client.connectionDetails.endpoint)
    }

    override fun saveJob(jobSize: Long) {
        ParseJobInterruptionMap.saveValuesToFiles(jte.jobInterruptionStore, prefixMap, mapOf(), jte.client.connectionDetails.endpoint, job!!.jobId, jobSize, targetPath(), jte.dateTimeUtils, "PUT", bucket)
    }
}