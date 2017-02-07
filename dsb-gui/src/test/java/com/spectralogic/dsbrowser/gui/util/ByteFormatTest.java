package com.spectralogic.dsbrowser.gui.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ByteFormatTest {
    @Test
    public void humanReadableByteCount() throws Exception {
        final String byteCount1 = ByteFormat.humanReadableByteCount(9907, false);
        final String byteCount2 = ByteFormat.humanReadableByteCount(9907, true);
        final String byteCount3 = ByteFormat.humanReadableByteCount(907, true);
        assertThat(byteCount1,is("9.7 KiB"));
        assertThat(byteCount2,is("9.9 kB"));
        assertThat(byteCount3,is("907 B"));
    }

}