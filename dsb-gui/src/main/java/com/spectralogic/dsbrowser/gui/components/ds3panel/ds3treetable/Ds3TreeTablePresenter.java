package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.DeleteObjectsRequest;
import com.spectralogic.ds3client.commands.DeleteObjectsResponse;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.DeleteBucketSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.DeleteFolderRecursivelySpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.DeleteFolderRecursivelySpectraS3Response;
import com.spectralogic.ds3client.models.DeleteResult;
import com.spectralogic.ds3client.models.ListBucketResult;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.Ds3PanelService;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.services.tasks.GetServiceTask;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class Ds3TreeTablePresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3TreeTablePresenter.class);

    private final Alert ALERT = new Alert(Alert.AlertType.INFORMATION);

    private final List<String> rowNameList = new ArrayList<>();

    @FXML
    public TreeTableView<Ds3TreeTableValue> ds3TreeTable;

    @FXML
    public TreeTableColumn<Ds3TreeTableValue, String> fileName;

    @FXML
    public TreeTableColumn<Ds3TreeTableValue, Ds3TreeTableValue.Type> fileType;

    @FXML
    private TreeTableColumn<Ds3TreeTableValue, Number> sizeColumn;

    @FXML
    private TreeTableColumn fullPath;

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

    @Inject
    private DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    private ContextMenu contextMenu;

    private MenuItem physicalPlacement, deleteFile, deleteFolder, deleteBucket, metaData, createBucket, createFolder;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            ALERT.setTitle("Error");
            ALERT.setHeaderText(null);
            final Stage stage = (Stage) ALERT.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(ImageURLs.DEEP_STORAGE_BROWSER));
            ds3Common.getDeepStorageBrowserPresenter().logText("Loading Session " + session.getSessionName(), LogType.INFO);
            initContextMenu();
            initTreeTableView();
        } catch (final Throwable e) {
            LOG.error("Encountered error when creating Ds3TreeTablePresenter", e);
            throw e;
        }
    }

    private void checkInterruptedJob(final String endpoint) {
        final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints = jobInterruptionStore.getJobIdsModel().getEndpoints();
        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(endpoints, endpoint, ds3Common.getDeepStorageBrowserPresenter().getJobProgressView(), null);
        if (!Guard.isMapNullOrEmpty(jobIDMap)) {
            ds3Common.getDeepStorageBrowserPresenter().getLblCount().setText(String.valueOf(jobIDMap.size()));
        } else {
            ds3Common.getDeepStorageBrowserPresenter().getLblCount().setText(StringConstants.EMPTY_STRING);
            ds3Common.getDeepStorageBrowserPresenter().getJobButton().setDisable(true);
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
        physicalPlacement.setOnAction(event -> Ds3PanelService.showPhysicalPlacement(ds3Common, workers));

        metaData = new MenuItem(resourceBundle.getString("metaDataContextMenu"));
        metaData.setOnAction(event -> Ds3PanelService.showMetadata(ds3Common, workers));

        createBucket = new MenuItem(resourceBundle.getString("createBucketContextMenu"));
        createBucket.setOnAction(event -> Ds3PanelService.createBucketPrompt(ds3Common, workers));

        createFolder = new MenuItem(resourceBundle.getString("createFolderContextMenu"));
        createFolder.setOnAction(event -> Ds3PanelService.createFolderPrompt(ds3Common));

        contextMenu.getItems().addAll(metaData, physicalPlacement, new SeparatorMenuItem(), deleteFile, deleteFolder, deleteBucket, new SeparatorMenuItem(), createBucket, createFolder);
    }

    @SuppressWarnings("unchecked")
    private void initTreeTableView() {
        ds3Common.setDs3TreeTableView(ds3TreeTable);

        fullPath.setText(resourceBundle.getString("fullPath"));
        fileName.setText(resourceBundle.getString("fileName"));

        ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ds3TreeTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {

            @Override
            public void changed(final ObservableValue observable, final Object oldValue,
                                final Object newValue) {
                //noinspection unchecked
                TreeItem<Ds3TreeTableValue> selectedItem = (TreeItem<Ds3TreeTableValue>) newValue;

                //If selected item is null then setting up root as selected item
                if (selectedItem == null) {
                    selectedItem = ds3TreeTable.getRoot();
                }

                if (selectedItem.getValue() != null) {
                    String path = selectedItem.getValue().getFullName();
                    if (ds3TreeTable.getSelectionModel().getSelectedItems() != null && ds3TreeTable.getSelectionModel().getSelectedItems().size() > 1) {
                        path = StringConstants.EMPTY_STRING;
                    } else if (!selectedItem.getValue().getType().equals(Ds3TreeTableValue.Type.Bucket)) {
                        path = selectedItem.getValue().getBucketName() + StringConstants.FORWARD_SLASH + path;
                    }
                    ds3PanelPresenter.getDs3PathIndicator().setText(path);
                    ds3PanelPresenter.getDs3PathIndicatorTooltip().setText(path);
                    manageItemsCount(selectedItem);
                }
                final String info = StringBuilderUtil.setSelectedItemCount(ds3TreeTable.getExpandedItemCount(), ds3TreeTable.getSelectionModel().getSelectedItems().size());
                ds3PanelPresenter.getPaneItems().setVisible(true);
                ds3PanelPresenter.getPaneItems().setText(info);
            }

        });
        //Add new bucket on right click when there is no bucket present
        ds3TreeTable.setOnContextMenuRequested(event -> {
            disableContextMenu(true);
            createBucket.setDisable(false);
        });

        ds3TreeTable.setContextMenu(contextMenu);

        ds3TreeTable.setRowFactory(view -> {
            final TreeTableRow<Ds3TreeTableValue> row = new TreeTableRow<>();
            row.setOnContextMenuRequested(event -> setContextMenuBehaviour());
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
                                        LOG.error("Failed to save job ID", ex);
                                    }

                                });
                                putJob.setOnFailed(e -> {
                                    LOG.error("setOnFailed");
                                    refresh(treeItem);
                                    ds3TreeTable.getSelectionModel().clearSelection();
                                    ds3TreeTable.getSelectionModel().select(treeItem);
                                    RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers);
                                });
                                putJob.setOnCancelled(e -> {
                                    LOG.info("setOnCancelled");
                                    if (putJob.getJobId() != null) {
                                        try {
                                            session.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(putJob.getJobId()));
                                            //  deepStorageBrowserPresenter.logText("PUT Job Cancelled. Response code:" + cancelJobSpectraS3Response.getResponse().getStatusCode(), LogType.SUCCESS);
                                            ParseJobInterruptionMap.removeJobID(jobInterruptionStore, putJob.getJobId().toString(), putJob.getClient().getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
                                        } catch (final IOException e1) {
                                            LOG.error("Failed to cancel job", e1);
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
            row.setOnMouseClicked(event -> {
                if (event.isControlDown() || event.isShiftDown()) {
                    if (!rowNameList.contains(row.getTreeItem().getValue().getName())) {
                        rowNameList.add(row.getTreeItem().getValue().getName());
                        ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                        ds3TreeTable.getSelectionModel().select(row.getIndex());
                    } else {
                        ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                        final int selectedSize = ds3TreeTable.getSelectionModel().getSelectedItems().size();
                        ds3TreeTable.getSelectionModel().clearSelection(row.getIndex());
                        rowNameList.remove(row.getTreeItem().getValue().getName());
                        manageItemsCount(row.getTreeItem());
                    }
                } else if (event.getClickCount() == 2) {
                    final ProgressIndicator progress = new ProgressIndicator();
                    progress.setMaxSize(90, 90);
                    ds3TreeTable.setPlaceholder(new StackPane(progress));
                    ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                    ds3TreeTable.getSelectionModel().select(row.getIndex());
                    ds3Common.getDs3PanelPresenter().setDs3TreeTablePresenter(this);
                    ds3Common.getDs3PanelPresenter().setDs3TreeTableView(ds3TreeTable);
                    if (row.getTreeItem() != null && !row.getTreeItem().getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
                        if (checkIfBucketEmpty(row.getTreeItem().getValue().getBucketName(), row.getTreeItem()))
                            ds3TreeTable.setPlaceholder(null);
                        row.getTreeItem().setExpanded(true);
                        ds3TreeTable.setShowRoot(false);
                        ds3TreeTable.setRoot(row.getTreeItem());

                    }
                } else if (event.getButton().name().equals("SECONDARY")) {
                    try {
                        if (row.getTreeItem().getValue().getType().equals(Ds3TreeTableValue.Type.Loader)) {
                            loadMore(row.getTreeItem());
                        }
                    } catch (final Exception e) {
                        LOG.error("Unable to get value of selected item", e);
                    }
                } else {
                    try {
                        rowNameList.clear();
                        if (null == row.getTreeItem()) {
                            ds3TreeTable.getSelectionModel().clearSelection();
                        } else {
                            rowNameList.add(row.getTreeItem().getValue().getName());
                            ds3TreeTable.getSelectionModel().clearAndSelect(row.getIndex());
                            if (row.getTreeItem().getValue().getType().equals(Ds3TreeTableValue.Type.Loader)) {
                                loadMore(row.getTreeItem());
                            }
                        }
                        if (ds3TreeTable.getRoot().getParent() == null && ds3TreeTable.getSelectionModel().getSelectedItem() == null) {
                            ds3PanelPresenter.getDs3PathIndicator().setText("");
                            ds3PanelPresenter.getDs3PathIndicator().setTooltip(null);
                        } else {
                            ds3PanelPresenter.getDs3PathIndicator().setTooltip(ds3PanelPresenter.getDs3PathIndicatorTooltip());
                        }
                    } catch (final Exception e) {
                        LOG.error("Unable to get value of selected item", e);
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
                                    LOG.error("Unable to sort tree", e);
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
                                    LOG.error("Unable to sort", e);
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
        final GetServiceTask getServiceTask = new GetServiceTask(rootTreeItem.getChildren(), session, workers, ds3Common);
        workers.execute(getServiceTask);
        ds3TreeTable.setRoot(rootTreeItem);
        ds3TreeTable.expandedItemCountProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) {
                final String info = ds3TreeTable.getExpandedItemCount() + " item(s), " + ds3TreeTable.getSelectionModel().getSelectedItems().size() + " item(s) selected";
                ds3PanelPresenter.getPaneItems().setVisible(true);
                ds3PanelPresenter.getPaneItems().setText(info);
            }
        });
        progress.progressProperty().bind(getServiceTask.progressProperty());
        getServiceTask.setOnSucceeded(event -> {
            ds3TreeTable.setPlaceholder(oldPlaceHolder);
            final ObservableList<TreeItem<Ds3TreeTableValue>> children = ds3TreeTable.getRoot().getChildren();
            children.forEach(i -> i.expandedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue) {
                    final BooleanProperty bb = (BooleanProperty) observable;
                    final TreeItem<Ds3TreeTableValue> bean = (TreeItem<Ds3TreeTableValue>) bb.getBean();
                    ((Ds3TreeTableItem) bean).setDs3TreeTable(ds3TreeTable);
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
                        if (item.toString().equals("Loader")) {
                            setText("");
                        } else {
                            setText(item.toString());
                        }
                    }
                }

            });
            checkInterruptedJob(session.getEndpoint() + ":" + session.getPortNo());
        });
    }


    public void deletePrompt() {
        LOG.info("Got delete event");
        ImmutableList<TreeItem<Ds3TreeTableValue>> tempValues = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());
        final TreeItem<Ds3TreeTableValue> root = ds3TreeTable.getRoot();
        if (tempValues.isEmpty() && null == root) {
            LOG.error("No files selected");
            return;
        } else if (tempValues.isEmpty()) {
            final ImmutableList.Builder<TreeItem<Ds3TreeTableValue>> builder = ImmutableList.builder();
            tempValues = builder.add(root).build().asList();

        }
        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = tempValues;
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
        final TreeItem<Ds3TreeTableValue> selectedItem = ds3TreeTable.getSelectionModel().getSelectedItems().stream().findFirst().get().getParent();
        final Ds3Task task = new Ds3Task(session.getClient()) {
            @Override
            protected Object call() throws Exception {
                int deleteSize = 0;
                try {
                    for (final String bucket : buckets) {
                        final DeleteObjectsResponse deleteObjectsResponse = getClient().deleteObjects(new DeleteObjectsRequest(bucket, bucketObjectsMap.get(bucket).stream().map(Ds3TreeTableValue::getFullName).collect(Collectors.toList())));
                        final DeleteResult deleteResult = deleteObjectsResponse.getDeleteResult();
                        deleteSize++;
                        if (deleteSize == bukcets.size()) {
                            Platform.runLater(() -> {
                                if (values.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
                                    ds3PanelPresenter.filterChanged(ds3PanelPresenter.getSearchedText());
                                }
                                // deepStorageBrmainowserPresenter.logText("Delete response code: " + deleteObjectsResponse.getStatusCode(), LogType.SUCCESS);
                                if (ds3TreeTable.getRoot() == null || ds3TreeTable.getRoot().getValue() == null) {
                                    ds3TreeTable.setRoot(ds3TreeTable.getRoot().getParent());
                                    Platform.runLater(() -> {
                                        ds3TreeTable.getSelectionModel().clearSelection();
                                        ds3PanelPresenter.getDs3PathIndicator().setText("");
                                        ds3PanelPresenter.getDs3PathIndicatorTooltip().setText("");
                                    });

                                } else {
                                    ds3TreeTable.setRoot(selectedItem);
                                }
                                ds3TreeTable.getSelectionModel().select(selectedItem);
                                RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers);
                                deepStorageBrowserPresenter.logText("Successfully deleted file(s)", LogType.SUCCESS);

                            });
                        }
                    }

                } catch (final FailedRequestException fre) {
                    LOG.error("Failed to delete files", fre);
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete files : " + fre, LogType.ERROR));
                    ALERT.setContentText("Failed to delete a files");
                    ALERT.showAndWait();
                } catch (final IOException ioe) {
                    LOG.error("Failed to delete files", ioe);
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete files: " + ioe, LogType.ERROR));
                    ALERT.setContentText("Failed to delete a files");
                    ALERT.showAndWait();
                }
                return null;
            }
        };
        DeleteFilesPopup.show(task, null, this, ds3Common);
       /* values.stream().forEach(file -> refresh(file.getParent()));
        ds3TreeTable.getSelectionModel().clearSelection();*/
    }

    private void setContextMenuBehaviour() {
        disableContextMenu(true);
        ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // detect which deletes should be enabled
        ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = ds3TreeTable.getSelectionModel().getSelectedItems();
        if (Guard.isNullOrEmpty(selectedItems)) {
            selectedItems = FXCollections.observableArrayList();
            selectedItems.add(ds3TreeTable.getRoot());
        } else if (selectedItems.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
            LOG.error("You can not delete from here. Please go to specific location and delete object(s)");
            deleteFile.setDisable(false);
        } else {
            final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = selectedItems.stream().findFirst().orElse(null);

            if (selectedItems.size() == 1) {
                physicalPlacement.setDisable(false);

                switch (ds3TreeTableValueTreeItem.getValue().getType()) {
                    case Bucket:
                        deleteBucket.setDisable(false);
                        createFolder.setDisable(false);
                        createBucket.setDisable(false);
                        break;
                    case Directory:
                        deleteFolder.setDisable(false);
                        createFolder.setDisable(false);
                        break;
                    case File:
                        deleteFile.setDisable(false);
                        createFolder.setDisable(true);
                        metaData.setDisable(false);
                        break;
                    default:
                        break;
                }

            } else {
                if (!selectedItems.stream().map(TreeItem::getValue).anyMatch(value -> (value.getType() == Ds3TreeTableValue.Type.Directory) || (value.getType() == Ds3TreeTableValue.Type.Bucket))) {
                    deleteFile.setDisable(false);
                }
            }
        }
    }

    private void disableContextMenu(boolean disabled) {
        deleteBucket.setDisable(disabled);
        deleteFolder.setDisable(disabled);
        deleteFile.setDisable(disabled);
        physicalPlacement.setDisable(disabled);
        metaData.setDisable(disabled);
        createFolder.setDisable(disabled);
        createBucket.setDisable(disabled);
    }


    private void manageItemsCount(final TreeItem<Ds3TreeTableValue> selectedItem) {
        if (ds3TreeTable.getSelectionModel().getSelectedItems().size() == 1
                && selectedItem.getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
            ds3Common.getDs3PanelPresenter().getInfoLabel().setVisible(false);
            ds3Common.getDs3PanelPresenter().getCapacityLabel().setVisible(false);
        } else {
            ds3Common.getDs3PanelPresenter().getInfoLabel().setVisible(true);
            ds3Common.getDs3PanelPresenter().getCapacityLabel().setVisible(true);
            ds3Common.getDs3PanelPresenter().getInfoLabel().setText("Calculating.......");
            ds3Common.getDs3PanelPresenter().getCapacityLabel().setText(resourceBundle.getString("infoLabel"));
            ds3PanelPresenter.calculateFiles(ds3TreeTable);
        }
    }

    private void deleteFolderPrompt() {
        LOG.info("Got delete folder event");
        ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());
        final TreeItem<Ds3TreeTableValue> root = ds3TreeTable.getRoot();
        if (values.isEmpty() && null == root) {
            LOG.error("No files selected");
            return;
        } else if (values.isEmpty()) {
            final ImmutableList.Builder<TreeItem<Ds3TreeTableValue>> builder = ImmutableList.builder();
            values = builder.add(root).build().asList();

        }
        if (values.size() > 1) {
            LOG.error("You can only select a single folder to delete");
            return;
        }
        final TreeItem<Ds3TreeTableValue> selectedItem = values.get(0).getParent();
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
                        if (ds3TreeTable.getRoot() == null || ds3TreeTable.getRoot().getValue() == null) {
                            ds3TreeTable.setRoot(ds3TreeTable.getRoot().getParent());
                            Platform.runLater(() -> {
                                ds3TreeTable.getSelectionModel().clearSelection();
                                ds3PanelPresenter.getDs3PathIndicator().setText("");
                                ds3PanelPresenter.getDs3PathIndicatorTooltip().setText("");
                            });

                        } else {
                            ds3TreeTable.setRoot(selectedItem);
                        }
                        ds3TreeTable.getSelectionModel().select(selectedItem);
                        RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers);
                    });
                } catch (final FailedRequestException fre) {
                    LOG.error("Failed to delete folder", fre);
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete folder : " + fre, LogType.ERROR));
                    ALERT.setContentText("Failed to delete a folder");
                    ALERT.showAndWait();

                } catch (final IOException ioe) {
                    LOG.error("Failed to delete folder", ioe);
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete folder" + ioe, LogType.ERROR));
                    ALERT.setContentText("Failed to delete a folder");
                    ALERT.showAndWait();
                }
                return null;
            }
        };
        DeleteFilesPopup.show(task, null, this, ds3Common);
        /*values.stream().forEach(file -> refresh(file.getParent()));*/
    }

    private void deleteBucketPrompt() {
        LOG.info("Got delete bucket event");
        ImmutableList<TreeItem<Ds3TreeTableValue>> buckets = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());
        final TreeItem<Ds3TreeTableValue> root = ds3TreeTable.getRoot();
        if (buckets.isEmpty() && null == root) {
            LOG.info("No files selected");
            return;
        } else if (buckets.isEmpty()) {
            final ImmutableList.Builder<TreeItem<Ds3TreeTableValue>> builder = ImmutableList.builder();
            buckets = builder.add(root).build().asList();

        }
        if (buckets.size() > 1) {
            LOG.info("The user selected objects from multiple buckets.  This is not allowed.");
            ALERT.setContentText("The user selected multiple buckets.  This is not allowed.");
            ALERT.showAndWait();
            return;
        }
        final TreeItem<Ds3TreeTableValue> value = buckets.stream().findFirst().orElse(null);
        if (value != null) {
            if (value.getValue().getType() != Ds3TreeTableValue.Type.Bucket) {
                LOG.info("You can only delete a bucket with this command");
                ALERT.setContentText("You can only delete a bucket with this command");
                ALERT.showAndWait();
                return;
            }
            if (!checkIfBucketEmpty(value.getValue().getBucketName(), null)) {
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
                            getClient().deleteBucketSpectraS3(new DeleteBucketSpectraS3Request(value.getValue().getBucketName()).withForce(true));
                            Platform.runLater(() -> {
                                deepStorageBrowserPresenter.logText("Successfully deleted bucket", LogType.SUCCESS);
                            });

                        } catch (final FailedRequestException fre) {
                            LOG.error("Failed to delete Buckets", fre);
                            Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete Bucket : " + fre, LogType.ERROR));
                            ALERT.setContentText("Failed to delete bucket");
                            ALERT.showAndWait();

                        } catch (final IOException ioe) {
                            LOG.error("Failed to delete Bucket", ioe);
                            Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete Bucket." + ioe, LogType.ERROR));
                            ALERT.setContentText("Failed to delete a bucket");
                            ALERT.showAndWait();
                        }
                        return null;
                    }
                };
                DeleteFilesPopup.show(task, null, this, ds3Common);
                refreshTreeTableView();
                ds3PanelPresenter.getDs3PathIndicator().setText("");
                ds3PanelPresenter.getDs3PathIndicatorTooltip().setText("");
            }
        }
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
            ds3TreeTableItem.loadMore(ds3TreeTable, deepStorageBrowserPresenter);
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
        final GetServiceTask getServiceTask = new GetServiceTask(rootTreeItem.getChildren(), session, workers, ds3Common);
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

    /**
     * check if bucket contains files or folders
     *
     * @param bucketName
     * @param treeItem
     * @return true if bucket is empty else return false
     */
    private boolean checkIfBucketEmpty(final String bucketName, final TreeItem<Ds3TreeTableValue> treeItem) {
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