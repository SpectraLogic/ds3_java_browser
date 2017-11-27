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

package com.spectralogic.dsbrowser.gui.services.jobService

import com.google.common.collect.ImmutableMap
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.helpers.FileObjectPutter
import com.spectralogic.ds3client.models.bulk.Ds3Object
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.util.GuavaCollectors
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.stream.Collectors

data class PutJobData(private val items: List<Pair<String, Path>>, public val remotePath: String, val settingsStore: SettingsStore, val loggingService: LoggingService, val dateTimeUtils: DateTimeUtils, val targetDir: String) {
    private val prefixMap: HashMap<String, Path> = HashMap()
    private var startTime: Instant = Instant.now()

    public fun getStartTime(): Instant = startTime
    public fun setStartTime(): Instant {
        startTime = Instant.now()
        return startTime
    }

    fun getObjectChannelBuilder(s: String?): FileObjectPutter = FileObjectPutter(prefixMap().get(s)!!.parent)

    fun prefixMap(): ImmutableMap<String, Path> {
        return ImmutableMap.copyOf(prefixMap)
    }


    public fun buildDs3Objects(): List<Ds3Object> = items.flatMap({ dataToDs3Objects(it) }).distinct()

    private fun dataToDs3Objects(item: Pair<String, Path>): Iterable<Ds3Object> {
        val parent = item.second.parent
        var x = Files.walk(item.second)
                .filter { !(Files.isDirectory(it) && Files.list(it).findAny().isPresent) }
                .map { Ds3Object(targetDir + parent.relativize(it).toString(), if (Files.isDirectory(it)) 0L else Files.size(it)) }.collect(GuavaCollectors.immutableList())
        x.forEach({prefixMap.put(it.name, item.second)})
        return x
    }

    fun hasMetadata() = settingsStore.filePropertiesSettings.isFilePropertiesEnabled
}