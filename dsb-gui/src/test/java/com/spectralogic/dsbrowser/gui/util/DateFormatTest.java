package com.spectralogic.dsbrowser.gui.util;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DateFormatTest {
    @Test
    public void formatDateTest() throws Exception {
        final Date date = new Date(1485757788120l);
        final String formatedDate = DateFormat.formatDate(date);
        assertThat(formatedDate, is("1/30/2017 11:59:48"));
    }

    @Test
    public void timeConversionTest() throws Exception {
        final Date date = new Date(1485757788120l);

        final String timeConversionSecond = DateFormat.timeConversion(1);
        assertThat(timeConversionSecond, is("1 second "));

        final String timeConversionSeconds = DateFormat.timeConversion(1 * 2);
        assertThat(timeConversionSeconds, is("2 seconds "));

        final String timeConversion1 = DateFormat.timeConversion(60);
        assertThat(timeConversion1, is("1 minute "));

        final String timeConversion2 = DateFormat.timeConversion(60 * 2);
        assertThat(timeConversion2, is("2 minutes "));

        final String timeConversion3 = DateFormat.timeConversion(60 * 60);
        assertThat(timeConversion3, is("1 hour "));

        final String timeConversion4 = DateFormat.timeConversion(60 * 60 * 2);
        assertThat(timeConversion4, is("2 hours "));

        final String timeConversion5 = DateFormat.timeConversion(60 * 60 * 24);
        assertThat(timeConversion5, is("1 day "));

        final String timeConversion6 = DateFormat.timeConversion(60 * 60 * 24 * 2);
        assertThat(timeConversion6, is("2 days "));
    }
}