package com.spectralogic.dsbrowser.gui.components.version

import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.toByteRepresentation
import java.util.*

data class VersionItem(
    val key: String,
    val lastModified: Date,
    val version: UUID,
    val size: Long) {

    fun getName() : String = key.substring(key.lastIndexOf("/"))
    fun getCreated() : String = DateTimeUtils().format(lastModified)
    fun getVersionId() : String = version.toString()
    fun getSize() : String = size.toByteRepresentation()
}