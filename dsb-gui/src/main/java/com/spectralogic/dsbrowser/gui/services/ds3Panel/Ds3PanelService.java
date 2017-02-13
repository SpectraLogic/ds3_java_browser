package com.spectralogic.dsbrowser.gui.services.ds3Panel;

import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.spectrads3.GetBucketsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetBucketsSpectraS3Response;
import com.spectralogic.ds3client.models.Bucket;
import com.spectralogic.ds3client.models.ListBucketResult;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class Ds3PanelService {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PanelService.class);

    /**
     * check if bucket contains files or folders
     *
     * @param bucketName bucketName
     * @return true if bucket is empty else return false
     */
    public static boolean checkIfBucketEmpty(final String bucketName, final Session session) {
        try {
            final GetBucketRequest request = new GetBucketRequest(bucketName).withDelimiter(StringConstants.FORWARD_SLASH).withMaxKeys(1);
            final GetBucketResponse bucketResponse = session.getClient().getBucket(request);
            final ListBucketResult listBucketResult = bucketResponse.getListBucketResult();
            return Guard.isNullOrEmpty(listBucketResult.getObjects()) && Guard.isNullOrEmpty(listBucketResult.getCommonPrefixes());

        } catch (final Exception e) {
            LOG.error("could not get bucket response", e);
            return false;
        }
    }

    public static List<Bucket> setSearchableBucket(final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItem,
                                                   final Session session, final TreeTableView<Ds3TreeTableValue> treeTableView) {
        try {
            if (null != treeTableView) {
                ObservableList<TreeItem<Ds3TreeTableValue>> selectedItemTemp = selectedItem;
                if (null == selectedItemTemp) {
                    selectedItemTemp = FXCollections.observableArrayList();
                    if (null != treeTableView.getRoot() && null != treeTableView.getRoot().getValue()) {
                        selectedItemTemp.add(treeTableView.getRoot());
                    }
                }
                final GetBucketsSpectraS3Request getBucketsSpectraS3Request = new GetBucketsSpectraS3Request();
                final GetBucketsSpectraS3Response response = session.getClient().getBucketsSpectraS3(getBucketsSpectraS3Request);
                final List<Bucket> buckets = response.getBucketListResult().getBuckets();
                if (!Guard.isNullOrEmpty(selectedItemTemp)) {
                    final ImmutableSet<String> bucketNameSet = selectedItemTemp.stream().map(item -> item.getValue()
                            .getBucketName()).collect(GuavaCollectors.immutableSet());
                    return buckets.stream().filter(bucket -> bucketNameSet.contains(bucket.getName())).collect
                            (GuavaCollectors.immutableList());
                } else {
                    return buckets;
                }
            } else {
                throw new NullPointerException("TreeTableView can't be null");
            }
        } catch (final Exception e) {
            LOG.error("Something went wrong!", e);
            return null;
        }
    }
}
