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

package com.spectralogic.dsbrowser.gui.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ByteFormatTest {
    @Test
    public void humanReadableByteCountKiB() throws Exception {
        final String byteCount1 = ByteFormat.humanReadableByteCount(9907, false);
        assertThat(byteCount1, is("9.7 KiB"));

    }

    @Test
    public void humanReadableByteCountkB() throws Exception {
        final String byteCount2 = ByteFormat.humanReadableByteCount(9907, true);
        assertThat(byteCount2, is("9.9 kB"));
    }

    @Test
    public void humanReadableByteCountB() throws Exception {
        final String byteCount3 = ByteFormat.humanReadableByteCount(907, true);
        assertThat(byteCount3, is("907 B"));
    }

    @Test
    public void humanReadableByteCountMiB() throws Exception {
        final String byteCount1 = ByteFormat.humanReadableByteCount(9999999, false);
        assertThat(byteCount1, is("9.5 MiB"));
    }

    @Test
    public void humanReadableByteCountGiB() throws Exception {
        final String byteCount1 = ByteFormat.humanReadableByteCount(9999999999L, false);
        assertThat(byteCount1, is("9.3 GiB"));
    }

    @Test
    public void humanReadableByteCountTiB() throws Exception {
        final String byteCount1 = ByteFormat.humanReadableByteCount(9999999999999L, false);
        assertThat(byteCount1, is("9.1 TiB"));
    }



}