/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

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
        final ImmutableList<CreateBucketModel> value1 = new ArrayList<CreateBucketModel>().stream().collect(GuavaCollectors.immutableList());
        final CreateBucketWithDataPoliciesModel value = new CreateBucketWithDataPoliciesModel(value1, new Session(), new Workers());
        assertNotEquals(value.getSession(), null);
    }

    @Test
    public void getWorkers() throws Exception {
        final ImmutableList<CreateBucketModel> value1 = new ArrayList<CreateBucketModel>().stream().collect(GuavaCollectors.immutableList());
        final CreateBucketWithDataPoliciesModel value = new CreateBucketWithDataPoliciesModel(value1, new Session(), new Workers());
        assertNotEquals(value.getWorkers(), null);
    }

    @Test
    public void getDataPolicies() throws Exception {
        final ImmutableList<CreateBucketModel> value1 = new ArrayList<CreateBucketModel>().stream().collect(GuavaCollectors.immutableList());
        final CreateBucketWithDataPoliciesModel value = new CreateBucketWithDataPoliciesModel(value1, new Session(), new Workers());
        assertNotEquals(value.getDataPolicies(), null);

    }

}