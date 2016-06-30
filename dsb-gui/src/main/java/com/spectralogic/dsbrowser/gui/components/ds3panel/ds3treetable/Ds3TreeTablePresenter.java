package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import java.io.IOException;
import java.net.URL;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.DeleteObjectsRequest;
import com.spectralogic.ds3client.commands.GetServiceRequest;
import com.spectralogic.ds3client.commands.GetServiceResponse;
import com.spectralogic.ds3client.commands.HeadObjectRequest;
import com.spectralogic.ds3client.commands.HeadObjectResponse;
import com.spectralogic.ds3client.commands.spectrads3.DeleteBucketSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.DeleteFolderRecursivelySpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetDataPoliciesSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsSpectraS3Response;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketPopup;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketWithDataPoliciesModel;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.metadata.Ds3Metadata;
import com.spectralogic.dsbrowser.gui.components.metadata.MetadataPopup;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.Ds3PhysicalPlacement;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.PhysicalPlacementPopup;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.util.GuavaCollectors;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class Ds3TreeTablePresenter implements Initializable {
    private final static Logger LOG = LoggerFactory.getLogger(Ds3TreeTablePresenter.class);

    private ContextMenu contextMenu;

    @FXML
    TreeTableView<Ds3TreeTableValue> ds3TreeTable;

    @Inject
    Workers workers;

    @Inject
    JobWorkers jobWorkers;

    @Inject
    Session session;

    @Inject
    ResourceBundle resourceBundle;

    private MenuItem createBucket, physicalPlacement, metaData, deleteFile, deleteFolder, deleteBucket;

    private final Alert alert = new Alert(Alert.AlertType.INFORMATION);

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Loading Ds3TreeTablePresenter with session " + session.getSessionName());
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
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
        ds3TreeTable.setRowFactory(view -> {

            final TreeTableRow<Ds3TreeTableValue> row = new TreeTableRow<>();

            row.setOnContextMenuRequested(event -> {
                LOG.info("Context menu requested");
                // detect which deletes should be enabled
                final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = ds3TreeTable.getSelectionModel().getSelectedItems();
                if(selectedItems.size() == 1 && selectedItems.get(0).getValue().getType() == Ds3TreeTableValue.Type.BUCKET) {
                    deleteFile.setDisable(true);
                    deleteFolder.setDisable(true);
                    deleteBucket.setDisable(false);
                }
                else if (selectedItems.size() == 1 && selectedItems.get(0).getValue().getType() == Ds3TreeTableValue.Type.DIRECTORY) {
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
                if (!selectedItems.isEmpty()) {
                    LOG.info("Starting drag and drop event");
                    final Dragboard db = ds3TreeTable.startDragAndDrop(TransferMode.COPY);
                    final ClipboardContent content = new ClipboardContent();
                    content.putFilesByPath(selectedItems.stream().map(i -> i.getValue().getName()).collect(GuavaCollectors.immutableList()));
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

                if(treeItem != null) {
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

                    final Ds3PutJob putJob = new Ds3PutJob(session.getClient(), db.getFiles(), bucket, targetDir);

                    putJob.setOnSucceeded(e -> {
                        LOG.info("job completed successfully");
                        refresh(row.getTreeItem());
                    });

                    jobWorkers.execute(putJob);
                }
                event.consume();
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
            alert.setContentText("Nothing selected !!");
            alert.showAndWait();
            return;
        }

        if (values.size() > 1) {
            LOG.error("Only a single object can be selected to view physical placement ");
            alert.setContentText("Only a single object can be selected to view physical placement");
            alert.showAndWait();
            return;
        }

        final Task<Ds3PhysicalPlacement> getPhysicalPlacement = new Task<Ds3PhysicalPlacement>() {
            @Override
            protected Ds3PhysicalPlacement call() throws Exception {

                final Ds3Client client = session.getClient();
                final Ds3TreeTableValue value = values.get(0).getValue();

                List<Ds3Object> list = values.stream().map(item -> new Ds3Object(item.getValue().getName(), 12))
                        .collect(Collectors.toList());

                GetPhysicalPlacementForObjectsSpectraS3Response response = client
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

	public void showMetadata() {
        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());
        if (values.isEmpty()) {
        	// TODO display an error
            LOG.error("No files selected");
            alert.setContentText("No files selected !!");
            alert.showAndWait();
            return;
        }

        if (values.size() > 1) {
        	// TODO display an error
            LOG.error("Only a single object can be selected to view metadata ");
            alert.setContentText("Only a single object can be selected to view metadata ");
            alert.showAndWait();
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
        getMetadata.setOnSucceeded(event -> Platform.runLater( () -> {
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

        if (value.getType() != Ds3TreeTableValue.Type.DIRECTORY) {
            LOG.error("You can only delete a folder with this command");
            // TODO display an error
            return;
        }

        final Ds3Task task = new Ds3Task(session.getClient()) {
            @Override
            protected Object call() throws Exception {
                try {
                    getClient().deleteFolderRecursivelySpectraS3(new DeleteFolderRecursivelySpectraS3Request(value.getBucketName(), value.getFullName()));
                } catch (final IOException | SignatureException e) {
                    LOG.error("Failed to delete files" + e);
                }
                return null;
            }
        };
        DeleteFilesPopup.show(task);
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

        if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.DIRECTORY)) {
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
                    getClient().deleteObjects(new DeleteObjectsRequest(buckets.get(0), filesToDelete.stream().map(Ds3TreeTableValue::getFullName).collect(Collectors.toList())));
                } catch (final IOException | SignatureException e) {
                    LOG.error("Failed to delete files" + e);
                }
                return null;
            }
        };
        DeleteFilesPopup.show(task);
        values.stream().forEach(file -> refresh(file.getParent()));
    }

    private void deleteBucketPrompt() {
        LOG.info("Got delete bucket event");

        final ImmutableList<TreeItem<Ds3TreeTableValue>> buckets = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());

        if (buckets.size() > 1) {
        	// TODO display an error
            LOG.error("The user selected objects from multiple buckets.  This is not allowed.");
            alert.setContentText("The user selected objects from multiple buckets.  This is not allowed.");
            alert.showAndWait();
            return;
        }

        final Ds3TreeTableValue value = buckets.get(0).getValue();

        if (value.getType() != Ds3TreeTableValue.Type.BUCKET) {
            LOG.error("You can only delete a bucket with this command");
            // TODO display an error
            alert.setContentText("You can only delete a bucket with this command");
            alert.showAndWait();
            return;
        }

        final Ds3Task task = new Ds3Task(session.getClient()) {
            @Override
            protected Object call() throws Exception {
                try {
                	getClient().deleteBucketSpectraS3(new DeleteBucketSpectraS3Request(value.getBucketName()));
                } catch (final IOException | SignatureException e) {
                    LOG.error("Failed to delete Bucket" + e);
                    alert.setContentText("Failed to delete a bucket");
                    alert.showAndWait();
                }
                return null;
            }
        };
        DeleteFilesPopup.show(task);
        refreshTreeTableView(ds3TreeTable, workers, session);

   }

    private void createBucketPrompt() {
        LOG.info("Create Bucket Prompt");

        final Task<CreateBucketWithDataPoliciesModel> getDataPolicies = new Task<CreateBucketWithDataPoliciesModel>() {

            @Override
            protected CreateBucketWithDataPoliciesModel call() throws Exception {
                final Ds3Client client = session.getClient();
                final ImmutableList<CreateBucketModel> buckets = client.getDataPoliciesSpectraS3(new GetDataPoliciesSpectraS3Request()).getDataPolicyListResult().
                        getDataPolicies().stream().map(bucket -> new CreateBucketModel(bucket.getName())).collect(GuavaCollectors.immutableList());
                final ImmutableList<CreateBucketWithDataPoliciesModel> dataPoliciesList=buckets.stream().map(policies ->
                        new CreateBucketWithDataPoliciesModel(buckets,session,workers)).collect(GuavaCollectors.immutableList());
                return dataPoliciesList.get(0);
            }
        };

        workers.execute(getDataPolicies);

        getDataPolicies.setOnSucceeded(event -> Platform.runLater(() -> {
            LOG.info("Launching create bucket popup"+getDataPolicies.getValue().getDataPolicies().size());
            CreateBucketPopup.show(getDataPolicies.getValue());
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

    public void refreshTreeTableView(TreeTableView<Ds3TreeTableValue> ds3TreeTableView, Workers workers, Session session) {
        LOG.info("Running refresh of row");
        final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        ds3TreeTableView.setShowRoot(false);

        final GetServiceTask getServiceTask = new GetServiceTask(rootTreeItem.getChildren());
        ds3TreeTableView.setRoot(rootTreeItem);
        this.workers=workers;
        this.session=session;
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
                    .map(bucket -> new Ds3TreeTableValue(bucket.getName(), bucket.getName(), Ds3TreeTableValue.Type.BUCKET, 0, bucket.getCreationDate().toString()))
                    .collect(GuavaCollectors.immutableList());

            Platform.runLater(() -> {
                final ImmutableList<Ds3TreeTableItem> treeItems = buckets.stream().map(value -> new Ds3TreeTableItem(value.getName(), session, value, workers)).collect(GuavaCollectors.immutableList());
                partialResults.get().addAll(treeItems);
            });

            return this.partialResults.get();
        }
    }
}
