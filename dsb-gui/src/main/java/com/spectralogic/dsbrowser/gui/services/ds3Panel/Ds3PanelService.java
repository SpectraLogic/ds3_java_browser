package com.spectralogic.dsbrowser.gui.services.ds3Panel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.HeadObjectRequest;
import com.spectralogic.ds3client.commands.HeadObjectResponse;
import com.spectralogic.ds3client.commands.spectrads3.GetBucketsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetBucketsSpectraS3Response;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsSpectraS3Response;
import com.spectralogic.ds3client.models.Bucket;
import com.spectralogic.ds3client.models.ListBucketResult;
import com.spectralogic.ds3client.models.PhysicalPlacement;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.metadata.Ds3Metadata;
import com.spectralogic.dsbrowser.gui.components.metadata.MetadataView;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.PhysicalPlacementPopup;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.SearchJobTask;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public final class Ds3PanelService {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PanelService.class);

    private static final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();

    /**
     * check if bucket contains or folders
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

    public static void refresh(final TreeItem<Ds3TreeTableValue> modifiedTreeItem) {
        LOG.info("Running refresh of row");
        if (modifiedTreeItem instanceof Ds3TreeTableItem) {
            final Ds3TreeTableItem item;
            if (modifiedTreeItem.getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
                item = (Ds3TreeTableItem) modifiedTreeItem.getParent();
            } else {
                item = (Ds3TreeTableItem) modifiedTreeItem;
            }
            if (item.isExpanded()) {
                item.refresh();
            } else if (item.isAccessedChildren()) {
                item.setExpanded(true);
                item.refresh();
            } else {
                item.setExpanded(true);
            }
        }
    }

    public static void showPhysicalPlacement(final Ds3Common ds3Common, final Workers workers) {
        ImmutableList<TreeItem<Ds3TreeTableValue>> tempValues = ds3Common.getDs3TreeTableView().getSelectionModel().getSelectedItems()
                .stream().collect(GuavaCollectors.immutableList());
        final TreeItem<Ds3TreeTableValue> root = ds3Common.getDs3TreeTableView().getRoot();
        if (tempValues.isEmpty() && (root == null || root.getValue() != null)) {
            LOG.error("Nothing selected");
            Ds3Alert.show(null, "Nothing selected !!", Alert.AlertType.INFORMATION);
            return;
        } else if (tempValues.isEmpty()) {
            final ImmutableList.Builder<TreeItem<Ds3TreeTableValue>> builder = ImmutableList.builder();
            tempValues = builder.add(root).build().asList();

        }
        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = tempValues;
        if (values.size() > 1) {
            LOG.error("Only a single object can be selected to view physical placement ");
            Ds3Alert.show(null, "Only a single object can be selected to view physical placement", Alert.AlertType.INFORMATION);
            return;
        }

        final Task<PhysicalPlacement> getPhysicalPlacement = new Task<PhysicalPlacement>() {
            @Override
            protected PhysicalPlacement call() throws Exception {
                final Ds3Client client = ds3Common.getCurrentSession().getClient();
                final Ds3TreeTableValue value = values.get(0).getValue();
                List<Ds3Object> list = null;
                if (null != value && (value.getType().equals(Ds3TreeTableValue.Type.Bucket))) {
                } else if (value.getType().equals(Ds3TreeTableValue.Type.File)) {
                    list = values.stream().map(item -> new Ds3Object(item.getValue().getFullName(), item.getValue().getSize()))
                            .collect(Collectors.toList());
                } else if (null != value && value.getType().equals(Ds3TreeTableValue.Type.Directory)) {
                    final Ds3PanelService.GetDirectoryObjects getDirectoryObjects = new Ds3PanelService.GetDirectoryObjects(value.getBucketName(), value.getDirectoryName(), ds3Common);
                    workers.execute(getDirectoryObjects);
                    final ListBucketResult listBucketResult = getDirectoryObjects.getValue();
                    if (null != listBucketResult) {
                        list = listBucketResult.getObjects().stream().map(item -> new Ds3Object(item.getKey(), item.getSize()))
                                .collect(Collectors.toList());
                    }
                }
                final GetPhysicalPlacementForObjectsSpectraS3Response response = client
                        .getPhysicalPlacementForObjectsSpectraS3(
                                new GetPhysicalPlacementForObjectsSpectraS3Request(value.getBucketName(), list));
                return response.getPhysicalPlacementResult();
            }
        };
        workers.execute(getPhysicalPlacement);
        getPhysicalPlacement.setOnSucceeded(event -> Platform.runLater(() -> {
            LOG.info("Launching PhysicalPlacement popup");
            PhysicalPlacementPopup.show(getPhysicalPlacement.getValue(), resourceBundle);
        }));
    }

    @SuppressWarnings("unchecked")
    public static void showMetadata(final Ds3Common ds3Common, final Workers workers) {
        final TreeTableView ds3TreeTableView = ds3Common.getDs3TreeTableView();
        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = (ImmutableList<TreeItem<Ds3TreeTableValue>>) ds3TreeTableView.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());
        if (values.isEmpty()) {
            LOG.error("No files selected");
            Ds3Alert.show(null, "No files selected", Alert.AlertType.ERROR);
            return;
        }
        if (values.size() > 1) {
            LOG.error("Only a single object can be selected to view metadata ");
            Ds3Alert.show(null, "Only a single object can be selected to view metadata ", Alert.AlertType.INFORMATION);
            return;
        }
        final Task<Ds3Metadata> getMetadata = new Task<Ds3Metadata>() {
            @Override
            protected Ds3Metadata call() throws Exception {
                final Ds3Client client = ds3Common.getCurrentSession().getClient();
                final Ds3TreeTableValue value = values.get(0).getValue();
                final HeadObjectResponse headObjectResponse = client.headObject(new HeadObjectRequest(value.getBucketName(), value.getFullName()));
                return new Ds3Metadata(headObjectResponse.getMetadata(), headObjectResponse.getObjectSize(), value.getFullName(), value.getLastModified());
            }
        };
        workers.execute(getMetadata);
        getMetadata.setOnSucceeded(event -> Platform.runLater(() -> {
            LOG.info("Launching metadata popup");
            final MetadataView metadataView = new MetadataView(getMetadata.getValue());
            Popup.show(metadataView.getView(), resourceBundle.getString("metaDataContextMenu"));
        }));
    }

    /**
     * check if bucket contains files or folders
     *
     * @param bucketName
     * @param treeItem
     * @return true if bucket is empty else return false
     */
    public static boolean checkIfBucketEmpty(final String bucketName, final Session session, final TreeItem<Ds3TreeTableValue> treeItem) {
        try {
            final GetBucketRequest request = new GetBucketRequest(bucketName).withDelimiter("/").withMaxKeys(1);
            if (null != treeItem && !treeItem.getValue().getType().equals(Ds3TreeTableValue.Type.Bucket))
                request.withPrefix(treeItem.getValue().getFullName());
            final GetBucketResponse bucketResponse = session.getClient().getBucket(request);
            final ListBucketResult listBucketResult = bucketResponse.getListBucketResult();
            return (listBucketResult.getObjects().size() == 0 && listBucketResult.getCommonPrefixes().size() == 0)
                    || (listBucketResult.getObjects().size() == 1 && listBucketResult.getNextMarker() == null);


        } catch (final Exception e) {
            LOG.error("could not get bucket response", e);
            return false;
        }

    }

    public static void filterChanged(final Ds3Common ds3Common, final Workers workers) {
        final Ds3PanelPresenter ds3PanelPresenter = ds3Common.getDs3PanelPresenter();
        final String newValue = ds3PanelPresenter.getSearchedText();
        ds3PanelPresenter.getDs3PathIndicator().setText(resourceBundle.getString("searching"));
        ds3PanelPresenter.getDs3PathIndicatorTooltip().setText(resourceBundle.getString("searching"));
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = ds3Common.getDs3TreeTableView();
        final Session session = ds3Common.getCurrentSession();
        if (Guard.isStringNullOrEmpty(newValue)) {
            setVisibilityOfItemsInfo(true, ds3Common);
            RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers);
        } else {
            try {
                ObservableList<TreeItem<Ds3TreeTableValue>> selectedItem = ds3TreeTableView.getSelectionModel().getSelectedItems();
                final TreeItem<Ds3TreeTableValue> root = ds3TreeTableView.getRoot();
                if (Guard.isNullOrEmpty(selectedItem) && (root != null && root.getValue() != null)) {
                    selectedItem = FXCollections.observableArrayList();
                    selectedItem.add(root);
                }

                final List<Bucket> searchableBuckets = Ds3PanelService.setSearchableBucket(selectedItem, session,
                        ds3TreeTableView);
                final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
                rootTreeItem.setExpanded(true);
                ds3TreeTableView.setShowRoot(false);
                setVisibilityOfItemsInfo(false, ds3Common);

                final SearchJobTask searchJobTask = new SearchJobTask(searchableBuckets, newValue, session, workers, ds3Common);
                workers.execute(searchJobTask);
                searchJobTask.setOnSucceeded(event -> {
                    LOG.info("Search completed!");
                    Platform.runLater(() -> {
                        try {
                            final List<Ds3TreeTableItem> treeTableItems = searchJobTask.get();
                            ds3PanelPresenter.getDs3PathIndicator().setText(StringBuilderUtil.nObjectsFoundMessage(treeTableItems.size()).toString());
                            ds3PanelPresenter.getDs3PathIndicatorTooltip().setText(StringBuilderUtil.nObjectsFoundMessage(treeTableItems.size()).toString());
                            ds3Common.getDeepStorageBrowserPresenter().logText(
                                    StringBuilderUtil.nObjectsFoundMessage(treeTableItems.size()).toString(), LogType.INFO);
                            treeTableItems.sort(Comparator.comparing(t -> t.getValue().getType().toString()));
                            treeTableItems.forEach(value -> rootTreeItem.getChildren().add(value));
                            if (rootTreeItem.getChildren().size() == 0) {
                                ds3TreeTableView.setPlaceholder(new Label(resourceBundle.getString("0_SearchResult")));
                            }
                            ds3TreeTableView.setRoot(rootTreeItem);
                            final TreeTableColumn<Ds3TreeTableValue, ?> ds3TreeTableValueTreeTableColumn = ds3TreeTableView
                                    .getColumns().get(1);
                            if (null != ds3TreeTableValueTreeTableColumn) {
                                ds3TreeTableValueTreeTableColumn.setVisible(true);
                            }
                        } catch (final Exception e) {
                            LOG.error("Search failed", e);
                            ds3Common.getDeepStorageBrowserPresenter().logText(StringBuilderUtil.searchFailedMessage().toString() + e, LogType.ERROR);
                        }
                    });
                });
                searchJobTask.setOnCancelled(event -> LOG.info("Search cancelled"));
            } catch (final Exception e) {
                LOG.error("Could not complete search: {}", e);
            }
        }
    }

    private static void setVisibilityOfItemsInfo(final boolean visibility, final Ds3Common ds3Common) {
        ds3Common.getDs3PanelPresenter().getInfoLabel().setVisible(visibility);
        ds3Common.getDs3PanelPresenter().getCapacityLabel().setVisible(visibility);

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
