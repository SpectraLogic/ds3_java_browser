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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DateFormatTest {

    private final DateTimeUtils dateTimeUtils = new DateTimeUtils(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    @Test
    public void formatDateTest() throws Exception {
        final Instant date = Instant.ofEpochMilli(1485757788120L);
        final String formatedDate = dateTimeUtils.format(date);
        assertThat(formatedDate, is("2017-01-29T23:29:48.12"));
    }

    @Test
    public void timeConversionTest() throws Exception {

        final String timeConversionSecond = DateTimeUtils.timeConversion(1);
        assertThat(timeConversionSecond, is("1 second "));

        final String timeConversionSeconds = DateTimeUtils.timeConversion(1 * 2);
        assertThat(timeConversionSeconds, is("2 seconds "));

        final String timeConversion1 = DateTimeUtils.timeConversion(60);
        assertThat(timeConversion1, is("1 minute "));

        final String timeConversion2 = DateTimeUtils.timeConversion(60 * 2);
        assertThat(timeConversion2, is("2 minutes "));

        final String timeConversion3 = DateTimeUtils.timeConversion(60 * 60);
        assertThat(timeConversion3, is("1 hour "));

        final String timeConversion4 = DateTimeUtils.timeConversion(60 * 60 * 2);
        assertThat(timeConversion4, is("2 hours "));

        final String timeConversion5 = DateTimeUtils.timeConversion(60 * 60 * 24);
        assertThat(timeConversion5, is("1 day "));

        final String timeConversion6 = DateTimeUtils.timeConversion(60 * 60 * 24 * 2);
        assertThat(timeConversion6, is("2 days "));
    }
}