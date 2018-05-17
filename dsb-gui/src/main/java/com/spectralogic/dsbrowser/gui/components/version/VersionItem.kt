/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
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

package com.spectralogic.dsbrowser.gui.components.version

import com.spectralogic.dsbrowser.gui.util.DateTimeUtils
import com.spectralogic.dsbrowser.gui.util.toByteRepresentation
import java.util.*

data class VersionItem(
    val key: String,
    val lastModified: Date,
    val version: UUID,
    val size: Long) {

    fun getName() : String = key.substring((key.lastIndexOf("/") + 1))
    fun getCreated() : String = DateTimeUtils().format(lastModified)
    fun getVersionId() : String = version.toString()
    fun getSize() : String = size.toByteRepresentation()
}