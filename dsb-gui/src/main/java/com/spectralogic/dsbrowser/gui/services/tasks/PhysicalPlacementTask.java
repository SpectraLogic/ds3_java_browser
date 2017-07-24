package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsSpectraS3Response;
import com.spectralogic.ds3client.models.ListBucketResult;
import com.spectralogic.ds3client.models.PhysicalPlacement;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicalPlacementTask extends Ds3Task {
    private final static Logger LOG = LoggerFactory.getLogger(PhysicalPlacementTask.class);

    private final Ds3Common ds3Common;
    private final ImmutableList<TreeItem<Ds3TreeTableValue>> values;
    private final Workers workers;

    public PhysicalPlacementTask(final Ds3Common ds3Common, final ImmutableList<TreeItem<Ds3TreeTableValue>> values, final Workers workers) {
        this.ds3Common = ds3Common;
        this.values = values;
        this.workers = workers;
    }

    @Override
    protected PhysicalPlacement call() throws Exception {
        final Ds3Client client = ds3Common.getCurrentSession().getClient();
        final Ds3TreeTableValue value = values.get(0).getValue();
        ImmutableList<Ds3Object> list = null;
        if (null != value && (value.getType().equals(Ds3TreeTableValue.Type.Bucket))) {
        } else if (value.getType().equals(Ds3TreeTableValue.Type.File)) {
            list = values.stream().map(item -> new Ds3Object(item.getValue().getFullName(), item.getValue().getSize()))
                    .collect(GuavaCollectors.immutableList());
        //TODO This always evaluates to tue at this point
        } else if (null != value && value.getType().equals(Ds3TreeTableValue.Type.Directory)) {
            final PhysicalPlacementTask.GetDirectoryObjects getDirectoryObjects = new PhysicalPlacementTask.GetDirectoryObjects(value.getBucketName(), value.getDirectoryName(), ds3Common);
            workers.execute(getDirectoryObjects);
            final ListBucketResult listBucketResult = getDirectoryObjects.getValue();
            if (null != listBucketResult) {
                list = listBucketResult.getObjects().stream().map(item -> new Ds3Object(item.getKey(), item.getSize()))
                        .collect(GuavaCollectors.immutableList());
            }
        }
        final GetPhysicalPlacementForObjectsSpectraS3Response response = client
                .getPhysicalPlacementForObjectsSpectraS3(
                        new GetPhysicalPlacementForObjectsSpectraS3Request(value.getBucketName(), list));
        return response.getPhysicalPlacementResult();
    }


    private static class GetDirectoryObjects extends Task<ListBucketResult> {
        final String bucketName, directoryFullName;
        final Ds3Common ds3Common;

        public GetDirectoryObjects(final String bucketName, final String directoryFullName, final Ds3Common ds3Common) {
            this.bucketName = bucketName;
            this.directoryFullName = directoryFullName;
            this.ds3Common = ds3Common;
        }

        @Override
        protected ListBucketResult call() throws Exception {
            try {
                final GetBucketRequest request = new GetBucketRequest(bucketName);
                request.withPrefix(directoryFullName);
                final GetBucketResponse bucketResponse = ds3Common.getCurrentSession().getClient().getBucket(request);
                return bucketResponse.getListBucketResult();
            } catch (final Exception e) {
                LOG.error("unable to get bucket response", e);
                return null;
            }
        }
    }


}

