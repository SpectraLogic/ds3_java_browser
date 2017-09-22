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

package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.HeadObjectRequest;
import com.spectralogic.ds3client.commands.HeadObjectResponse;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.metadata.Ds3Metadata;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import javafx.scene.control.TreeItem;

public class MetadataTask extends Ds3Task {

    private final Ds3Common ds3Common;
    private final ImmutableList<TreeItem<Ds3TreeTableValue>> values;

    public MetadataTask(final Ds3Common ds3Common, final ImmutableList<TreeItem<Ds3TreeTableValue>> values) {
        this.ds3Common = ds3Common;
        this.values = values;
    }

    @Override
    protected Ds3Metadata call() throws Exception {
        final Ds3Client client = ds3Common.getCurrentSession().getClient();
        final Ds3TreeTableValue value = values.get(0).getValue();
        final HeadObjectResponse headObjectResponse = client.headObject(new HeadObjectRequest(value.getBucketName(), value.getFullName()));
        return new Ds3Metadata(headObjectResponse.getMetadata(), headObjectResponse.getObjectSize(), value.getFullName(), value.getLastModified());
    }

}
