package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.*;
import com.spectralogic.ds3client.commands.spectrads3.*;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketPopup;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketWithDataPoliciesModel;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.metadata.Ds3Metadata;
import com.spectralogic.dsbrowser.gui.components.metadata.MetadataPopup;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.Ds3PhysicalPlacement;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.PhysicalPlacementPopup;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Ds3TreeTablePresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3TreeTablePresenter.class);

    private final Alert ALERT = new Alert(Alert.AlertType.INFORMATION);

    @Inject
    protected DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    private ContextMenu contextMenu;

    @FXML
    public TreeTableView<Ds3TreeTableValue> ds3TreeTable;

    @Inject
    private Workers workers;

    @Inject
    private JobWorkers jobWorkers;

    @Inject
    private Session session;

    @Inject
    private ResourceBundle resourceBundle;

    @Inject
    private Ds3PanelPresenter ds3PanelPresenter;

    @Inject
    private DataFormat dataFormat;

    private MenuItem createBucket, physicalPlacement, metaData, deleteFile, deleteFolder, deleteBucket;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            deepStorageBrowserPresenter.logText("Loading Session " + session.getSessionName(), LogType.INFO);
            ALERT.setTitle("Information Dialog");
            ALERT.setHeaderText(null);
            initContextMenu();
            initTreeTableView();
        } catch (final Throwable e) {
            LOG.error("Encountered error when creating Ds3TreeTablePresenter", e);
            throw e;
        }
    }

    private void initContextMenu() {
        contextMenu = new ContextMenu();
        deleteFile = new MenuItem(resourceBundle.getString("deleteFileContextMenu"));
        deleteFile.setOnAction(event -> deletePrompt());

        deleteFolder = new MenuItem(resourceBundle.getString("deleteFolderContextMenu"));
        deleteFolder.setOnAction(event -> deleteFolderPrompt());

        deleteBucket = new MenuItem(resourceBundle.getString("deleteBucketContextMenu"));
        deleteBucket.setOnAction(event -> deleteBucketPrompt());

        metaData = new MenuItem(resourceBundle.getString("metaDataContextMenu"));
        metaData.setOnAction(event -> showMetadata());

        physicalPlacement = new MenuItem(resourceBundle.getString("physicalPlacementContextMenu"));
        physicalPlacement.setOnAction(event -> showPhysicalPlacement());

        createBucket = new MenuItem(resourceBundle.getString("createBucketContextMenu"));
        createBucket.setOnAction(event -> createBucketPrompt());

        contextMenu.getItems().addAll(metaData, physicalPlacement, createBucket, new SeparatorMenuItem(), deleteFile, deleteFolder, deleteBucket);
    }

    private void initTreeTableView() {

        ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //noinspection unchecked
        ds3TreeTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue,
                                Object newValue) {
                //noinspection unchecked
                final TreeItem<Ds3TreeTableValue> selectedItem = (TreeItem<Ds3TreeTableValue>) newValue;

                if (selectedItem != null) {
                    String path = selectedItem.getValue().getFullName();
                    if (!selectedItem.getValue().getType().equals(Ds3TreeTableValue.Type.Bucket))
                        path = selectedItem.getValue().getBucketName() + "/" + path;
                    ds3PanelPresenter.ds3PathIndicator.setText(path);
                }
            }
        });

        ds3TreeTable.setRowFactory(view -> {

            final TreeTableRow<Ds3TreeTableValue> row = new TreeTableRow<>();

            row.setOnContextMenuRequested(event -> {
                LOG.info("Context menu requested");
                // detect which deletes should be enabled
                final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = ds3TreeTable.getSelectionModel().getSelectedItems();

                if (selectedItems.size() == 1 && selectedItems.get(0).getValue().getType() == Ds3TreeTableValue.Type.Bucket) {
                    deleteFile.setDisable(true);
                    deleteFolder.setDisable(true);
                    deleteBucket.setDisable(false);
                } else if (selectedItems.size() == 1 && selectedItems.get(0).getValue().getType() == Ds3TreeTableValue.Type.Directory) {
                    deleteFile.setDisable(true);
                    deleteFolder.setDisable(false);
                    deleteBucket.setDisable(true);
                } else {
                    deleteFile.setDisable(false);
                    deleteFolder.setDisable(true);
                    deleteBucket.setDisable(true);
                }

                if (selectedItems.size() >= 1) {
                    physicalPlacement.setDisable(false);
                } else {
                    physicalPlacement.setDisable(true);
                }

            });

            row.setOnDragDetected(event -> {
                LOG.info("Drag detected...");

                final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = ds3TreeTable.getSelectionModel().getSelectedItems();
                final List<Ds3TreeTableValue> selectedI = selectedItems.stream().map(v -> v.getValue()).collect(Collectors.toList());

                if (!selectedI.isEmpty()) {
                    LOG.info("Starting drag and drop event");
                    final Dragboard db = ds3TreeTable.startDragAndDrop(TransferMode.COPY);

                    final ClipboardContent content = new ClipboardContent();
                    content.put(dataFormat, selectedI);
                    content.putString(session.getSessionName() + "-" + session.getEndpoint());
                    content.putFilesByPath(selectedI.stream().map(i -> i.getName()).collect(GuavaCollectors.immutableList()));

                    db.setContent(content);
                }

                event.consume();

            });

            row.setOnDragOver(event -> {
                if (event.getGestureSource() != ds3TreeTable && event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY);
                }

                event.consume();
            });

            row.setOnDragEntered(event -> {
                final TreeItem<Ds3TreeTableValue> treeItem = row.getTreeItem();

                if (treeItem != null) {
                    if (!treeItem.isLeaf() && !treeItem.isExpanded()) {
                        LOG.info("Expanding closed row");
                        treeItem.setExpanded(true);
                    }

                    final InnerShadow is = new InnerShadow();
                    is.setOffsetY(1.0f);

                    row.setEffect(is);
                }
                event.consume();
            });

            row.setOnDragExited(event -> {
                row.setEffect(null);
                event.consume();
            });

            row.setOnDragDropped(event -> {
                LOG.info("Got drop event");
                final Dragboard db = event.getDragboard();

                if (db.hasFiles()) {
                    LOG.info("Drop event contains files");
                    // get bucket info and current path
                    final TreeItem<Ds3TreeTableValue> treeItem = row.getTreeItem();
                    final Ds3TreeTableValue value = treeItem.getValue();
                    final String bucket = value.getBucketName();
                    final String targetDir = value.getDirectoryName();
                    LOG.info("Passing new Ds3PutJob to jobWorkers thread pool to be scheduled");

                    final Ds3PutJob putJob = new Ds3PutJob(session.getClient(), db.getFiles(), bucket, targetDir, deepStorageBrowserPresenter);

                    putJob.setOnSucceeded(e -> {
                        LOG.info("job completed successfully");
                        refresh(row.getTreeItem());
                    });

                    jobWorkers.execute(putJob);
                }
                event.consume();
            });

            row.setOnMouseClicked(event -> {
                final TreeItem<Ds3TreeTableValue> selectedTreeItem = row.getTreeItem();
                if (selectedTreeItem.getValue().getType() == Ds3TreeTableValue.Type.Bucket || selectedTreeItem.getValue().getType() == Ds3TreeTableValue.Type.Directory) {
                    ds3TreeTable.getSelectionModel().clearAndSelect(row.getIndex());
                    event.consume();
                }
            });

            row.setContextMenu(contextMenu);

            return row;
        });


        final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        ds3TreeTable.setShowRoot(false);

        final GetServiceTask getServiceTask = new GetServiceTask(rootTreeItem.getChildren());
        ds3TreeTable.setRoot(rootTreeItem);
        workers.execute(getServiceTask);
    }

    private void showPhysicalPlacement() {

        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTable.getSelectionModel().getSelectedItems()
                .stream().collect(GuavaCollectors.immutableList());
        if (values.isEmpty()) {
            LOG.error("Nothing selected");
            ALERT.setContentText("Nothing selected !!");
            ALERT.showAndWait();
            return;
        }

        if (values.size() > 1) {
            LOG.error("Only a single object can be selected to view physical placement ");
            ALERT.setContentText("Only a single object can be selected to view physical placement");
            ALERT.showAndWait();
            return;
        }

        final Task<Ds3PhysicalPlacement> getPhysicalPlacement = new Task<Ds3PhysicalPlacement>() {
            @Override
            protected Ds3PhysicalPlacement call() throws Exception {

                final Ds3Client client = session.getClient();
                final Ds3TreeTableValue value = values.get(0).getValue();

                final List<Ds3Object> list = values.stream().map(item -> new Ds3Object(item.getValue().getName(), 12))
                        .collect(Collectors.toList());

                final GetPhysicalPlacementForObjectsSpectraS3Response response = client
                        .getPhysicalPlacementForObjectsSpectraS3(
                                new GetPhysicalPlacementForObjectsSpectraS3Request(value.getBucketName(), list));
                return new Ds3PhysicalPlacement(response.getPhysicalPlacementResult(),
                        response.getPhysicalPlacementResult().getTapes(),
                        response.getPhysicalPlacementResult().getPools());

            }
        };
        workers.execute(getPhysicalPlacement);
        getPhysicalPlacement.setOnSucceeded(event -> Platform.runLater(() -> {
            LOG.info("Launching PhysicalPlacement popup");
            PhysicalPlacementPopup.show(getPhysicalPlacement.getValue());
        }));
    }

    private void showMetadata() {
        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());
        if (values.isEmpty()) {
            LOG.error("No files selected");
            ALERT.setContentText("No files selected !!");
            ALERT.showAndWait();
            return;
        }

        if (values.size() > 1) {
            LOG.error("Only a single object can be selected to view metadata ");
            ALERT.setContentText("Only a single object can be selected to view metadata ");
            ALERT.showAndWait();
            return;
        }

        final Task<Ds3Metadata> getMetadata = new Task<Ds3Metadata>() {
            @Override
            protected Ds3Metadata call() throws Exception {
                final Ds3Client client = session.getClient();
                final Ds3TreeTableValue value = values.get(0).getValue();
                final HeadObjectResponse headObjectResponse = client.headObject(new HeadObjectRequest(value.getBucketName(), value.getFullName()));
                return new Ds3Metadata(headObjectResponse.getMetadata(), headObjectResponse.getObjectSize(), value.getFullName());
            }
        };
        workers.execute(getMetadata);
        getMetadata.setOnSucceeded(event -> Platform.runLater(() -> {
            LOG.info("Launching metadata popup");
            MetadataPopup.show(getMetadata.getValue());
        }));
    }

    private void deleteFolderPrompt() {
        LOG.info("Got delete folder event");

        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());
        if (values.isEmpty()) {
            LOG.error("No files selected");
            // TODO display an error
            return;
        }

        if (values.size() > 1) {
            LOG.error("You can only select a single folder to delete");
            // TODO display an error
            return;
        }

        final Ds3TreeTableValue value = values.get(0).getValue();

        if (value.getType() != Ds3TreeTableValue.Type.Directory) {
            LOG.error("You can only delete a folder with this command");
            // TODO display an error
            return;
        }

        final Ds3Task task = new Ds3Task(session.getClient()) {
            @Override
            protected Object call() throws Exception {
                try {
                    final DeleteFolderRecursivelySpectraS3Response deleteFolderRecursivelySpectraS3Response = getClient().deleteFolderRecursivelySpectraS3(new DeleteFolderRecursivelySpectraS3Request(value.getBucketName(), value.getFullName()));
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Delete response code: " + deleteFolderRecursivelySpectraS3Response.getStatusCode(), LogType.SUCCESS);
                        deepStorageBrowserPresenter.logText("Successfully deleted folder", LogType.SUCCESS);
                    });
                } catch (final IOException | SignatureException e) {
                    LOG.error("Failed to delete files" + e);
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Failed to delete folder", LogType.ERROR);
                    });
                    ALERT.setContentText("Failed to delete a folder");
                    ALERT.showAndWait();
                }
                return null;
            }
        };
        DeleteFilesPopup.show(task, null, this);
        values.stream().forEach(file -> refresh(file.getParent()));

    }

    public void deletePrompt() {
        LOG.info("Got delete event");

        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());
        if (values.isEmpty()) {
            LOG.error("No files selected");
            // TODO display an error
            return;
        }

        if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.Directory)) {
            LOG.error("You can only recursively delete a folder.  Please select the folder to delete, Right click, and select 'Delete Folder...'");
            // TODO display an error
            return;
        }

        final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());

        if (buckets.size() > 1) {
            LOG.error("The user selected objects from multiple buckets.  This is not allowed.");
            // TODO show error
            return;
        }

        final ArrayList<Ds3TreeTableValue> filesToDelete = new ArrayList<>(values
                .stream()
                .map(TreeItem::getValue)
                .collect(Collectors.toList())
        );

        final Ds3Task task = new Ds3Task(session.getClient()) {
            @Override
            protected Object call() throws Exception {
                try {
                    final DeleteObjectsResponse deleteObjectsResponse = getClient().deleteObjects(new DeleteObjectsRequest(buckets.get(0), filesToDelete.stream().map(Ds3TreeTableValue::getFullName).collect(Collectors.toList())));
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Delete response code: " + deleteObjectsResponse.getStatusCode(), LogType.SUCCESS);
                        deepStorageBrowserPresenter.logText("Successfully deleted file(s)", LogType.SUCCESS);
                    });
                } catch (final IOException | SignatureException e) {
                    LOG.error("Failed to delete files" + e);
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Failed to delete files", LogType.ERROR);
                    });
                    ALERT.setContentText("Failed to delete a files");
                    ALERT.showAndWait();
                }
                return null;
            }
        };
        DeleteFilesPopup.show(task, null, this);
        values.stream().forEach(file -> refresh(file.getParent()));
        ds3TreeTable.getSelectionModel().clearSelection();
    }

    private void deleteBucketPrompt() {
        LOG.info("Got delete bucket event");

        final ImmutableList<TreeItem<Ds3TreeTableValue>> buckets = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());

        if (buckets.size() > 1) {
            // TODO display an error
            LOG.error("The user selected objects from multiple buckets.  This is not allowed.");
            ALERT.setContentText("The user selected objects from multiple buckets.  This is not allowed.");
            ALERT.showAndWait();
            return;
        }

        final Ds3TreeTableValue value = buckets.get(0).getValue();

        if (value.getType() != Ds3TreeTableValue.Type.Bucket) {
            LOG.error("You can only delete a bucket with this command");
            // TODO display an error
            ALERT.setContentText("You can only delete a bucket with this command");
            ALERT.showAndWait();
            return;
        }

        final Ds3Task task = new Ds3Task(session.getClient()) {
            @Override
            protected Object call() throws Exception {
                try {
                    final DeleteBucketSpectraS3Response deleteBucketSpectraS3Response = getClient().deleteBucketSpectraS3(new DeleteBucketSpectraS3Request(value.getBucketName()).withForce(true));
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Delete response code: " + deleteBucketSpectraS3Response.getStatusCode(), LogType.SUCCESS);
                        deepStorageBrowserPresenter.logText("Successfully deleted bucket", LogType.SUCCESS);
                    });
                } catch (final IOException | SignatureException e) {
                    LOG.error("Failed to delete Bucket" + e);
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Failed to delete Bucket.", LogType.ERROR);
                    });
                    ALERT.setContentText("Failed to delete a bucket");
                    ALERT.showAndWait();
                }
                return null;
            }
        };
        DeleteFilesPopup.show(task, null, this);
        refreshTreeTableView(ds3TreeTable, workers, session);

    }

    private void createBucketPrompt() {
        LOG.info("Create Bucket Prompt");

        deepStorageBrowserPresenter.logText("Retrieving data policies..", LogType.SUCCESS);
        final Task<CreateBucketWithDataPoliciesModel> getDataPolicies = new Task<CreateBucketWithDataPoliciesModel>() {

            @Override
            protected CreateBucketWithDataPoliciesModel call() throws Exception {
                final Ds3Client client = session.getClient();
                final GetDataPoliciesSpectraS3Response response = client.getDataPoliciesSpectraS3(new GetDataPoliciesSpectraS3Request());

                final ImmutableList<CreateBucketModel> buckets = response.getDataPolicyListResult().
                        getDataPolicies().stream().map(bucket -> new CreateBucketModel(bucket.getName(), bucket.getId())).collect(GuavaCollectors.immutableList());
                final ImmutableList<CreateBucketWithDataPoliciesModel> dataPoliciesList = buckets.stream().map(policies ->
                        new CreateBucketWithDataPoliciesModel(buckets, session, workers)).collect(GuavaCollectors.immutableList());
                return dataPoliciesList.get(0);
            }
        };

        workers.execute(getDataPolicies);

        getDataPolicies.setOnSucceeded(event -> Platform.runLater(() -> {
            Ds3TreeTablePresenter.this.deepStorageBrowserPresenter.logText("Data policies retrieved", LogType.SUCCESS);
            LOG.info("Launching create bucket popup" + getDataPolicies.getValue().getDataPolicies().size());
            CreateBucketPopup.show(getDataPolicies.getValue(), deepStorageBrowserPresenter);
            refreshTreeTableView(ds3TreeTable, workers, session);
        }));

    }

    private void refresh(final TreeItem<Ds3TreeTableValue> modifiedTreeItem) {
        LOG.info("Running refresh of row");
        if (modifiedTreeItem instanceof Ds3TreeTableItem) {
            LOG.info("Refresh row");
            final Ds3TreeTableItem ds3TreeTableItem = (Ds3TreeTableItem) modifiedTreeItem;
            ds3TreeTableItem.refresh();
        }
    }

    public void refreshTreeTableView(final TreeTableView<Ds3TreeTableValue> ds3TreeTableView, final Workers workers, final Session session) {
        LOG.info("Running refresh of row");
        final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        ds3TreeTableView.setShowRoot(false);

        final GetServiceTask getServiceTask = new GetServiceTask(rootTreeItem.getChildren());
        ds3TreeTableView.setRoot(rootTreeItem);
        this.workers = workers;
        this.session = session;
        this.workers.execute(getServiceTask);
    }

    private class GetServiceTask extends Task<ObservableList<TreeItem<Ds3TreeTableValue>>> {

        private final ReadOnlyObjectWrapper<ObservableList<TreeItem<Ds3TreeTableValue>>> partialResults;

        public GetServiceTask(final ObservableList<TreeItem<Ds3TreeTableValue>> observableList) {

            partialResults = new ReadOnlyObjectWrapper<>(this, "partialResults", observableList);
        }

        public ObservableList<TreeItem<Ds3TreeTableValue>> getPartialResults() {
            return this.partialResults.get();
        }

        public ReadOnlyObjectProperty<ObservableList<TreeItem<Ds3TreeTableValue>>> partialResultsProperty() {
            return partialResults.getReadOnlyProperty();
        }

        @Override
        protected ObservableList<TreeItem<Ds3TreeTableValue>> call() throws Exception {
            final GetServiceResponse response = session.getClient().getService(new GetServiceRequest());

            final ImmutableList<Ds3TreeTableValue> buckets = response.getListAllMyBucketsResult()
                    .getBuckets().stream()
                    .map(bucket -> new Ds3TreeTableValue(bucket.getName(), bucket.getName(), Ds3TreeTableValue.Type.Bucket, FileSizeFormat.getFileSizeType(0), DateFormat.formatDate(bucket.getCreationDate())))
                    .collect(GuavaCollectors.immutableList());

            Platform.runLater(() -> {
                if (deepStorageBrowserPresenter != null)
                    deepStorageBrowserPresenter.logText("Received bucket list", LogType.SUCCESS);
                final ImmutableList<Ds3TreeTableItem> treeItems = buckets.stream().map(value -> new Ds3TreeTableItem(value.getName(), session, value, workers)).collect(GuavaCollectors.immutableList());
                partialResults.get().addAll(treeItems);
            });

            return this.partialResults.get();
        }
    }
}
