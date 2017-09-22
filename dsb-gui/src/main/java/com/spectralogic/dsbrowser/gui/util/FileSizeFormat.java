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

import java.text.DecimalFormat;
import java.util.regex.Pattern;

public final class FileSizeFormat {

    private final static DecimalFormat df = new DecimalFormat("0.00");
    private final static DecimalFormat dfbytes = new DecimalFormat("0");

    private final static double sizeKb = 1024.00;
    private final static double sizeMo = sizeKb * sizeKb;
    private final static double sizeGo = sizeMo * sizeKb;
    private final static double sizeTerra = sizeGo * sizeKb;
    private final static double sizePeta = sizeTerra * sizeKb;
    private final static double sizeExa = sizePeta * sizeKb;
    private static final Pattern DECIMAL = Pattern.compile("[^.0-9]");

    public static String getFileSizeType(final long size) {

        if (size == 0) {
            return StringConstants.TWO_DASH;
        }
        else if(size == -1)
            return StringConstants.EMPTY_STRING;
        else if (size < sizeKb)
            return dfbytes.format(size) + " Bytes";
        else if (size < sizeMo)
            return df.format(size / sizeKb) + " KB";
        else if (size < sizeGo)
            return df.format(size / sizeMo) + " MB";
        else if (size < sizeTerra)
            return df.format(size / sizeGo) + " GB";
        else if (size < sizePeta)
            return df.format(size / sizeTerra) + " TB";
        else if (size < sizeExa)
            return df.format(size / sizePeta) + " PB";
        else
            return "";
    }

    public static long convertSizeToByte(final String size) {

        final double aFloat = Double.valueOf(DECIMAL.matcher(size).replaceAll(StringConstants.EMPTY_STRING));

        if (size.contains("KB")) {
            return new Double(aFloat * sizeKb).longValue();
        } else if (size.contains("MB")) {
            return new Double(aFloat * sizeMo).longValue();
        } else if (size.contains("GB")) {
            return new Double(aFloat * sizeGo).longValue();
        } else if (size.contains("TB")) {
            return new Double(aFloat * sizeTerra).longValue();
        } else if (size.contains("PB")) {
            return new Double(aFloat * sizePeta).longValue();
        } else {
            return new Double(aFloat).longValue();
        }
    }

}
