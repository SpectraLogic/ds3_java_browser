package com.spectralogic.dsbrowser.gui.services.ds3Panel;

import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.models.Bucket;
import com.spectralogic.ds3client.models.ListBucketResult;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Ds3PanelService {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3Common.class);

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
            return listBucketResult.getObjects().size() == 0 && listBucketResult.getCommonPrefixes().size() == 0;

        } catch (final Exception e) {
            LOG.error("could not get bucket response", e);
            return false;
        }
    }

    public static List<Bucket> setSearchableBucket(final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItem, final List<Bucket> buckets, final TreeTableView<Ds3TreeTableValue> treeTableView) {

        ObservableList<TreeItem<Ds3TreeTableValue>> selectedItemTemp = selectedItem;

        final List<Bucket> searchableBuckets = new ArrayList<>();

        if (null == selectedItemTemp || selectedItemTemp.size() == 0) {
            selectedItemTemp = FXCollections.observableArrayList();
            selectedItem.add(treeTableView.getRoot());
        }

        if (selectedItemTemp.size() != 0) {
            selectedItemTemp.stream().forEach(temp1 -> buckets.stream().forEach(bucket -> {
                if (bucket.getName().equals(temp1.getValue().getBucketName()))
                    searchableBuckets.add(bucket);
            }));
        } else {
            searchableBuckets.addAll(buckets);
        }

        return searchableBuckets;
    }

}
