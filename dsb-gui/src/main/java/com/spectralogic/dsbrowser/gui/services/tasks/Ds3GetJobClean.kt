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
package com.spectralogic.dsbrowser.gui.services.tasks

import com.google.inject.assistedinject.Assisted
import com.spectralogic.ds3client.helpers.DataTransferredListener
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.helpers.FileObjectGetter
import com.spectralogic.ds3client.helpers.ObjectCompletedListener
import com.spectralogic.ds3client.helpers.WaitingForChunksListener
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.services.jobService.GetJob
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import java.time.Instant
import java.util.*
import javax.inject.Inject

class Ds3GetJobClean @Inject constructor(@Assisted private val getJob: GetJob,
                                         private val resourceBundle: ResourceBundle,
                                         private val dateTimeUtils: DateTimeUtils) : Ds3JobTask() {
    override fun executeJob() {
        updateMessage("Transferring " + getJob.totalJobSize() + " to " + getJob.fileTreePath())
        updateTitle("Get job initiated to " + getJob.endpoint() + " at " + dateTimeUtils.nowAsString())

        getJob.setDataTransferredListener(DataTransferredListener { l: Long ->
            updateProgress(getJob.totalSent().getAndAdd(l), getJob.totalJobSize())
        })

        getJob.setObjectCompleteListener(ObjectCompletedListener { s: String ->
            getTransferRates(Instant.now(), getJob.totalSent(), getJob.totalJobSize(), s, getJob.fileTreePath().toString())
        })

        getJob.setWaitingForChunkListener(WaitingForChunksListener { i: Int ->
            try {
                loggingService.logMessage("Waiting for chunks, will try again in " + DateTimeUtils.timeConversion(i.toLong()), LogType.INFO)
                Thread.sleep((1000 * i).toLong())
            } catch (e : InterruptedException) {
                loggingService.logMessage("Did not receive chunks before timeout", LogType.ERROR)
            }
        })

        getJob.runTransfer(Ds3ClientHelpers.ObjectChannelBuilder { s: String? ->
            if (getJob.prefix().isEmpty()) {
                FileObjectGetter(getJob.fileTreePath()).buildChannel(s)
            } else {
                PrefixRemoverObjectChannelBuilder(FileObjectGetter(getJob.fileTreePath()), getJob.prefix()).buildChannel(s)
            }
        })
    }

    override fun getJobId(): UUID = getJob.uuid()

    public interface Ds3GetJobFactory {
        fun create(getJob: GetJob): Ds3GetJob
    }
}