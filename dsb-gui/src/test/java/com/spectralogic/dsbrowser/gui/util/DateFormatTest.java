package com.spectralogic.dsbrowser.gui.util;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by linchpin on 30/1/17.
 */
public class DateFormatTest {
    @Test
    public void formatDate() throws Exception {
        final Date d = new Date(1485757788120l);
        final String formatedDate = DateFormat.formatDate(d);
        assertThat(formatedDate,is("1/30/2017 11:59:48"));
    }

}