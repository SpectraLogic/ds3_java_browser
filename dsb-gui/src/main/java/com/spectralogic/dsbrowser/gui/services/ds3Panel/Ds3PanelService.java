package com.spectralogic.dsbrowser.gui.services.ds3Panel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.HeadObjectRequest;
import com.spectralogic.ds3client.commands.HeadObjectResponse;
import com.spectralogic.ds3client.commands.spectrads3.*;
import com.spectralogic.ds3client.models.Bucket;
import com.spectralogic.ds3client.models.ListBucketResult;
import com.spectralogic.ds3client.models.PhysicalPlacement;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketPopup;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketWithDataPoliciesModel;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderModel;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.metadata.Ds3Metadata;
import com.spectralogic.dsbrowser.gui.components.metadata.MetadataView;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.PhysicalPlacementPopup;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public final class Ds3PanelService {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PanelService.class);

    private static final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();

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

    public static void createBucketPrompt(final Ds3Common ds3Common, final Workers workers) {
        LOG.info("Create Bucket Prompt");
        final Session session = ds3Common.getCurrentSession();
        if (session != null) {
            ds3Common.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("fetchingDataPolicies"), LogType.INFO);
            final Task<CreateBucketWithDataPoliciesModel> getDataPolicies = new Task<CreateBucketWithDataPoliciesModel>() {

                @Override
                protected CreateBucketWithDataPoliciesModel call() throws Exception {
                    final Ds3Client client = session.getClient();
                    final ImmutableList<CreateBucketModel> buckets = client.getDataPoliciesSpectraS3(new GetDataPoliciesSpectraS3Request()).getDataPolicyListResult().
                            getDataPolicies().stream().map(bucket -> new CreateBucketModel(bucket.getName(), bucket.getId())).collect(GuavaCollectors.immutableList());
                    final ImmutableList<CreateBucketWithDataPoliciesModel> dataPoliciesList = buckets.stream().map(policies ->
                            new CreateBucketWithDataPoliciesModel(buckets, session, workers)).collect(GuavaCollectors.immutableList());
                    Platform.runLater(() -> ds3Common.getDeepStorageBrowserPresenter().logText(resourceBundle.getString
                            ("dataPolicyRetrieved"), LogType.SUCCESS));
                    return dataPoliciesList.stream().findFirst().orElse(null);
                }
            };
            workers.execute(getDataPolicies);
            getDataPolicies.setOnSucceeded(taskEvent -> Platform.runLater(() -> {
                LOG.info("Launching create bucket popup {}", getDataPolicies.getValue().getDataPolicies().size());
                CreateBucketPopup.show(getDataPolicies.getValue(), ds3Common.getDeepStorageBrowserPresenter());
                RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers);
            }));

        } else {
            Ds3Alert.show(null, resourceBundle.getString("invalidSession"), Alert.AlertType.ERROR);
        }

    }

    @SuppressWarnings("unchecked")
    public static void createFolderPrompt(final Ds3Common ds3Common) {
        ImmutableList<TreeItem<Ds3TreeTableValue>> values = (ImmutableList<TreeItem<Ds3TreeTableValue>>) ds3Common.getDs3TreeTableView().getSelectionModel().getSelectedItems()
                .stream().collect(GuavaCollectors.immutableList());
        final TreeItem<Ds3TreeTableValue> root = ds3Common.getDs3TreeTableView().getRoot();
        if (values.isEmpty()) {
            ds3Common.getDeepStorageBrowserPresenter().logText("Select bucket/folder where you want to create an empty folder.", LogType.ERROR);
            Ds3Alert.show(null, "Location is not selected", Alert.AlertType.ERROR);
            return;
        } else if (values.isEmpty()) {
            final ImmutableList.Builder<TreeItem<Ds3TreeTableValue>> builder = ImmutableList.builder();
            values = builder.add(root).build().asList();
        }
        if (values.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
            LOG.error("You can not create folder here. Please refresh your view");
            Ds3Alert.show(null, "You can not create folder here. Please refresh your view", Alert.AlertType.ERROR);
            return;
        }
        if (values.size() > 1) {
            LOG.error("Only a single location can be selected to create empty folder");
            Ds3Alert.show(null, "Only a single location can be selected to create empty folder", Alert.AlertType.ERROR);
            return;
        }
        final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = values.stream().findFirst().orElse(null);
        if (ds3TreeTableValueTreeItem != null) {
            //Can not assign final as assigning value again in next step
            final String location = ds3TreeTableValueTreeItem.getValue().getFullName();
            final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());
            CreateFolderPopup.show(new CreateFolderModel(ds3Common.getCurrentSession().getClient(), location, buckets.stream().findFirst().orElse(null)), ds3Common.getDeepStorageBrowserPresenter());
            refresh(ds3TreeTableValueTreeItem);
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
        if (tempValues.isEmpty()) {
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
            PhysicalPlacementPopup.show(getPhysicalPlacement.getValue());
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

}
