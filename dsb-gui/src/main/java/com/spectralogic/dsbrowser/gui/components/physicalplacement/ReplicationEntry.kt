package com.spectralogic.dsbrowser.gui.components.physicalplacement

import com.spectralogic.ds3client.models.Ds3TargetAccessControlReplication
import com.spectralogic.ds3client.models.Quiesced
import com.spectralogic.ds3client.models.TargetState
import java.util.*

data class ReplicationEntry(
    val dataPathEndPoint: String,
    val dataPathPort: Int,
    val authId: String,
    val id: String,
    val name: String,
    val accessControlReplication: String,
    val state: String,
    val permitGoingOutOfSync: Boolean,
    val quiesced: String
)
