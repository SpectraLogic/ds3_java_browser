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

import java.util.UUID;

public class CreateBucketModel {

    private final String dataPolicy;

    private final UUID id;

    //to be added more

    public CreateBucketModel() {
        this("", null);
    }

    public CreateBucketModel(final String dataPolicy, final UUID id) {
        this.dataPolicy = dataPolicy;
        this.id = id;
    }

    public String getDataPolicy() {
        return dataPolicy;
    }

    public UUID getId() {
        return id;
    }
}
