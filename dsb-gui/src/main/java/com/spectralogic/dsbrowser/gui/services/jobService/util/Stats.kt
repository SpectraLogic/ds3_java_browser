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

package com.spectralogic.dsbrowser.gui.services.jobService.util

import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.StringBuilderUtil
import javafx.beans.property.LongProperty
import javafx.beans.property.StringProperty
import java.time.Instant

class Stats {
    fun updateStatistics(name: String,
                         startTime: Instant,
                         sent: LongProperty,
                         total: LongProperty,
                         message: StringProperty,
                         loggingService: LoggingService,
                         toPath: String,
                         dateTimeUtils: DateTimeUtils,
                         location: String,
                         finished: Boolean) {
        val elapsedSeconds = Instant.now().epochSecond - startTime.epochSecond
        val transferRate = estimateTransferRate(sent, elapsedSeconds)
        val timeRemaining: Float = estimateTimeRemaning(transferRate, total)
        message.set(StringBuilderUtil.getTransferRateString(transferRate.toLong(), timeRemaining.toLong(), (sent.get()),
                total.longValue(), name, location).toString())
        if (finished) {loggingService.logMessage(StringBuilderUtil.objectSuccessfullyTransferredString(name, toPath, dateTimeUtils.nowAsString(), location).toString(), LogType.SUCCESS)}
    }

    private fun estimateTransferRate(sent: LongProperty, elapsedSeconds: Long) =
            sent.get() / elapsedSeconds.toFloat()

    private fun estimateTimeRemaning(transferRate: Float, total: LongProperty): Float {
        return if (transferRate != 0F) {
            total.get().toFloat() / transferRate
        } else {
            0F
        }
    }
}