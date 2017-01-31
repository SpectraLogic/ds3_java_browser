package com.spectralogic.dsbrowser.gui.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by linchpin on 30/1/17.
 */
public class FileSizeFormatTest {
    @Test
    public void getFileSizeType() throws Exception {
        final String fileSizeType = FileSizeFormat.getFileSizeType(65553);
        assertThat(fileSizeType,is("64.02 KB"));
    }

    @Test
    public void convertSizeToByte() throws Exception {
        final long size = FileSizeFormat.convertSizeToByte("64.02 KB");
        assertThat(size,is(65556L));
    }

}