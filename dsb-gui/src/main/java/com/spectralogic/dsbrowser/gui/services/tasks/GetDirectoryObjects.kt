/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */
package com.spectralogic.dsbrowser.gui.services.tasks
import com.google.inject.assistedinject.Assisted
import com.spectralogic.ds3client.commands.GetBucketRequest
import com.spectralogic.ds3client.models.ListBucketResult
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common
import javafx.concurrent.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Inject

class GetDirectoryObjects @Inject constructor(
    @param:Assisted("bucketName") val bucketName: String,
    @param:Assisted("directoryFullName") val directoryFullName: String,
    val ds3Common: Ds3Common
) : Task<ListBucketResult?>() {

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(GetDirectoryObjects::class.java)
        public interface GetDirectoryObjectsFactory {
            fun create(@Assisted("bucketName") bucketName: String, @Assisted("directoryFullName") directoryFullName: String): GetDirectoryObjects
        }
    }

    override fun call(): ListBucketResult? {
        try {
            val request = GetBucketRequest(bucketName)
            request.withPrefix(directoryFullName)
            val bucketResponse = ds3Common.currentSession.client.getBucket(request)
            return bucketResponse.listBucketResult
        } catch (e: Exception) {
            LOG.error("unable to get bucket response", e)
            return null
        }
    }
}
