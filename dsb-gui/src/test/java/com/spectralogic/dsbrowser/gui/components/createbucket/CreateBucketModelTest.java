package com.spectralogic.dsbrowser.gui.components.createbucket;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;

/**
 * Created by Kunal on 27-Jun-16.
 */
public class CreateBucketModelTest {
    @Test
    public void getInitializedDataPolicy() throws Exception {
        final CreateBucketModel value = new CreateBucketModel();
        assertThat(value.getDataPolicy(), is(""));
    }

    @Test
    public void getDataPolicy() throws Exception {
        final CreateBucketModel value = new CreateBucketModel("fake");
        assertThat(value.getDataPolicy(), is("fake"));
    }

}