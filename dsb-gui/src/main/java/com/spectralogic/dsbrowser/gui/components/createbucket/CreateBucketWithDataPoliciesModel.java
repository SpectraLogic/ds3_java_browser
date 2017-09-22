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

public class CreateBucketWithDataPoliciesModel {

    private final ImmutableList<CreateBucketModel> dataPolicies;
    private final Session session;
    private final Workers workers;

    public CreateBucketWithDataPoliciesModel() {
        this(null, null, null);
    }

    public CreateBucketWithDataPoliciesModel(final ImmutableList<CreateBucketModel> dataPolicies, final Session session, final Workers workers) {
        this.dataPolicies = dataPolicies;
        this.session = session;
        this.workers = workers;
    }

    public Session getSession() {
        return session;
    }

    public Workers getWorkers() {
        return workers;
    }

    public ImmutableList<CreateBucketModel> getDataPolicies() {
        return dataPolicies;
    }
}
