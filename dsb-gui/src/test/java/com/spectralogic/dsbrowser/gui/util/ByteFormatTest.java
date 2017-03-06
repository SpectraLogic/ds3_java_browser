package com.spectralogic.dsbrowser.gui.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ByteFormatTest {
    @Test
    public void humanReadableByteCountKiB() throws Exception {
        final String byteCount1 = ByteFormat.humanReadableByteCount(9907, false);
        assertThat(byteCount1, is("9.7 KiB"));

    }

    @Test
    public void humanReadableByteCountkB() throws Exception {
        final String byteCount2 = ByteFormat.humanReadableByteCount(9907, true);
        assertThat(byteCount2, is("9.9 kB"));
    }

    @Test
    public void humanReadableByteCountB() throws Exception {
        final String byteCount3 = ByteFormat.humanReadableByteCount(907, true);
        assertThat(byteCount3, is("907 B"));
    }

    @Test
    public void humanReadableByteCountMiB() throws Exception {
        final String byteCount1 = ByteFormat.humanReadableByteCount(9999999, false);
        assertThat(byteCount1, is("9.5 MiB"));
    }

    @Test
    public void humanReadableByteCountGiB() throws Exception {
        final String byteCount1 = ByteFormat.humanReadableByteCount(9999999999L, false);
        assertThat(byteCount1, is("9.3 GiB"));
    }

    @Test
    public void humanReadableByteCountTiB() throws Exception {
        final String byteCount1 = ByteFormat.humanReadableByteCount(9999999999999L, false);
        assertThat(byteCount1, is("9.1 TiB"));
    }



}