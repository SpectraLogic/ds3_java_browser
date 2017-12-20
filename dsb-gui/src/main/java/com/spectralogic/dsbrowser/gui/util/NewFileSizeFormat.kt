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

package com.spectralogic.dsbrowser.gui.util

const val bigger = 1024
const val bytes = "Bytes"
const val kilo = "KB"
const val mega = "MB"
const val giga = "GB"
const val tera = "TB"
const val peta = "PB"
const val exa = "EB"
val listOfPrefixs = arrayOf(bytes, kilo, mega, giga, tera, peta, exa)


public fun Long.toByteRepresentation(): String {
    var value = this
    var timesReduced = 0
    while (timesReduced < listOfPrefixs.size && value >= bigger) {
        value = value.shr(10)
        timesReduced++
    }
   return "$value ${listOfPrefixs[timesReduced]}"
}
