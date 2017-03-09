/*
 * ****************************************************************************
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
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateFormat {

    public static String formatDate(final Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat("M/dd/yyyy HH:mm:ss", Locale.US);
        if (date == null)
            return "";
        return sdf.format(date);
    }
}
