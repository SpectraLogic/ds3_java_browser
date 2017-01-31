package com.spectralogic.dsbrowser.gui.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by linchpin on 30/1/17.
 */
public class ByteFormatTest {
    @Test
    public void humanReadableByteCount() throws Exception {
        final String byteCount = ByteFormat.humanReadableByteCount(9907, false);
        assertThat(byteCount,is("9.7 KiB"));
    }

}