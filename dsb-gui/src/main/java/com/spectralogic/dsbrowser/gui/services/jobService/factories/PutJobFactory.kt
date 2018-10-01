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
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter
import com.spectralogic.dsbrowser.gui.services.JobWorkers
import com.spectralogic.dsbrowser.gui.services.Workers
import com.spectralogic.dsbrowser.gui.services.jobService.JobTask
import com.spectralogic.dsbrowser.gui.services.jobService.JobTaskElement
import com.spectralogic.dsbrowser.gui.services.jobService.PutJob
import com.spectralogic.dsbrowser.gui.services.jobService.data.PutJobData
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler
import com.spectralogic.dsbrowser.util.andThen
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PutJobFactory @Inject constructor(private val loggingService: LoggingService,
                                        private val jobInterruptionStore: JobInterruptionStore,
                                        private val deepStorageBrowserPresenter: DeepStorageBrowserPresenter,
                                        private val jobWorkers: JobWorkers,
                                        private val workers: Workers,
                                        private val jobTaskElementFactory: JobTaskElement.JobTaskElementFactory) {
    private companion object {
        private val LOG = LoggerFactory.getLogger(PutJobFactory::class.java)
        private const val TYPE: String = "Put"
    }

    fun create(session: Session, files: List<Pair<String, Path>>, bucket: String, targetDir: String, client: Ds3Client, refreshBehavior: () -> Unit = {}) {
        jobTaskElementFactory.create(client)
                .let { PutJobData(files, targetDir, bucket, it) }
                .let { PutJob(it) }
                .let { JobTask(it, session.sessionName) }
                .apply {
                    onSucceeded = SafeHandler.logHandle(onSucceeded(TYPE, LOG).andThen(refreshBehavior))
                    onFailed = SafeHandler.logHandle(onFailed(client, jobInterruptionStore, deepStorageBrowserPresenter, loggingService, workers, TYPE).andThen(refreshBehavior))
                    onCancelled = SafeHandler.logHandle(onCancelled(client, loggingService, jobInterruptionStore, deepStorageBrowserPresenter).andThen(refreshBehavior))
                }
                .also { jobWorkers.execute(it) }
    }
}