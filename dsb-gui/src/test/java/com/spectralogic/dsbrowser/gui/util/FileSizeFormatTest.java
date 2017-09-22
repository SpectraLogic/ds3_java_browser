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

public class FileSizeFormatTest {

    @Test
    public void getFileSizeType() throws Exception {
        final String fileSizeType = FileSizeFormat.getFileSizeType(1023);
        assertThat(fileSizeType, is("1023 Bytes"));

        final String fileSizeType1 = FileSizeFormat.getFileSizeType(1024);
        assertThat(fileSizeType1, is("1.00 KB"));

        final String fileSizeType2 = FileSizeFormat.getFileSizeType(1024 * 1024);
        assertThat(fileSizeType2, is("1.00 MB"));

        final String fileSizeType3 = FileSizeFormat.getFileSizeType(1024 * 1024 * 1024);
        assertThat(fileSizeType3, is("1.00 GB"));

        final String fileSizeType4 = FileSizeFormat.getFileSizeType(1024 * 1024 * 1024 * 1024L);
        assertThat(fileSizeType4, is("1.00 TB"));

        final String fileSizeType5 = FileSizeFormat.getFileSizeType(1024 * 1024 * 1024 * 1024 * 1024L);
        assertThat(fileSizeType5, is("--"));
    }

    @Test
    public void convertSizeToByte() throws Exception {
        final long size = FileSizeFormat.convertSizeToByte("1023 Bytes");
        assertThat(size, is(1023L));

        final long size1 = FileSizeFormat.convertSizeToByte("1.00 KB");
        assertThat(size1, is(1024L));

        final long size2 = FileSizeFormat.convertSizeToByte("1.00 MB");
        assertThat(size2, is(1024 * 1024L));

        final long size3 = FileSizeFormat.convertSizeToByte("1.00 GB");
        assertThat(size3, is(1024 * 1024 * 1024L));

        final long size4 = FileSizeFormat.convertSizeToByte("1.00 TB");
        assertThat(size4, is(1024 * 1024 * 1024 * 1024L));
    }

}