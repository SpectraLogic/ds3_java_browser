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

import com.google.common.collect.ImmutableList
import com.spectralogic.ds3client.commands.spectrads3.GetActiveJobSpectraS3Request
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.helpers.FileObjectGetter
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder
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

data class GetJobData(private val list: List<Pair<String, String>>,
                      private val localPath: Path,
                      val bucket: String,
                      private val jte: JobTaskElement) : JobData {

    override var job: Ds3ClientHelpers.Job? = null
        get() {
            if (field == null) {
                field = Ds3ClientHelpers.wrap(jte.client).startReadJob(bucket, buildDs3Objects())
            }
            return field!!
        }
        set(value) {
            if (value != null) field = value
        }

    override fun showCachedJobProperty(): SimpleBooleanProperty = SimpleBooleanProperty(true)
    override fun loggingService(): LoggingService = jte.loggingService
    override fun targetPath(): String = localPath.toString()
    override fun dateTimeUtils(): DateTimeUtils = jte.dateTimeUtils
    private var startTime = Instant.now()
    private val last: String? = null
    override var prefixMap: MutableMap<String, Path> = mutableMapOf()
        get() {
            if (field.isEmpty()) {
                list.forEach({ field.put(it.first, Paths.get(it.second)) })
            }
            return field
        }

    override public fun getStartTime(): Instant = startTime
    override public fun setStartTime(): Instant {
        startTime = Instant.now()
        return startTime
    }

    override fun getObjectChannelBuilder(prefix: String): Ds3ClientHelpers.ObjectChannelBuilder {
        val ocb: Ds3ClientHelpers.ObjectChannelBuilder = DelimChannelBuilder(DirectoryChannelBuilder(FileObjectGetter(localPath), localPath), localPath)
        return if (Guard.isStringNullOrEmpty(prefix)) {
            ocb
        } else {
            PrefixRemoverObjectChannelBuilder(ocb, prefix)
        }
    }

    private fun buildDs3Objects(): List<Ds3Object> = list.flatMap({ dataToDs3Objects(it) }).distinct()

    private fun dataToDs3Objects(t: Pair<String, String>): Iterable<Ds3Object> = when (t.first.last()) {
        '/' -> {
            folderToObjects(t)
        }
        else -> {
            checkifOverWriting(t.first, t.second)
            ImmutableList.of(Ds3Object(t.first))
        }
    }

    private fun folderToObjects(t: Pair<String, String>): Iterable<Ds3Object> {
        var list: ImmutableList<Ds3Object> = Ds3ClientHelpers.wrap(jte.client).listObjects(bucket, t.first)
                .map { contents -> Ds3Object(contents.key) }
                .stream()
                .collect(GuavaCollectors.immutableList())
        list.forEach {
            prefixMap.put(it.name, Paths.get(t.second))
            checkifOverWriting(it.name, t.second)
        }
        return list
    }

    override fun shouldRestoreFileAttributes() = jte.settingsStore.filePropertiesSettings.isFilePropertiesEnabled
    override fun jobSize(): Long {
        return jte.client.getActiveJobSpectraS3(GetActiveJobSpectraS3Request(job!!.jobId)).activeJobResult.originalSizeInBytes
    }

    override fun isCompleted(): Boolean = true
    override fun removeJob() {
        ParseJobInterruptionMap.removeJobIdFromFile(jte.jobInterruptionStore, job!!.jobId.toString(), jte.client.connectionDetails.endpoint)
    }

    override fun saveJob(jobSize: Long) {
        ParseJobInterruptionMap.saveValuesToFiles(jte.jobInterruptionStore, prefixMap, mapOf(), jte.client.connectionDetails.endpoint, job!!.jobId, jobSize, targetPath(), jte.dateTimeUtils, "GET", bucket)
    }

    private fun checkifOverWriting(name: String, path: String) {
        val filePath = Paths.get(targetPath(), name.removePrefix(path))
        if(Files.exists(filePath)) {
            loggingService().logMessage("Overwriting file ${filePath.toString()}", LogType.INFO)
        }
    }

}
