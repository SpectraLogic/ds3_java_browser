package com.spectralogic.dsbrowser.gui.util;

import java.text.DecimalFormat;

public class FileSizeFormat {

    public static String getFileSizeType(final long size) {
        final DecimalFormat df = new DecimalFormat("0.00");

        final double sizeKb = 1024.00;
        final double sizeMo = sizeKb * sizeKb;
        final double sizeGo = sizeMo * sizeKb;
        final double sizeTerra = sizeGo * sizeKb;
        final double sizePeta = sizeTerra * sizeKb;
        final double sizeExa = sizePeta * sizeKb;

        if (size == 0) {
            return "--";
        } else if (size < sizeMo)
            return df.format(size / sizeKb) + "KB";
        else if (size < sizeGo)
            return df.format(size / sizeMo) + "MB";
        else if (size < sizeTerra)
            return df.format(size / sizeGo) + "GB";
        else if (size < sizePeta)
            return df.format(size / sizeTerra) + "TB";
        else if (size < sizeExa)
            return df.format(size / sizePeta) + "PB";
        else
            return "";
    }
}
