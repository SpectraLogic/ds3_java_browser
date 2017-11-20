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

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.helpers.FileObjectGetter
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder
import com.spectralogic.ds3client.models.bulk.Ds3Object
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.services.jobService.Util.OverWritingObjectChannelBuilder
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore
import com.spectralogic.dsbrowser.gui.util.BaseTreeModel
import com.spectralogic.dsbrowser.gui.util.BaseTreeModel.Type.*
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import java.nio.file.Path
import java.time.Instant
import java.util.*

data class GetJobData(val localPath: Path,
                      val client: Ds3Client,
                      private val doubles: List<Pair<String, BaseTreeModel.Type>>,
                      val bucket: String,
                      val remotePrefix: String,
                      val loggingService: LoggingService,
                      val resourceBundle: ResourceBundle,
                      private val settingStore: SettingsStore,
                      val dateTimeUtils: DateTimeUtils) {

    private var startTime = Instant.now()
    public fun getStartTime() : Instant = startTime
    public fun setStartTime() : Instant {
        startTime = Instant.now()
        return startTime
    }

    fun getObjectChannelBuilder(): Ds3ClientHelpers.ObjectChannelBuilder {
        val ocb: Ds3ClientHelpers.ObjectChannelBuilder = OverWritingObjectChannelBuilder(FileObjectGetter(localPath), localPath, loggingService, resourceBundle)
        return if (remotePrefix.isEmpty()) {
            ocb
        } else {
            PrefixRemoverObjectChannelBuilder(ocb, remotePrefix)
        }
    }

    fun getJob(): Ds3ClientHelpers.Job = Ds3ClientHelpers.wrap(client).startReadJob(bucket, buildDs3Objects())

    private fun buildDs3Objects(): List<Ds3Object> = doubles.flatMap({ dataToDs3Objects(it) })

    private fun dataToDs3Objects(t: Pair<String, BaseTreeModel.Type>): Iterable<Ds3Object> = when(t.second) {
        Directory -> { folderToObjects(t) }
        File -> { ImmutableList.of(Ds3Object(t.first))}
        else -> { Collections.emptyList()}
    }

    private fun folderToObjects(t: Pair<String, BaseTreeModel.Type>) : Iterable<Ds3Object> {
        var folderObjects = Ds3ClientHelpers.wrap(client).listObjects(bucket, t.first).map { contents -> Ds3Object(contents.key) }
        return if (folderObjects.isEmpty()) {
           ImmutableList.of(Ds3Object(t.first))
        } else {
            folderObjects
        }
    }

    fun hasMetadata() = settingStore.filePropertiesSettings.isFilePropertiesEnabled

}