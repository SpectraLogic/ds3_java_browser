package com.spectralogic.dsbrowser.gui.util;

import java.text.DecimalFormat;

public class FileSizeFormat {

    final static DecimalFormat df = new DecimalFormat("0.00");
    final static DecimalFormat dfbytes = new DecimalFormat("0");

    final static double sizeKb = 1024.00;
    final static double sizeMo = sizeKb * sizeKb;
    final static double sizeGo = sizeMo * sizeKb;
    final static double sizeTerra = sizeGo * sizeKb;
    final static double sizePeta = sizeTerra * sizeKb;
    final static double sizeExa = sizePeta * sizeKb;

    public static String getFileSizeType(final long size) {

        if (size == 0) {
            return "--";
        }
        else if(size == -1)
            return "";
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

        final double aFloat = Double.valueOf(size.replaceAll("[^.0-9]", ""));

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
