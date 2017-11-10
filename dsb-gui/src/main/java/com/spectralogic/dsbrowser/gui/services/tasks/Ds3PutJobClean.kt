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
import com.spectralogic.ds3client.helpers.ObjectCompletedListener
import com.spectralogic.ds3client.helpers.WaitingForChunksListener
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue
import com.spectralogic.dsbrowser.gui.services.ds3Panel.Ds3PanelService
import com.spectralogic.dsbrowser.gui.services.jobService.PutJob
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.PathUtil
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.scene.control.TreeItem
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.*
import javax.inject.Inject


class Ds3PutJobClean @Inject constructor(@Assisted private val putJob: PutJob,
                                         @Assisted private val treeItem: TreeItem<Ds3TreeTableValue>?,
                                         private val settingsStore: SettingsStore) : Ds3JobTask() {

    public val isVisible: BooleanProperty = SimpleBooleanProperty(true)

    override fun executeJob() {
        updateTitle("")
        putJob.setWaitingForChunkListener(WaitingForChunksListener { i: Int ->
            try {
                loggingService.logMessage("Waiting for chunks, will try again in " + DateTimeUtils.timeConversion(i.toLong()), LogType.INFO)
                Thread.sleep((1000 * i).toLong())
            } catch (e: InterruptedException) {
                loggingService.logMessage("Did not receive chunks before timeout", LogType.ERROR)
            }
        })

        putJob.setDataTransferListener(DataTransferredListener { l: Long ->
            updateProgress(putJob.totalSent().getAndAdd(l), putJob.totalJobSize())
        })

        putJob.setObjectCompletedListener(ObjectCompletedListener { s: String? ->
            getTransferRates(Instant.now(), putJob.totalSent(), putJob.totalJobSize(), s, putJob.fileTreePath().toString())
            Ds3PanelService.throttledRefresh(treeItem)
        })

        putJob.runTransfer(Ds3ClientHelpers.ObjectChannelBuilder { s: String? ->
            FileChannel.open(PathUtil.resolveForSymbolic(putJob.fileMapper().get(s)), StandardOpenOption.READ)
        })

        isVisible.bind(settingsStore.showCachedJobSettings.showCachedJobEnableProperty())

        while (!putJob.waitForStorage().isDisposed) {
            Thread.sleep(1000)
        }
    }

    override fun getJobId(): UUID = putJob.uuid()

    public interface Ds3PutJobFactory {
        fun create(putJob: PutJob, treeItem: TreeItem<Ds3TreeTableValue>?)
    }
}