package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.*;
import com.spectralogic.ds3client.commands.spectrads3.*;
import com.spectralogic.ds3client.models.DeleteResult;
import com.spectralogic.ds3client.models.ListBucketResult;
import com.spectralogic.ds3client.models.PhysicalPlacement;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketPopup;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketWithDataPoliciesModel;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderModel;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderPopup;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.metadata.Ds3Metadata;
import com.spectralogic.dsbrowser.gui.components.metadata.MetadataPopup;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.PhysicalPlacementPopup;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class Ds3TreeTablePresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3TreeTablePresenter.class);
    private final Alert ALERT = new Alert(Alert.AlertType.INFORMATION);

    private ContextMenu contextMenu;

    @FXML
    public TreeTableView<Ds3TreeTableValue> ds3TreeTable;

    @FXML
    public TreeTableColumn<Ds3TreeTableValue, String> fileName;

    @FXML
    private TreeTableColumn<Ds3TreeTableValue, Number> sizeColumn;

    @FXML
    public TreeTableColumn<Ds3TreeTableValue, Ds3TreeTableValue.Type> fileType;

    @Inject
    protected DeepStorageBrowserPresenter deepStorageBrowserPresenter;

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

    @Inject
    private Ds3Common ds3Common;

    @Inject
    private SavedJobPrioritiesStore savedJobPrioritiesStore;

    @Inject
    private JobInterruptionStore jobInterruptionStore;

    @Inject
    private SettingsStore settingsStore;

    private IntegerProperty limit;

    private MenuItem physicalPlacement, deleteFile, deleteFolder, deleteBucket, metaData, createBucket, createFolder;

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

    private void checkInterruptedJob(final String endpoint) {
        final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints = jobInterruptionStore.getJobIdsModel().getEndpoints();
        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(endpoints, endpoint, deepStorageBrowserPresenter.getJobProgressView(), null);
        if (jobIDMap != null && jobIDMap.size() > 0) {
            deepStorageBrowserPresenter.getCount().setText("" + jobIDMap.size());
        } else {
            deepStorageBrowserPresenter.getCount().setText("");
            deepStorageBrowserPresenter.getJobButton().setDisable(true);
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

        physicalPlacement = new MenuItem(resourceBundle.getString("physicalPlacementContextMenu"));
        physicalPlacement.setOnAction(event -> showPhysicalPlacement());

        metaData = new MenuItem(resourceBundle.getString("metaDataContextMenu"));
        metaData.setOnAction(event -> showMetadata());

        createBucket = new MenuItem(resourceBundle.getString("createBucketContextMenu"));
        createBucket.setOnAction(event -> createBucketPrompt());

        createFolder = new MenuItem(resourceBundle.getString("createFolderContextMenu"));
        createFolder.setOnAction(event -> createFolderPrompt());

        contextMenu.getItems().addAll(metaData, physicalPlacement, deleteFile, deleteFolder, deleteBucket, new SeparatorMenuItem(), createBucket, createFolder);
    }

    public void deletePrompt() {
        LOG.info("Got delete event");

        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());
        if (values.isEmpty()) {
            LOG.error("No files selected");
            return;
        }

        if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.Directory)) {
            LOG.error("You can only recursively delete a folder. Please select the folder to delete, Right click, and select 'Delete Folder...'");
            return;
        }

       /* if (values.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
            LOG.error("You can not delete from here. Please go to specific location and delete object(s)");
            return;
        }*/

        final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());

       /* if (buckets.size() > 1) {
            LOG.error("The user selected objects from multiple buckets.  This is not allowed.");
            return;
        }*/

        final ArrayList<Ds3TreeTableValue> filesToDelete = new ArrayList<>(values
                .stream()
                .map(TreeItem::getValue)
                .collect(Collectors.toList())
        );

        final Map<String, List<Ds3TreeTableValue>> bucketObjectsMap = filesToDelete.stream().collect(Collectors.groupingBy(Ds3TreeTableValue::getBucketName));
        final Set<String> bukcets = bucketObjectsMap.keySet();


        final Ds3Task task = new Ds3Task(session.getClient()) {
            @Override
            protected Object call() throws Exception {
                int deleteSize = 0;
                try {
                    for(final String bucket : buckets) {
                        final DeleteObjectsResponse deleteObjectsResponse = getClient().deleteObjects(new DeleteObjectsRequest(bucket, bucketObjectsMap.get(bucket).stream().map(Ds3TreeTableValue::getFullName).collect(Collectors.toList())));
                        final DeleteResult deleteResult = deleteObjectsResponse.getDeleteResult();
                        deleteSize++;
                        if(deleteSize == bukcets.size())
                        {
                            Platform.runLater(() -> {
                                if (values.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
                                    ds3PanelPresenter.filterChanged(ds3PanelPresenter.getSearchedText());
                                }
                                // deepStorageBrmainowserPresenter.logText("Delete response code: " + deleteObjectsResponse.getStatusCode(), LogType.SUCCESS);
                                deepStorageBrowserPresenter.logText("Successfully deleted file(s)", LogType.SUCCESS);
                            });
                        }
                    }

                } catch (final IOException e) {
                    if(e instanceof FailedRequestException)
                    {
                        LOG.error("Failed to delete files" + e);
                        Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete files : "+((FailedRequestException)e).getError().getMessage(), LogType.ERROR));
                        ALERT.setContentText("Failed to delete a files");
                        ALERT.showAndWait();
                    }
                    else {
                        LOG.error("Failed to delete files" + e);
                        Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete files", LogType.ERROR));
                        ALERT.setContentText("Failed to delete a files");
                        ALERT.showAndWait();
                    }
                }

                return null;
            }
        };
        DeleteFilesPopup.show(task, null, this);
        values.stream().forEach(file -> refresh(file.getParent()));
        ds3TreeTable.getSelectionModel().clearSelection();
        ds3PanelPresenter.getDs3PathIndicator().setText("");
    }

    private void createFolderPrompt() {
        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTable.getSelectionModel().getSelectedItems()
                .stream().collect(GuavaCollectors.immutableList());

        if (values.isEmpty()) {
            deepStorageBrowserPresenter.logText("Select bucket/folder where you want to create an empty folder.", LogType.ERROR);
            ALERT.setContentText("Location is not selected");
            ALERT.showAndWait();
            return;
        }

        if (values.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
            LOG.error("You can not create folder here. Please refresh your view");
            ALERT.setContentText("You can not create folder here. Please refresh your view");
            ALERT.showAndWait();
            return;
        }

        if (values.size() > 1) {
            LOG.error("Only a single location can be selected to create empty folder");
            ALERT.setContentText("Only a single location can be selected to create empty folder");
            ALERT.showAndWait();
            return;
        }

        final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = values.stream().findFirst().orElse(null);
        if (ds3TreeTableValueTreeItem != null) {
            //Can not assign final as assigning value again in next step
            final String location = ds3TreeTableValueTreeItem.getValue().getFullName();
            final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());
            CreateFolderPopup.show(new CreateFolderModel(session.getClient(), location, buckets.stream().findFirst().orElse(null)), deepStorageBrowserPresenter);
            refresh(ds3TreeTableValueTreeItem);
        }
    }

    private void initTreeTableView() {

        ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ds3TreeTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(final ObservableValue observable, final Object oldValue,
                                final Object newValue) {
                //noinspection unchecked
                final TreeItem<Ds3TreeTableValue> selectedItem = (TreeItem<Ds3TreeTableValue>) newValue;

                if (selectedItem != null) {
                    //can not assign final
                    String path = selectedItem.getValue().getFullName();
                    if (selectedItem.getValue().getType().equals(Ds3TreeTableValue.Type.Loader)) {
                        // loadMore(selectedItem);
                    } else {
                        if (!selectedItem.getValue().getType().equals(Ds3TreeTableValue.Type.Bucket))
                            path = selectedItem.getValue().getBucketName() + "/" + path;
                        ds3PanelPresenter.getDs3PathIndicator().setText(path);
                        ds3PanelPresenter.getDs3PathIndicatorTooltip().setText(path);
                    }
                }
            }
        });
/*********Add new buckect on right click when there is no bucket present***************/
        ds3TreeTable.setOnContextMenuRequested(event -> {
            deleteBucket.setDisable(true);
            deleteFolder.setDisable(true);
            deleteFile.setDisable(true);
            physicalPlacement.setDisable(true);
            metaData.setDisable(true);
            createFolder.setDisable(true);
            createBucket.setDisable(false);
        });
        ds3TreeTable.setContextMenu(contextMenu);

        ds3TreeTable.setRowFactory(view -> {
            final TreeTableRow<Ds3TreeTableValue> row = new TreeTableRow<>();

            row.setOnContextMenuRequested(event ->
                    {
                        ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                        // detect which deletes should be enabled
                        final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = ds3TreeTable.getSelectionModel().getSelectedItems();

                        if (selectedItems.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
                            LOG.error("You can not delete from here. Please go to specific location and delete object(s)");
                            deleteFile.setDisable(false);
                            deleteFolder.setDisable(true);
                            deleteBucket.setDisable(true);
                        } else {
                            if (selectedItems.size() == 0) {
                                deleteBucket.setDisable(true);
                                deleteFolder.setDisable(true);
                                deleteFile.setDisable(true);
                                physicalPlacement.setDisable(true);
                                metaData.setDisable(true);
                                createFolder.setDisable(true);
                                createBucket.setDisable(false);
                            } else if (selectedItems.size() > 1) {
                                physicalPlacement.setDisable(true);
                                metaData.setDisable(false);
                                createFolder.setDisable(false);
                                createBucket.setDisable(true);
                                if (selectedItems.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.Directory)) {
                                    deleteFile.setDisable(true);
                                    metaData.setDisable(true);
                                    deleteFolder.setDisable(true);
                                    deleteBucket.setDisable(true);
                                } else if (selectedItems.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.Bucket)) {
                                    deleteFile.setDisable(true);
                                    metaData.setDisable(true);
                                    deleteFolder.setDisable(true);
                                    deleteBucket.setDisable(true);
                                } else {
                                    deleteFile.setDisable(false);
                                    deleteFolder.setDisable(true);
                                    deleteBucket.setDisable(true);
                                }
                            } else {
                                metaData.setDisable(false);
                                createFolder.setDisable(true);
                                createBucket.setDisable(true);
                                final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = selectedItems.stream().findFirst().orElse(null);
                                if (ds3TreeTableValueTreeItem.getValue().getType() == Ds3TreeTableValue.Type.Bucket) {
                                    deleteFile.setDisable(true);
                                    metaData.setDisable(true);
                                    deleteFolder.setDisable(true);
                                    deleteBucket.setDisable(false);
                                    physicalPlacement.setDisable(true);
                                    createFolder.setDisable(false);
                                } else if (ds3TreeTableValueTreeItem.getValue().getType() == Ds3TreeTableValue.Type.Directory) {
                                    deleteFile.setDisable(true);
                                    metaData.setDisable(true);
                                    deleteFolder.setDisable(false);
                                    deleteBucket.setDisable(true);
                                    createFolder.setDisable(false);
                                    physicalPlacement.setDisable(true);
                                } else {
                                    deleteFile.setDisable(false);
                                    deleteFolder.setDisable(true);
                                    deleteBucket.setDisable(true);
                                    physicalPlacement.setDisable(false);
                                }

                            }
                        }
                    }
            );


            row.setOnDragDetected(event ->
                    {
                        LOG.info("Drag detected...");
                        final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = ds3TreeTable.getSelectionModel().getSelectedItems();
                        final List<Ds3TreeTableValue> selectedI = selectedItems.stream().map(TreeItem::getValue).collect(Collectors.toList());
                        final List<Ds3TreeTableValueCustom> selected = selectedI.stream().map(v -> new Ds3TreeTableValueCustom(v.getBucketName(), v.getFullName(), v.getType(), v.getSize(), v.getLastModified(), v.getOwner(), v.isSearchOn())).collect(Collectors.toList());
                        if (!selectedI.isEmpty()) {
                            LOG.info("Starting drag and drop event");
                            final Dragboard db = ds3TreeTable.startDragAndDrop(TransferMode.COPY);
                            final ClipboardContent content = new ClipboardContent();
                            content.put(dataFormat, selected);
                            content.putString(session.getSessionName() + "-" + session.getEndpoint());
                            content.putFilesByPath(selected.stream().map(Ds3TreeTableValueCustom::getName).collect(GuavaCollectors.immutableList()));
                            db.setContent(content);
                        }
                        event.consume();
                    }
            );

            row.setOnDragOver(event ->
                    {
                        if (event.getGestureSource() != ds3TreeTable && event.getDragboard().hasFiles()) {
                            if (!row.getTreeItem().getValue().isSearchOn())
                                event.acceptTransferModes(TransferMode.COPY);
                            else
                                event.acceptTransferModes(TransferMode.NONE);
                        } else {
                            event.acceptTransferModes(TransferMode.NONE);
                        }
                        event.consume();
                    }
            );

            row.setOnDragEntered(event ->
                    {
                        final TreeItem<Ds3TreeTableValue> treeItem = row.getTreeItem();
                        if (treeItem != null) {
                            final InnerShadow is = new InnerShadow();
                            is.setOffsetY(1.0f);
                            row.setEffect(is);
                        }
                        event.consume();
                    }
            );

            row.setOnDragExited(event ->
                    {
                        row.setEffect(null);
                        event.consume();
                    }
            );

            row.setOnDragDropped(event ->
                    {
                        LOG.info("Got drop event");

                        if (row.getTreeItem() != null) {
                            if (!row.getTreeItem().isLeaf() && !row.getTreeItem().isExpanded()) {
                                LOG.info("Expanding closed row");
                                row.getTreeItem().setExpanded(true);
                            }
                        }

                        if (!row.getTreeItem().getValue().isSearchOn()) {

                            final Dragboard db = event.getDragboard();

                            if (db.hasFiles()) {
                                LOG.info("Drop event contains files");
                                // get bucket info and current path
                                final TreeItem<Ds3TreeTableValue> treeItem = row.getTreeItem();
                                final Ds3TreeTableValue value = treeItem.getValue();
                                final String bucket = value.getBucketName();
                                final String targetDir = value.getDirectoryName();
                                LOG.info("Passing new Ds3PutJob to jobWorkers thread pool to be scheduled");
                                final String priority = (!savedJobPrioritiesStore.getJobSettings().getPutJobPriority().equals(resourceBundle.getString("defaultPolicyText"))) ? savedJobPrioritiesStore.getJobSettings().getPutJobPriority() : null;
                                final Ds3PutJob putJob = new Ds3PutJob(session.getClient(), db.getFiles(), bucket, targetDir, deepStorageBrowserPresenter, priority, settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads(), jobInterruptionStore, ds3Common, settingsStore);

                                jobWorkers.execute(putJob);

                                putJob.setOnSucceeded(e -> {
                                    LOG.info("Succeed");

                                    try {
                                        refresh(treeItem);
                                        ds3TreeTable.getSelectionModel().clearSelection();
                                        ds3TreeTable.getSelectionModel().select(treeItem);
                                    } catch (final Exception ex) {
                                        LOG.info("Failed to save job ID");
                                    }

                                });

                                putJob.setOnFailed(e -> {
                                    LOG.info("setOnFailed");
                                    refresh(treeItem);
                                    ds3TreeTable.getSelectionModel().clearSelection();
                                    ds3TreeTable.getSelectionModel().select(treeItem);
                                    ParseJobInterruptionMap.refreshCompleteTreeTableView(ds3Common, workers);
                                });

                                putJob.setOnCancelled(e -> {
                                    LOG.info("setOnCancelled");
                                    if (putJob.getJobId() != null) {
                                        try {
                                            final CancelJobSpectraS3Response cancelJobSpectraS3Response = session.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(putJob.getJobId()));
                                          //  deepStorageBrowserPresenter.logText("PUT Job Cancelled. Response code:" + cancelJobSpectraS3Response.getResponse().getStatusCode(), LogType.SUCCESS);
                                            ParseJobInterruptionMap.removeJobID(jobInterruptionStore, putJob.getJobId().toString(), putJob.getClient().getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
                                        } catch (final IOException e1) {
                                            LOG.info("Failed to cancel job", LogType.ERROR);
                                        }
                                    }
                                    refresh(treeItem);
                                    ds3TreeTable.getSelectionModel().clearSelection();
                                    ds3TreeTable.getSelectionModel().select(treeItem);
                                });
                            }
                        } else {
                            ALERT.setContentText("Operation not allowed here");
                            ALERT.showAndWait();
                        }
                        event.consume();
                    }
            );

            final List<String> rowNameList = new ArrayList<>();
            row.setOnMouseClicked(event -> {
                if (event.isControlDown() || event.isShiftDown()) {
                    if (!rowNameList.contains(row.getTreeItem().getValue().getName())) {
                        rowNameList.add(row.getTreeItem().getValue().getName());
                        ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                        ds3TreeTable.getSelectionModel().select(row.getIndex());
                    } else {
                        ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                        ds3TreeTable.getSelectionModel().clearSelection(row.getIndex());
                        rowNameList.remove(row.getTreeItem().getValue().getName());
                    }
                } else if (event.getClickCount() == 2) {
                    ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                    ds3TreeTable.getSelectionModel().select(row.getIndex());
                    if (row.getTreeItem() != null) {
                        row.getTreeItem().setExpanded(true);
                    }
                } else if (event.getButton().name().equals("SECONDARY")) {
                    if (row.getTreeItem().getValue().getType().equals(Ds3TreeTableValue.Type.Loader)) {
                        loadMore(row.getTreeItem());
                    }
                } else {
                    ds3TreeTable.getSelectionModel().clearAndSelect(row.getIndex());
                    if (row.getTreeItem().getValue().getType().equals(Ds3TreeTableValue.Type.Loader)) {
                        loadMore(row.getTreeItem());
                    }
                }
            });

            row.setContextMenu(contextMenu);
            return row;
        });

        ds3TreeTable.sortPolicyProperty().
                set(

                        new Callback<TreeTableView<Ds3TreeTableValue>, Boolean>() {
                            @Override
                            public Boolean call(final TreeTableView<Ds3TreeTableValue> param) {

                                try {
                                    final Comparator<TreeItem<Ds3TreeTableValue>> comparator = new Comparator<TreeItem<Ds3TreeTableValue>>() {
                                        @Override
                                        public int compare(final TreeItem<Ds3TreeTableValue> o1, final TreeItem<Ds3TreeTableValue> o2) {
                                            if (param.getComparator() == null) {
                                                return 0;
                                            } else {
                                                return param.getComparator()
                                                        .compare(o1, o2);
                                            }
                                        }


                                    };
                                    if (ds3TreeTable.getRoot() != null) {
                                        final ImmutableList<TreeItem<Ds3TreeTableValue>> loaderList = ds3TreeTable.getRoot().getChildren().stream().filter(i -> (i.getValue().getType().toString().equals(Ds3TreeTableValue.Type.Loader.toString()))).collect(GuavaCollectors.immutableList());
                                        final ImmutableList<TreeItem<Ds3TreeTableValue>> collect = ds3TreeTable.getRoot().getChildren().stream().filter(i -> !(i.getValue().getType().toString().equals(Ds3TreeTableValue.Type.Loader.toString()))).collect(GuavaCollectors.immutableList());
                                        final ObservableList<TreeItem<Ds3TreeTableValue>> treeItems = FXCollections.observableArrayList(collect);

                                        FXCollections.sort(treeItems, comparator);

                                        if (!param.getSortOrder().stream().findFirst().get().getText().equals("Type")) {
                                            ds3TreeTable.getRoot().getChildren().removeAll(ds3TreeTable.getRoot().getChildren());
                                            ds3TreeTable.getRoot().getChildren().addAll(treeItems);

                                            if (loaderList.stream().findFirst().orElse(null) != null)
                                                ds3TreeTable.getRoot().getChildren().add(loaderList.stream().findFirst().get());
                                        }

                                        treeItems.forEach(i -> {
                                            if (i.isExpanded()) {
                                                if (param.getSortOrder().stream().findFirst().isPresent())
                                                    sortChild(i, comparator, param.getSortOrder().stream().findFirst().get().getText());
                                                else
                                                    sortChild(i, comparator, "");
                                            }
                                        });


                                        if (param.getSortOrder().stream().findFirst().isPresent()) {

                                            if (!param.getSortOrder().stream().findFirst().get().getText().equals("Type")) {
                                                FXCollections.sort(ds3TreeTable.getRoot().getChildren(), Comparator.comparing(t -> t.getValue().getType().toString()));
                                            }

                                        }
                                    }
                                } catch (final Exception e) {
                                    e.printStackTrace();

                                }

                                return true;
                            }

                            private void sortChild(final TreeItem<Ds3TreeTableValue> o1, final Comparator<TreeItem<Ds3TreeTableValue>> comparator, final String type) {
                                try {
                                    if (comparator != null) {


                                        final ImmutableList<TreeItem<Ds3TreeTableValue>> loaderList = o1.getChildren().stream().filter(i -> (i.getValue().getType().toString().equals(Ds3TreeTableValue.Type.Loader.toString()))).collect(GuavaCollectors.immutableList());

                                        final ImmutableList<TreeItem<Ds3TreeTableValue>> collect = o1.getChildren().stream().filter(i -> !(i.getValue().getType().toString().equals(Ds3TreeTableValue.Type.Loader.toString()))).collect(GuavaCollectors.immutableList());
                                        final ObservableList<TreeItem<Ds3TreeTableValue>> treeItems = FXCollections.observableArrayList(collect);

                                        treeItems.forEach(i -> {
                                            if (i.isExpanded())
                                                sortChild(i, comparator, type);
                                        });
                                        FXCollections.sort(treeItems, comparator);

                                        o1.getChildren().removeAll(o1.getChildren());
                                        o1.getChildren().addAll(treeItems);
                                        if (loaderList.stream().findFirst().orElse(null) != null)
                                            o1.getChildren().add(loaderList.stream().findFirst().get());

                                        if (!type.equals("Type")) {
                                            FXCollections.sort(o1.getChildren(), Comparator.comparing(t -> t.getValue().getType().toString()));
                                        }


                                    }
                                } catch (final Exception e) {
                                    LOG.info("Unable to sort", e.toString());
                                }
                            }
                        }
                );

        final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        ds3TreeTable.setShowRoot(false);
        ds3TreeTable.setPlaceholder(new Label("No Bucket found"));

        final Node oldPlaceHolder = ds3TreeTable.getPlaceholder();

        final ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(90, 90);
        ds3TreeTable.setPlaceholder(new StackPane(progress));

        final GetServiceTask getServiceTask = new GetServiceTask(rootTreeItem.getChildren());

        workers.execute(getServiceTask);
        ds3TreeTable.setRoot(rootTreeItem);

        progress.progressProperty().bind(getServiceTask.progressProperty());

        getServiceTask.setOnSucceeded(event -> {
            ds3TreeTable.setPlaceholder(oldPlaceHolder);

            final ObservableList<TreeItem<Ds3TreeTableValue>> children = ds3TreeTable.getRoot().getChildren();

            children.stream().forEach(i -> i.expandedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue) {
                    final BooleanProperty bb = (BooleanProperty) observable;
                    final TreeItem<Ds3TreeTableValue> bean = (TreeItem<Ds3TreeTableValue>) bb.getBean();
                    ds3Common.getExpandedNodesInfo().put(session.getSessionName() + "-" + session.getEndpoint(), bean);
                }
            }));


            fileName.setCellFactory(c -> new TreeTableCell<Ds3TreeTableValue, String>() {

                @Override
                protected void updateItem(final String item, final boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty && item.equals(resourceBundle.getString("addMoreButton")))
                        setStyle("-fx-font-weight: bold");
                    else if (!empty)
                        setStyle("-fx-font-weight: normal");
                    setText(item);
                }

            });


            sizeColumn.setCellFactory(c -> new TreeTableCell<Ds3TreeTableValue, Number>() {

                @Override
                protected void updateItem(final Number item, final boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(FileSizeFormat.getFileSizeType(item.longValue()));
                    }
                }

            });


            fileType.setCellFactory(c -> new TreeTableCell<Ds3TreeTableValue, Ds3TreeTableValue.Type>() {

                @Override
                protected void updateItem(final Ds3TreeTableValue.Type item, final boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        if (item.toString().equals("Loader"))
                            setText("");
                        else
                            setText(item.toString());
                    }
                }

            });

            checkInterruptedJob(session.getEndpoint() + ":" + session.getPortNo());
        });
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

        final Task<PhysicalPlacement> getPhysicalPlacement = new Task<PhysicalPlacement>() {
            @Override
            protected PhysicalPlacement call() throws Exception {
                final Ds3Client client = session.getClient();
                final Ds3TreeTableValue value = values.get(0).getValue();

                final List<Ds3Object> list = values.stream().map(item -> new Ds3Object(item.getValue().getFullName(), item.getValue().getSize()))
                        .collect(Collectors.toList());

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
            return;
        }

        if (values.size() > 1) {
            LOG.error("You can only select a single folder to delete");
            return;
        }

        final Ds3TreeTableValue value = values.get(0).getValue();

        if (value.getType() != Ds3TreeTableValue.Type.Directory) {
            LOG.error("You can only delete a folder with this command");
            return;
        }

        final Ds3Task task = new Ds3Task(session.getClient()) {
            @Override
            protected Object call() throws Exception {
                try {
                    final DeleteFolderRecursivelySpectraS3Response deleteFolderRecursivelySpectraS3Response = getClient().deleteFolderRecursivelySpectraS3(new DeleteFolderRecursivelySpectraS3Request(value.getBucketName(), value.getFullName()));
                    Platform.runLater(() -> {
                      //  deepStorageBrowserPresenter.logText("Delete response code: " + deleteFolderRecursivelySpectraS3Response.getStatusCode(), LogType.SUCCESS);
                        deepStorageBrowserPresenter.logText("Successfully deleted folder", LogType.SUCCESS);
                    });
                } catch (final IOException e) {
                    if(e instanceof FailedRequestException)
                    {
                        LOG.error("Failed to delete folder" + e);
                        Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete folder : "+((FailedRequestException)e).getError().getMessage(), LogType.ERROR));
                        ALERT.setContentText("Failed to delete a folder");
                        ALERT.showAndWait();
                    }
                    else {

                        LOG.error("Failed to delete folder" + e);
                        Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete folder", LogType.ERROR));
                        ALERT.setContentText("Failed to delete a folder");
                        ALERT.showAndWait();
                    }
                }
                return null;
            }
        };
        DeleteFilesPopup.show(task, null, this);
        values.stream().forEach(file -> refresh(file.getParent()));
        Platform.runLater(() -> {
            ds3TreeTable.getSelectionModel().clearSelection();
            ds3PanelPresenter.getDs3PathIndicator().setText("");
        });

    }


    private void deleteBucketPrompt() {
        LOG.info("Got delete bucket event");

        final ImmutableList<TreeItem<Ds3TreeTableValue>> buckets = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());

        if (buckets.size() > 1) {
            LOG.error("The user selected objects from multiple buckets.  This is not allowed.");
            ALERT.setContentText("The user selected multiple buckets.  This is not allowed.");
            ALERT.showAndWait();
            return;
        }

        final TreeItem<Ds3TreeTableValue> value = buckets.stream().findFirst().orElse(null);

        if (value != null) {
            if (value.getValue().getType() != Ds3TreeTableValue.Type.Bucket) {
                LOG.error("You can only delete a bucket with this command");
                ALERT.setContentText("You can only delete a bucket with this command");
                ALERT.showAndWait();
                return;
            }
            if (!checkIfBucketEmpty(value.getValue().getBucketName())) {
                Platform.runLater(() -> {
                    deepStorageBrowserPresenter.logText("Failed to delete Bucket as it contains files/folders", LogType.ERROR);
                    ALERT.setContentText("You can not delete bucket as it contains files/folders");
                    ALERT.showAndWait();
                });
            } else {

                final Ds3Task task = new Ds3Task(session.getClient()) {
                    @Override
                    protected Object call() throws Exception {
                        try {
                            final DeleteBucketSpectraS3Response deleteBucketSpectraS3Response = getClient().deleteBucketSpectraS3(new DeleteBucketSpectraS3Request(value.getValue().getBucketName()).withForce(true));
                            Platform.runLater(() -> {
                                // deepStorageBrowserPresenter.logText("Delete response code: " + deleteBucketSpectraS3Response.getStatusCode(), LogType.SUCCESS);
                                deepStorageBrowserPresenter.logText("Successfully deleted bucket", LogType.SUCCESS);
                            });

                        } catch (final IOException e) {
                            if(e instanceof FailedRequestException)
                            {
                                LOG.error("Failed to delete Buckets" + e);
                                Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete Bucket : "+((FailedRequestException)e).getError().getMessage(), LogType.ERROR));
                                ALERT.setContentText("Failed to delete bucket");
                                ALERT.showAndWait();
                            }
                            else {

                                LOG.error("Failed to delete Bucket" + e);
                                Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete Bucket.", LogType.ERROR));
                                ALERT.setContentText("Failed to delete a bucket");
                                ALERT.showAndWait();
                            }
                        }
                        return null;
                    }
                };
                DeleteFilesPopup.show(task, null, this);

                refreshTreeTableView();

                ds3PanelPresenter.getDs3PathIndicator().setText("");
            }
        }
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
                        getDataPolicies().stream().map(bucket -> new CreateBucketModel(bucket.getName().trim(), bucket.getId())).collect(GuavaCollectors.immutableList());
                final ImmutableList<CreateBucketWithDataPoliciesModel> dataPoliciesList = buckets.stream().map(policies ->
                        new CreateBucketWithDataPoliciesModel(buckets, session, workers)).collect(GuavaCollectors.immutableList());
                return dataPoliciesList.stream().findFirst().orElse(null);
            }
        };

        workers.execute(getDataPolicies);

        getDataPolicies.setOnSucceeded(event -> Platform.runLater(() -> {
            Ds3TreeTablePresenter.this.deepStorageBrowserPresenter.logText("Data policies retrieved", LogType.SUCCESS);
            LOG.info("Launching create bucket popup" + getDataPolicies.getValue().getDataPolicies().size());
            CreateBucketPopup.show(getDataPolicies.getValue(), deepStorageBrowserPresenter);
            refreshTreeTableView();
        }));

    }

    private void refresh(final TreeItem<Ds3TreeTableValue> modifiedTreeItem) {
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

    /**
     * @param modifiedTreeItem click to add more button as tree item
     *                         <p>
     *                         call load more item of treeitem class from click to add more button reference
     */

    private void loadMore(final TreeItem<Ds3TreeTableValue> modifiedTreeItem) {
        LOG.info("Running refresh of row");
        if (modifiedTreeItem instanceof Ds3TreeTableItem) {
            LOG.info("Refresh row");
            final Ds3TreeTableItem ds3TreeTableItem = (Ds3TreeTableItem) modifiedTreeItem;
            ds3TreeTableItem.loadMore(ds3TreeTable, ds3Common, deepStorageBrowserPresenter);
        }
    }

    public void refreshTreeTableView() {

        LOG.info("Running refresh of row");

        final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        ds3TreeTable.setShowRoot(false);

        final Node oldPlaceHolder = ds3TreeTable.getPlaceholder();

        final ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(90, 90);
        ds3TreeTable.setPlaceholder(new StackPane(progress));

        final GetServiceTask getServiceTask = new GetServiceTask(rootTreeItem.getChildren());
        ds3TreeTable.setRoot(rootTreeItem);

        workers.execute(getServiceTask);

        progress.progressProperty().bind(getServiceTask.progressProperty());

        getServiceTask.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                ds3TreeTable.setPlaceholder(oldPlaceHolder);
                if (ds3Common.getExpandedNodesInfo().containsKey(session.getSessionName() + "-" + session.getEndpoint())) {
                    final TreeItem<Ds3TreeTableValue> item = ds3Common.getExpandedNodesInfo().get(session.getSessionName() + "-" + session.getEndpoint());
                    if (ds3TreeTable.getRoot().getChildren().stream().filter(i -> i.getValue().getBucketName().equals(item.getValue().getBucketName())).findFirst().isPresent()) {
                        final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = ds3TreeTable.getRoot().getChildren().stream().filter(i -> i.getValue().getBucketName().equals(item.getValue().getBucketName())).findFirst().get();
                        ds3TreeTableValueTreeItem.setExpanded(false);
                        if (!ds3TreeTableValueTreeItem.isLeaf() && !ds3TreeTableValueTreeItem.isExpanded()) {
                            LOG.info("Expanding closed row");
                            ds3TreeTableValueTreeItem.setExpanded(true);
                        }
                    }
                }
            });
        });
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
            if (response.getListAllMyBucketsResult().getBuckets() != null) {
                final List<Ds3TreeTableValue> buckets = response.getListAllMyBucketsResult()
                        .getBuckets().stream()
                        .map(bucket -> {
                            final HBox hbox = new HBox();
                            hbox.getChildren().add(new Label("----"));
                            hbox.setAlignment(Pos.CENTER);
                            return new Ds3TreeTableValue(bucket.getName(), bucket.getName(), Ds3TreeTableValue.Type.Bucket, 0, DateFormat.formatDate(bucket.getCreationDate()), "--", false, hbox);
                        })
                        .collect(Collectors.toList());

                buckets.sort(Comparator.comparing(t -> t.getName().toLowerCase()));

                Platform.runLater(() -> {
                    if (deepStorageBrowserPresenter != null)
                        deepStorageBrowserPresenter.logText("Received bucket list", LogType.SUCCESS);
                    ds3PanelPresenter.disableSearch(false);
                    final ImmutableList<Ds3TreeTableItem> treeItems = buckets.stream().map(value -> new Ds3TreeTableItem(value.getName(), session, value, workers)).collect(GuavaCollectors.immutableList());
                    partialResults.get().addAll(treeItems);
                });
            } else {
                ds3PanelPresenter.disableSearch(true);
            }
            return this.partialResults.get();
        }
    }


    /**
     * check if bucket contains files or folders
     * @param bucketName
     * @return true if bucket is empty else return false
     */
    private boolean checkIfBucketEmpty(final String bucketName) {

        try {
            final GetBucketRequest request = new GetBucketRequest(bucketName).withDelimiter("/").withMaxKeys(1);
            final GetBucketResponse bucketResponse = session.getClient().getBucket(request);
            final ListBucketResult listBucketResult = bucketResponse.getListBucketResult();
            if(listBucketResult.getObjects().size() == 0 && listBucketResult.getCommonPrefixes().size() == 0) {
                return true;
            }
            else {
                return false;
            }

        }
        catch (final Exception e) {
            LOG.error("could not get bucket response" , e);
            return false;
        }

    }

}
