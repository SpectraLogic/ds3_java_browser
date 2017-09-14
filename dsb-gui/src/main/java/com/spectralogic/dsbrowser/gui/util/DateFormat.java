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

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public final class DateFormat {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static String now() {
        return FORMAT.format(Instant.now());
    }

    public static String format(final Date date) {
       return FORMAT.format(date.toInstant());
    }

    /**
     * convert seconds in to appropriate days or hours o rminutes
     *
     * @param seconds
     * @return
     */
    public static String timeConversion(final long seconds) {
        final long day = (seconds / (60 * 60 * 24));
        if (day >= 1) {
            if (day == 1) {

                return day + " day ";
            } else {
                return day + " days ";
            }
        }
        final long hour = (seconds / (60 * 60));
        if (hour >= 1) {
            if (hour == 1) {
                return hour + " hour ";
            } else {
                return hour + " hours ";
            }
        }
        final long minute = (seconds / 60);
        if (minute >= 1) {
            if (minute == 1) {
                return minute + " minute ";
            } else {
                return minute + " minutes ";
            }
        }
        if (seconds > 1) {
            return seconds + " seconds ";
        } else {
            return seconds + " second ";
        }

    }

    public static String formatDate(final long timeInMillis) {
        final SimpleDateFormat sdf = new SimpleDateFormat(StringConstants.SIMPLE_DATE_FORMAT, Locale.US);
        return sdf.format(new Date(timeInMillis));
    }
}
