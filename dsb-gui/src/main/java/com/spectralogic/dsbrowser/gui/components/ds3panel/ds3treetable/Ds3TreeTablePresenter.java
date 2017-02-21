package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.DeleteObjectsRequest;
import com.spectralogic.ds3client.commands.DeleteObjectsResponse;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.models.DeleteResult;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.CreateService;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.DeleteService;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.Ds3PanelService;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.SortPolicyCallback;
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
import javafx.scene.input.*;
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

@SuppressWarnings("unchecked")
public class Ds3TreeTablePresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3TreeTablePresenter.class);

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
            deepStorageBrowserPresenter.logText("Loading Session " + session.getSessionName(), LogType.INFO);
            initContextMenu();
            initTreeTableView();
        } catch (final Throwable e) {
            LOG.error("Encountered error when creating Ds3TreeTablePresenter", e);
            throw e;
        }
    }

    /**
     * Assigning events to context menu
     */
    private void initContextMenu() {
        contextMenu = new ContextMenu();

        deleteFile = new MenuItem(resourceBundle.getString("deleteFileContextMenu"));
        deleteFile.setOnAction(event -> ds3DeleteObject());

        deleteFolder = new MenuItem(resourceBundle.getString("deleteFolderContextMenu"));
        deleteFolder.setOnAction(event -> ds3DeleteObject());

        deleteBucket = new MenuItem(resourceBundle.getString("deleteBucketContextMenu"));
        deleteBucket.setOnAction(event -> ds3DeleteObject());

        physicalPlacement = new MenuItem(resourceBundle.getString("physicalPlacementContextMenu"));
        physicalPlacement.setOnAction(event -> Ds3PanelService.showPhysicalPlacement(ds3Common, workers));

        metaData = new MenuItem(resourceBundle.getString("metaDataContextMenu"));
        metaData.setOnAction(event -> Ds3PanelService.showMetadata(ds3Common, workers));

        createBucket = new MenuItem(resourceBundle.getString("createBucketContextMenu"));
        createBucket.setOnAction(event -> CreateService.createBucketPrompt(ds3Common, workers));

        createFolder = new MenuItem(resourceBundle.getString("createFolderContextMenu"));
        createFolder.setOnAction(event -> CreateService.createFolderPrompt(ds3Common));

        contextMenu.getItems().addAll(metaData, physicalPlacement, new SeparatorMenuItem(), deleteFile, deleteFolder, deleteBucket, new SeparatorMenuItem(), createBucket, createFolder);
    }

    private void ds3DeleteObject() {
        LOG.info("Got delete bucket event");

        ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());

        final TreeItem<Ds3TreeTableValue> root = ds3TreeTable.getRoot();
        if (values.isEmpty()) {
            if (root == null) {
                LOG.info("No files selected");
                return;
            } else {
                final ImmutableList.Builder<TreeItem<Ds3TreeTableValue>> builder = ImmutableList.builder();
                values = builder.add(root).build().asList();
            }
        }

        if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.Directory)) {
            LOG.info("Going delete the folder");
            final TreeItem<Ds3TreeTableValue> treeItem = values.stream().findFirst().orElse(null);
            if (treeItem != null) {
                DeleteService.deleteFolder(ds3Common, values, workers);
            }
        } else if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.Bucket)) {
            LOG.info("Going delete the bucket");
            final TreeItem<Ds3TreeTableValue> treeItem = values.stream().findFirst().orElse(null);
            if (treeItem != null) {
                DeleteService.deleteBucket(ds3Common, values, workers);
            }
        } else if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.File)) {
            LOG.info("Going delete the file(s)");
            DeleteService.deleteFiles(ds3Common, values, workers);
        }
    }

    @SuppressWarnings("unchecked")
    private void initTreeTableView() {
        ds3Common.setDs3TreeTableView(ds3TreeTable);

        fullPath.setText(resourceBundle.getString("fullPath"));
        fileName.setText(resourceBundle.getString("fileName"));

        ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ds3TreeTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListenerInnerClass());

        //To show create bucket option on right click when there is no bucket present
        ds3TreeTable.setOnContextMenuRequested(event -> {
            disableContextMenu(true);
            createBucket.setDisable(false);
        });

        ds3TreeTable.setContextMenu(contextMenu);

        ds3TreeTable.setRowFactory(view -> setTreeTableViewRowBehaviour());
        ds3TreeTable.sortPolicyProperty().set(new SortPolicyCallback(ds3Common));

        final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);

        ds3TreeTable.setShowRoot(false);
        ds3TreeTable.setPlaceholder(new Label(resourceBundle.getString("noBucketFound")));

        final Node oldPlaceHolder = ds3TreeTable.getPlaceholder();
        final ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(90, 90);
        ds3TreeTable.setPlaceholder(new StackPane(progress));

        ds3TreeTable.setRoot(rootTreeItem);

        ds3TreeTable.expandedItemCountProperty().addListener((observable, oldValue, newValue) -> {
            final String info = ds3TreeTable.getExpandedItemCount() + " item(s), " + ds3TreeTable.getSelectionModel().getSelectedItems().size() + " item(s) selected";
            ds3PanelPresenter.getPaneItems().setVisible(true);
            ds3PanelPresenter.getPaneItems().setText(info);
        });

        final GetServiceTask getServiceTask = new GetServiceTask(rootTreeItem.getChildren(), session, workers, ds3Common);
        workers.execute(getServiceTask);

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
                    ds3Common.getExpandedNodesInfo().put(session.getSessionName() + StringConstants.SESSION_SEPARATOR + session.getEndpoint(), bean);
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

    /**
     * To set the treeTableView behaviour
     *
     * @return treeTableView
     */
    private TreeTableRow<Ds3TreeTableValue> setTreeTableViewRowBehaviour() {
        {
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

            row.setOnDragDropped(event -> handleDropEvent(event, row)

            );
            row.setOnMouseClicked(event -> setBehaviorOnMouseClick(event, row));
            row.setContextMenu(contextMenu);
            return row;
        }
    }

    /**
     * To handle the drop event on treeTableView
     *
     * @param event event
     * @param row   row
     */
    private void handleDropEvent(final DragEvent event, final TreeTableRow<Ds3TreeTableValue> row) {
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
                    final Ds3PutJob putJob = new Ds3PutJob(session.getClient(), db.getFiles(), bucket, targetDir, priority, settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads(), jobInterruptionStore, ds3Common, settingsStore);
                    jobWorkers.execute(putJob);
                    putJob.setOnSucceeded(e -> {
                        LOG.info("Succeed");
                        try {
                            Ds3PanelService.refresh(treeItem);
                            ds3TreeTable.getSelectionModel().clearSelection();
                            ds3TreeTable.getSelectionModel().select(treeItem);
                        } catch (final Exception ex) {
                            LOG.error("Failed to save job ID", ex);
                        }

                    });
                    putJob.setOnFailed(e -> {
                        LOG.error("setOnFailed");
                        Ds3PanelService.refresh(treeItem);
                        ds3TreeTable.getSelectionModel().clearSelection();
                        ds3TreeTable.getSelectionModel().select(treeItem);
                        RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers);
                    });
                    putJob.setOnCancelled(e -> {
                        LOG.info("setOnCancelled");
                        if (putJob.getJobId() != null) {
                            try {
                                session.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(putJob.getJobId()));
                                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, putJob.getJobId().toString(), putJob.getDs3Client().getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
                            } catch (final IOException e1) {
                                LOG.error("Failed to cancel job", e1);
                            }
                        }
                        Ds3PanelService.refresh(treeItem);
                        ds3TreeTable.getSelectionModel().clearSelection();
                        ds3TreeTable.getSelectionModel().select(treeItem);
                    });
                }
            } else {
                Ds3Alert.show(null, "Operation not allowed here", Alert.AlertType.ERROR);
            }
            event.consume();
        }
    }

    /**
     * To set the behaviour of the mouse click on treeTableView row
     *
     * @param event event
     * @param row   row
     */
    private void setBehaviorOnMouseClick(final MouseEvent event, final TreeTableRow<Ds3TreeTableValue> row) {
        {
            if (event.isControlDown() || event.isShiftDown()) {
                if (!rowNameList.contains(row.getTreeItem().getValue().getName())) {
                    rowNameList.add(row.getTreeItem().getValue().getName());
                    ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    ds3TreeTable.getSelectionModel().select(row.getIndex());
                } else {
                    ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
                ds3Common.setDs3TreeTableView(ds3TreeTable);
                if (row.getTreeItem() != null && !row.getTreeItem().getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
                    if (Ds3PanelService.checkIfBucketEmpty(row.getTreeItem().getValue().getBucketName(), session, row.getTreeItem()))
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
        }
    }

    /**
     * To set context menu's behaviour.
     */
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

    /**
     * To enable/disable context menu items
     *
     * @param disabled disable flag
     */
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
            ds3Common.getDs3PanelPresenter().getInfoLabel().setText(resourceBundle.getString("calculationProgressIndicator"));
            ds3Common.getDs3PanelPresenter().getCapacityLabel().setText(resourceBundle.getString("infoLabel"));
            ds3PanelPresenter.calculateFiles(ds3TreeTable);
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

    /**
     * To check whether there is any interrupted job ot not
     *
     * @param endpoint endpoint of the session
     */
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

    private class ChangeListenerInnerClass implements ChangeListener {

        @Override
        public void changed(final ObservableValue observable, final Object oldValue,
                            final Object newValue) {
            //noinspection unchecked
            TreeItem<Ds3TreeTableValue> selectedItem = (TreeItem<Ds3TreeTableValue>) newValue;

            //If selected item is null then setting up root as selected item
            if (selectedItem == null) {
                selectedItem = ds3TreeTable.getRoot();
            }

            if (selectedItem != null && selectedItem.getValue() != null) {
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
    }
}