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

