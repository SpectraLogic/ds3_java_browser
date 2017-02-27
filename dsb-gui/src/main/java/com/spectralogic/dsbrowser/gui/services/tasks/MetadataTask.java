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
