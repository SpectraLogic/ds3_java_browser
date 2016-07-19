package com.spectralogic.dsbrowser.gui.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormat {

    public static String formatDate(final Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy HH:mm:ss");
        if (date == null)
            return "";
        return sdf.format(date);
    }
}
