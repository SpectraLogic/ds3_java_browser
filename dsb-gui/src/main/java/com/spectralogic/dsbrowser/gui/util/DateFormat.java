package com.spectralogic.dsbrowser.gui.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateFormat {

    public static String formatDate(final Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy HH:mm:ss", Locale.US);
        if (date == null)
            return "";
        return sdf.format(date);
    }
}
