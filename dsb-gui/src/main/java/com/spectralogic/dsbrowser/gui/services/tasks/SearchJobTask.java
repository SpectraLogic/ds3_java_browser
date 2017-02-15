package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.commands.spectrads3.GetObjectsWithFullDetailsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetObjectsWithFullDetailsSpectraS3Response;
import com.spectralogic.ds3client.models.Bucket;
import com.spectralogic.ds3client.models.BulkObject;
import com.spectralogic.ds3client.models.DetailedS3Object;
import com.spectralogic.ds3client.models.S3ObjectType;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SearchJobTask extends Ds3Task<List<Ds3TreeTableItem>> {
    private static final Logger LOG = LoggerFactory.getLogger(SearchJobTask.class);
    private final List<Bucket> searchableBuckets;
    private final String searchText;
    private final Session session;
    private final Workers workers;
    private final Ds3Common ds3Common;

    public SearchJobTask(final List<Bucket> searchableBuckets, final String searchText, final Session session,
                         final Workers workers, final Ds3Common ds3Common) {
        this.searchableBuckets = searchableBuckets;
        this.searchText = searchText;
        this.session = session;
        this.workers = workers;
        this.ds3Common = ds3Common;
    }

    @Override
    protected List<Ds3TreeTableItem> call() throws Exception {
        try {
            final List<Ds3TreeTableItem> list = new ArrayList<>();
            searchableBuckets.stream().forEach(bucket -> {
                if (bucket.getName().contains(searchText)) {
                    Platform.runLater(() -> {
                        if (null != ds3Common.getDeepStorageBrowserPresenter()) {
                            ds3Common.getDeepStorageBrowserPresenter().logText(StringBuilderUtil.bucketFoundMessage
                                    (searchText).toString(), LogType.INFO);
                        }
                    });
                    final Ds3TreeTableValue value = new Ds3TreeTableValue(bucket.getName(), bucket.getName(), Ds3TreeTableValue.Type.Bucket,
                            0, StringConstants.TWO_DASH, StringConstants.TWO_DASH, false, null);
                    list.add(new Ds3TreeTableItem(value.getName(), session, value, workers, ds3Common));
                } else {
                    final List<DetailedS3Object> detailedS3Objects;
                    try {
                        detailedS3Objects = getDetailedDs3Objects(bucket.getName());
                        final List<Ds3TreeTableValue> treeItems = new ArrayList<>();
                        detailedS3Objects.stream().forEach(itemObject -> {
                                    if (!itemObject.getType().equals(S3ObjectType.FOLDER)) {
                                        if (itemObject.getBlobs() != null) {
                                            final List<BulkObject> objects = itemObject.getBlobs().getObjects();
                                            if (!Guard.isNullOrEmpty(objects)) {
                                                //Check if object contains first element or not
                                                if (objects.stream().findFirst().isPresent()) {
                                                    treeItems.add(new Ds3TreeTableValue(bucket.getName(), itemObject.getName(),
                                                            Ds3TreeTableValue.Type.File, itemObject.getSize(),
                                                            DateFormat.formatDate(itemObject.getCreationDate()), itemObject.getOwner(), true,
                                                            getConfiguredHBox(objects)));
                                                }
                                            } else {
                                                treeItems.add(new Ds3TreeTableValue(bucket.getName(), itemObject.getName(), Ds3TreeTableValue.Type.File,
                                                        itemObject.getSize(), DateFormat.formatDate(itemObject.getCreationDate()),
                                                        itemObject.getOwner(), true, null));
                                            }
                                        } else {
                                            treeItems.add(new Ds3TreeTableValue(bucket.getName(), itemObject.getName(), Ds3TreeTableValue.Type.File,
                                                    itemObject.getSize(), DateFormat.formatDate(itemObject.getCreationDate()),
                                                    itemObject.getOwner(), true, null));
                                        }
                                    }
                                }
                        );
                        Platform.runLater(() -> {
                            if (null != ds3Common.getDeepStorageBrowserPresenter()) {
                                ds3Common.getDeepStorageBrowserPresenter().logText(
                                        StringBuilderUtil.searchInBucketMessage(bucket.getName(), list.size()).toString(), LogType.INFO);
                            }
                        });
                        treeItems.stream().forEach(item -> list.add(new Ds3TreeTableItem(item.getFullName(), session,
                                item, workers, ds3Common)));
                    } catch (final Exception e) {
                        LOG.error("Search failed", e);
                        Platform.runLater(() -> {
                            if (null != ds3Common.getDeepStorageBrowserPresenter()) {
                                ds3Common.getDeepStorageBrowserPresenter().logText(
                                        StringBuilderUtil.searchFailedMessage().toString() + e, LogType.ERROR);
                            }
                        });
                    }
                }
            });
            return list;
        } catch (final Exception e) {
            LOG.error("Search failed", e);
            Platform.runLater(() -> {
                if (null != ds3Common.getDeepStorageBrowserPresenter()) {
                    ds3Common.getDeepStorageBrowserPresenter().logText(StringBuilderUtil.searchFailedMessage().toString() + e, LogType.ERROR);
                }
            });
            return null;
        }
    }

    private HBox getConfiguredHBox(final List<BulkObject> objects) {
        final BulkObject bulkObject = objects.stream().findFirst().orElse(null);
        if (null == bulkObject) {
            return null;
        } else {
            return GetStorageLocations.addPlacementIconsandTooltip(bulkObject.getPhysicalPlacement(), bulkObject.getInCache());
        }
    }

    private List<DetailedS3Object> getDetailedDs3Objects(final String bucketName) throws Exception {
        final GetObjectsWithFullDetailsSpectraS3Request request = new GetObjectsWithFullDetailsSpectraS3Request()
                .withBucketId(bucketName).withName(StringConstants.PERCENT + searchText + StringConstants.PERCENT)
                .withIncludePhysicalPlacement(true);
        final GetObjectsWithFullDetailsSpectraS3Response responseFullDetails = session.getClient().getObjectsWithFullDetailsSpectraS3(request);
        final List<DetailedS3Object> detailedS3Objects = responseFullDetails.getDetailedS3ObjectListResult().getDetailedS3Objects();
        return detailedS3Objects;
    }
}