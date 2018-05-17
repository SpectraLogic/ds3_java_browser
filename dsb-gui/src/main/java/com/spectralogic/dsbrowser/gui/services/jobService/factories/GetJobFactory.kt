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

package com.spectralogic.dsbrowser.gui.services.jobService.factories

import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter
import com.spectralogic.dsbrowser.gui.services.JobWorkers
import com.spectralogic.dsbrowser.gui.services.Workers
import com.spectralogic.dsbrowser.gui.services.jobService.GetJob
import com.spectralogic.dsbrowser.gui.services.jobService.JobTask
import com.spectralogic.dsbrowser.gui.services.jobService.JobTaskElement
import com.spectralogic.dsbrowser.gui.services.jobService.data.GetJobData
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler
import com.spectralogic.dsbrowser.util.andThen
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*
import javax.inject.Inject

class GetJobFactory @Inject constructor(private val loggingService: LoggingService,
                                        private val jobInterruptionStore: JobInterruptionStore,
                                        private val deepStorageBrowserPresenter: DeepStorageBrowserPresenter,
                                        private val jobWorkers: JobWorkers,
                                        private val workers: Workers,
                                        private val jobTaskElementFactory: JobTaskElement.JobTaskElementFactory) {
    private companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GetJobFactory::class.java)
        private const val TYPE = "Get"
    }

    public fun create(files: List<Pair<String, String>>, bucket: String, targetDir: Path, client: Ds3Client, refreshBehavior: () -> Unit) {
        jobTaskElementFactory.create(client)
                .let { GetJobData(files, targetDir, bucket, it) }
                .let { GetJob(it) }
                .let { JobTask(it) }
                .apply {
                    onSucceeded = SafeHandler.logHandle(onSucceeded(TYPE, LOG).andThen(refreshBehavior))
                    onFailed = SafeHandler.logHandle(onFailed(client, jobInterruptionStore, deepStorageBrowserPresenter, loggingService, LOG, workers, TYPE).andThen(refreshBehavior))
                    onCancelled = SafeHandler.logHandle(onCancelled(client, TYPE, LOG, loggingService, jobInterruptionStore, workers, deepStorageBrowserPresenter).andThen(refreshBehavior))
                }
                .also { jobWorkers.execute(it) }
    }
}