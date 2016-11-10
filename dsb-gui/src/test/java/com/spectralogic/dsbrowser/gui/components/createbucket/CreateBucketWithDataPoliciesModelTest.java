package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CreateBucketWithDataPoliciesModelTest {
    @Test
    public void getInitializedValue() throws Exception {
        final CreateBucketWithDataPoliciesModel value = new CreateBucketWithDataPoliciesModel();
        assertEquals(value.getDataPolicies(), null);
        assertEquals(value.getSession(), null);
        assertEquals(value.getWorkers(), null);
    }

    @Test
    public void getSession() throws Exception {
        ImmutableList<CreateBucketModel> value1 = new ArrayList<CreateBucketModel>().stream().map(i -> i).collect(GuavaCollectors.immutableList());
        final CreateBucketWithDataPoliciesModel value = new CreateBucketWithDataPoliciesModel(value1, new Session(), new Workers());
        assertNotEquals(value.getSession(), null);
    }

    @Test
    public void getWorkers() throws Exception {
        ImmutableList<CreateBucketModel> value1 = new ArrayList<CreateBucketModel>().stream().map(i -> i).collect(GuavaCollectors.immutableList());
        final CreateBucketWithDataPoliciesModel value = new CreateBucketWithDataPoliciesModel(value1, new Session(), new Workers());
        assertNotEquals(value.getWorkers(), null);
    }

    @Test
    public void getDataPolicies() throws Exception {
        ImmutableList<CreateBucketModel> value1 = new ArrayList<CreateBucketModel>().stream().map(i -> i).collect(GuavaCollectors.immutableList());
        final CreateBucketWithDataPoliciesModel value = new CreateBucketWithDataPoliciesModel(value1, new Session(), new Workers());
        assertNotEquals(value.getDataPolicies(), null);

    }

}