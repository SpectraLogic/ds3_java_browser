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

import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import javafx.beans.property.BooleanProperty
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.function.Supplier

interface JobData {
    fun getStartTime(): Instant
    fun setStartTime(): Instant
    fun getObjectChannelBuilder(prefix: String): Ds3ClientHelpers.ObjectChannelBuilder
    fun shouldRestoreFileAttributes(): Boolean
    fun isCompleted(): Boolean
    fun jobSize(): Long
    fun loggingService(): LoggingService
    fun targetPath(): String
    fun dateTimeUtils(): DateTimeUtils
    fun showCachedJobProperty(): BooleanProperty
    fun saveJob(jobSize: Long)
    fun removeJob()
    fun modifyJob(job :Ds3ClientHelpers.Job)
    fun internationalize(labelName: String): String
    fun client(): Ds3Client
    fun runningTitle(): String
    val job: Ds3ClientHelpers.Job
    val bucket: String
    var prefixMap: MutableMap<String, Path>
    public val jobId: UUID
}