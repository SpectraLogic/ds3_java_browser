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

    /**
     * convert seconds in to appropriate days or hours o rminutes
     * @param seconds
     * @return
     */
    public static String timeConversion(final long seconds){
        final long day = (seconds/(60*60*24));
        if(day>=1){
            if(day==1){
                return day + " day ";
            }else{
                return day + " days ";
            }
        }
        final long hour = (seconds/(60*60));
        if(hour>=1){
            if(hour==1){
                return  hour + " hour ";
            }else{
                return  hour + " hours ";
            }
        }
        final long minute = (seconds/60);
        if(minute>=1){
            if(minute==1){
                return minute + " minute ";
            }else{
                return minute + " minutes ";
            }
        }
        if(seconds>1){
            return seconds + " seconds ";
        }else {
            return seconds + " second ";
        }

    }
}
