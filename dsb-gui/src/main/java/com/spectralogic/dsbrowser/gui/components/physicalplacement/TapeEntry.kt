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
package com.spectralogic.dsbrowser.gui.components.physicalplacement

data class TapeEntry(
    val barcode: String,
    val serialNumber: String,
    val type: String,
    val state: String,
    val writeProtected: Boolean,
    val availableCapacity: String,
    val usedCapacity: String,
    val tapePartition: String,
    val lastModified: String,
    val ejectLabel: String,
    val ejectLocation: String
)
