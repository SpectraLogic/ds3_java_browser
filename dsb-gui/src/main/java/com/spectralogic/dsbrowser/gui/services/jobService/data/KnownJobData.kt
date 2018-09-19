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

import com.google.common.collect.ImmutableMap
import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap
import java.nio.file.Path
import java.util.*
import javax.naming.ConfigurationException

private const val RETRY_TIME = 100
class KnownJobData constructor(
        private val jobData: JobData,
        private val filesAndFolderMap: FilesAndFolderMap,
        override val jobId: UUID,
        val client: Ds3Client,
        private val jobType: String
) : JobData by jobData {

    override val job: Ds3ClientHelpers.Job by lazy {
                if (jobType == "GET") {
                    Ds3ClientHelpers.wrap(client, RETRY_TIME).recoverReadJob(jobId)
                } else if (jobType == "PUT") {
                    Ds3ClientHelpers.wrap(client, RETRY_TIME).recoverWriteJob(jobId)
                } else {
                    throw ConfigurationException("Was expecting GET or PUT, but got" + jobType)
                }
            }

    override var prefixMap: MutableMap<String, Path> = mutableMapOf()
        get() {
            val builder: ImmutableMap.Builder<String, Path> = ImmutableMap.builder()
            builder.putAll(filesAndFolderMap.files)
            builder.putAll(filesAndFolderMap.folders)
            jobData.prefixMap = builder.build()
            return jobData.prefixMap
        }
}