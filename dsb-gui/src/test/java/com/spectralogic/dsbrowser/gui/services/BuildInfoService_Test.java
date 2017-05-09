package com.spectralogic.dsbrowser.gui.services;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class BuildInfoService_Test {

    @Test
    public void parseDateString() {
        final String buildDate = "Wed Mar 15 11:10:25 MDT 2017";
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(BuildInfoServiceImpl.dateTimeFormatterPattern);
        final LocalDateTime ldt = LocalDateTime.parse(buildDate, dtf);

        assertThat(ldt.toString(), is(buildDate));
    }
}
