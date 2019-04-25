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

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FileSizeFormatTest {
    @Test
    fun tenBytes() {
        assertThat(10L.toByteRepresentation()).isEqualTo("10 Bytes")
    }

    @Test
    fun zeroBytes() {
        assertThat(0L.toByteRepresentation()).isEqualTo("0 Bytes")
    }

    @Test
    fun oneKB() {
        assertThat(1024L.toByteRepresentation()).isEqualTo("1.00 KB")
    }

    @Test
    fun tenKB() {
        assertThat(10240L.toByteRepresentation()).isEqualTo("10.00 KB")
    }

    @Test
    fun tenMB() {
        assertThat((1024L * 1024L * 10L).toByteRepresentation()).isEqualTo("10.00 MB")
    }

    @Test
    fun tenGB() {
        assertThat((1024L * 1024L * 1024L * 10L).toByteRepresentation()).isEqualTo("10.00 GB")
    }

    @Test
    fun tenTB() {
        assertThat((1024L * 1024L * 1024L * 1024L * 10L).toByteRepresentation()).isEqualTo("10.00 TB")
    }

    @Test
    fun tenPB() {
        assertThat((1024L * 1024L * 1024L * 1024L * 1024L * 10L).toByteRepresentation()).isEqualTo("10.00 PB")
    }

    @Test
    fun tenEB() {
        assertThat((1024L * 1024L * 1024L * 1024L * 1024L * 1024L).toByteRepresentation()).isEqualTo("1.00 EB")
    }

    @Test
    fun threes() {
        assertThat(3500L.toByteRepresentation()).isEqualTo("3.42 KB")
    }

    @Test
    fun twoAndAQuarter() {
        assertThat(2359296L.toByteRepresentation()).isEqualTo("2.25 MB")
        assertThat(2359295L.toByteRepresentation()).isEqualTo("2.25 MB")
    }
}
