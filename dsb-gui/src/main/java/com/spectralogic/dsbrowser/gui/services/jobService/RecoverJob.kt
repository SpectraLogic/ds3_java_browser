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

import com.google.common.collect.ImmutableList
import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo
import com.spectralogic.dsbrowser.gui.services.jobService.data.GetJobData
import com.spectralogic.dsbrowser.gui.services.jobService.data.KnownJobData
import com.spectralogic.dsbrowser.gui.services.jobService.data.PutJobData
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

class RecoverJob(private val jobID: UUID, private val endpoint: EndpointInfo, private val jobTaskElement: JobTaskElement, val client: Ds3Client) {

    fun getTask(): JobService {
        val filesAndFolderMap = endpoint.jobIdAndFilesFoldersMap.get(jobID.toString())
        if (filesAndFolderMap != null) {
            return when (filesAndFolderMap.type) {
                "GET" -> GetJob(KnownJobData(GetJobData(emptyList(), Paths.get(filesAndFolderMap.targetLocation), filesAndFolderMap.bucket, jobTaskElement), filesAndFolderMap, jobID, client, filesAndFolderMap.type))
                "PUT" -> PutJob(KnownJobData(PutJobData(filesAndFolderMap.toPairs(), filesAndFolderMap.targetLocation, filesAndFolderMap.bucket, jobTaskElement), filesAndFolderMap, jobID, client, filesAndFolderMap.type))
                else -> throw RuntimeException("Unknown Job type ${filesAndFolderMap.type} was encountered")
            }
        }
        throw Exception("Could not recover interrupted jobs from disk")
    }

    private fun FilesAndFolderMap.toPairs(): List<Pair<String, Path>> {
        val builder: ImmutableList.Builder<Pair<String, Path>> = ImmutableList.builder()
        builder.addAll(this.files.entries.map { Pair(it.key, it.value) })
        builder.addAll(this.folders.entries.map { Pair(it.key, it.value) })
        return builder.build()
    }
}