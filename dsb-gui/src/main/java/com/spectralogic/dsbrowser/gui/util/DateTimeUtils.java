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

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Date;

public final class DateTimeUtils {

    private static final DateTimeFormatter DEFAULT_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
    private static final DateTimeFormatter DEFAULT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DEFAULT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int SECONDS_IN_DAY = (60 * 60 * 24);
    private static final int SECONDS_IN_HOUR = 60 * 60;
    private static final int SECONDS_IN_MINUTE = 60;
    private final DateTimeFormatter formatter;

    public DateTimeUtils() {
        this.formatter = DEFAULT_FORMAT;
    }

    public DateTimeUtils(@Nonnull final DateTimeFormatter formatter) {
        Preconditions.checkNotNull(formatter);
        this.formatter = formatter;

    }

    public String nowAsString() {
        return formatter.format(LocalDateTime.now());
    }

    public String format(final Temporal t) {
        return formatter.format(t);
    }

    public String format(final Instant t) {
        return formatter.format(t.atZone(ZoneId.systemDefault()));
    }

    public String format(final Date date) {
        if(date == null) {
            return "";
        } else {
            return formatter.format(date.toInstant().atZone(ZoneId.systemDefault()));
        }
    }

    /**
     * convert seconds in to appropriate days or hours o rminutes
     *
     * @param seconds
     * @return
     */
    public static String timeConversion(final long seconds) {
        final long day = (seconds / SECONDS_IN_DAY);
        if (day >= 1) {
            if (day == 1) {

                return day + " day ";
            } else {
                return day + " days ";
            }
        }
        final long hour = (seconds / SECONDS_IN_HOUR);
        if (hour >= 1) {
            if (hour == 1) {
                return hour + " hour ";
            } else {
                return hour + " hours ";
            }
        }
        final long minute = (seconds / SECONDS_IN_MINUTE);
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

    public String formatDate(final long timeInMillis) {
        return formatter.format(Instant.ofEpochMilli(timeInMillis).atZone(ZoneId.systemDefault()));
    }
}
