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

package com.spectralogic.dsbrowser.gui.services.jobService

import com.google.inject.assistedinject.Assisted
import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import java.util.ResourceBundle
import javax.inject.Inject

data class JobTaskElement @Inject constructor(
    val settingsStore: SettingsStore,
    val loggingService: LoggingService,
    val dateTimeUtils: DateTimeUtils,
    @Assisted val client: Ds3Client,
    val jobInterruptionStore: JobInterruptionStore,
    val savedJobPrioritiesStore: SavedJobPrioritiesStore,
    val resourceBundle: ResourceBundle
) {
    interface JobTaskElementFactory {
        fun create(client: Ds3Client): JobTaskElement
    }
}
