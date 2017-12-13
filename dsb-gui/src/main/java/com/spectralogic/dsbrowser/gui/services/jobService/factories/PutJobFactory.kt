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

package com.spectralogic.dsbrowser.gui.services.jobService.factories

import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue
import com.spectralogic.dsbrowser.gui.services.JobWorkers
import com.spectralogic.dsbrowser.gui.services.jobService.GetJob
import com.spectralogic.dsbrowser.gui.services.jobService.JobTask
import com.spectralogic.dsbrowser.gui.services.jobService.JobTaskElement
import com.spectralogic.dsbrowser.gui.services.jobService.PutJob
import com.spectralogic.dsbrowser.gui.services.jobService.data.PutJobData
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap
import com.spectralogic.dsbrowser.util.andThen
import com.spectralogic.dsbrowser.util.exists
import javafx.application.Platform
import javafx.concurrent.WorkerStateEvent
import javafx.scene.control.TreeItem
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import java.util.*
import javax.inject.Inject

class PutJobFactory @Inject constructor(private val loggingService: LoggingService,
                                        private val jobInterruptionStore: JobInterruptionStore,
                                        private val resourceBundle: ResourceBundle,
                                        private val deepStorageBrowserPresenter: DeepStorageBrowserPresenter,
                                        private val jobWorkers: JobWorkers,
                                        private val jobTaskElementFactory: JobTaskElement.JobTaskElementFactory) {

    private companion object {
        private val LOG = LoggerFactory.getLogger(PutJobFactory::class.java)
        private const val TYPE: String = "BULK_PUT"
    }

    fun create(files: List<Pair<String, Path>>, bucket: String, targetDir: String, client: Ds3Client, refreshBehavior: () -> Unit = {}) {
        jobTaskElementFactory.create(client)
                .let { PutJobData(files, targetDir, bucket, it) }
                .let { PutJob(it) }
                .let { JobTask(it) }
                .apply {
                    setOnCancelled(onCancelled(client, TYPE, LOG, loggingService, jobInterruptionStore, deepStorageBrowserPresenter).andThen(refreshBehavior))
                    setOnFailed(onFailed(client, jobInterruptionStore, deepStorageBrowserPresenter, loggingService, LOG, TYPE).andThen(refreshBehavior))
                    setOnSucceeded(onSucceeded(TYPE, jobId, LOG).andThen(refreshBehavior))
                }
                .also { jobWorkers.execute(it) }
    }
}