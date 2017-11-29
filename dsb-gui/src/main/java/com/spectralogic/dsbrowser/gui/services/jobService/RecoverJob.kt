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

package com.spectralogic.dsbrowser.gui.services.jobService

import com.google.common.collect.ImmutableList
import com.spectralogic.ds3client.Ds3Client
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo
import com.spectralogic.dsbrowser.gui.services.jobService.data.GetJobData
import com.spectralogic.dsbrowser.gui.services.jobService.data.PutJobData
import com.spectralogic.dsbrowser.gui.services.jobService.data.KnownJob
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class RecoverJob(private val jobID: UUID, private val endpoint: EndpointInfo, private val jte: JobTaskElement, val client: Ds3Client) {

    fun getTask(): JobService {
        val ffm: FilesAndFolderMap? = endpoint.jobIdAndFilesFoldersMap.get(jobID.toString())
        if(ffm !=null) {
            return when (ffm.type) {
                "GET" -> GetJob(KnownJob(GetJobData(listOf(), Paths.get(ffm.targetLocation), ffm.bucket, jte), ffm, jobID, client, ffm.type))
                "PUT" -> PutJob(KnownJob(PutJobData(ffm.toPairs(), ffm.targetLocation, ffm.bucket, jte), ffm, jobID, client, ffm.type))
                else -> throw Exception("")
            }
        }
        throw Exception("")
    }

    private fun FilesAndFolderMap.toPairs(): List<Pair<String, Path>> {
        val builder: ImmutableList.Builder<Pair<String,Path>> = ImmutableList.builder()
        builder.addAll(this.files.entries.map { Pair(it.key, it.value) })
        builder.addAll(this.folders.entries.map { Pair(it.key, it.value) })
        return builder.build()
    }
}