package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.CreateService;
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
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("unchecked")
@Presenter
public class Ds3TreeTablePresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3TreeTablePresenter.class);

    private final LazyAlert alert = new LazyAlert("Error");

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

    @ModelContext
    private Ds3PanelPresenter ds3PanelPresenter;

    @ModelContext
    private Session session;

    private final Workers workers;
    private final JobWorkers jobWorkers;
    private final ResourceBundle resourceBundle;
    private final DataFormat dataFormat;
    private final Ds3Common ds3Common;
    private final SavedJobPrioritiesStore savedJobPrioritiesStore;
    private final JobInterruptionStore jobInterruptionStore;
    private final SettingsStore settingsStore;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final LoggingService loggingService;

    private ContextMenu contextMenu;

    private MenuItem physicalPlacement, deleteFile, deleteFolder, deleteBucket, metaData, createBucket, createFolder;

    private final ObservableMap<Integer, Node> disclosureNodeMap = FXCollections.observableHashMap();

    private boolean isFirstTime = true;

    @Inject
    public Ds3TreeTablePresenter(final ResourceBundle resourceBundle,
                                 final DataFormat dataFormat,
                                 final Workers workers,
                                 final JobWorkers jobWorkers,
                                 final Ds3Common ds3Common,
                                 final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
                                 final SavedJobPrioritiesStore savedJobPrioritiesStore,
                                 final JobInterruptionStore jobInterruptionStore,
                                 final SettingsStore settingsStore,
                                 final LoggingService loggingService) {
        this.resourceBundle = resourceBundle;
        this.dataFormat = dataFormat;
        this.workers = workers;
        this.jobWorkers = jobWorkers;
        this.ds3Common = ds3Common;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.savedJobPrioritiesStore = savedJobPrioritiesStore;
        this.jobInterruptionStore = jobInterruptionStore;
        this.settingsStore = settingsStore;
        this.loggingService = loggingService;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.debug("Loading new session tab for Session [{}]", session.getSessionName());
            loggingService.logMessage("Loading Session " + session.getSessionName(), LogType.INFO);
            initContextMenu();
            initTreeTableView();
            setTreeTableViewBehaviour();
        } catch (final Exception e) {
            LOG.error("Encountered error when creating Ds3TreeTablePresenter", e);
        }
    }

    private void setTreeTableViewBehaviour() {
        ds3TreeTable.setOnDragEntered(event -> event.acceptTransferModes(TransferMode.COPY));
        ds3TreeTable.setOnDragOver(event -> event.acceptTransferModes(TransferMode.COPY));
        ds3TreeTable.setOnDragDropped(event -> {
            handleDropEvent(event, null);
            event.consume();
        });
        ds3TreeTable.focusedProperty().addListener((observable, oldValue, newValue) -> {
            this.deepStorageBrowserPresenter.getSelectAllMenuItem().setDisable(oldValue);
        });
    }

    /**
     * Assigning events to context menu
     */
    private void initContextMenu() {
        contextMenu = new ContextMenu();
        deleteFile = new MenuItem(resourceBundle.getString("deleteFileContextMenu"));
        deleteFile.setOnAction(event -> ds3PanelPresenter.ds3DeleteObject(false));

        deleteFolder = new MenuItem(resourceBundle.getString("deleteFolderContextMenu"));
        deleteFolder.setOnAction(event -> ds3PanelPresenter.ds3DeleteObject(false));

        deleteBucket = new MenuItem(resourceBundle.getString("deleteBucketContextMenu"));
        deleteBucket.setOnAction(event -> ds3PanelPresenter.ds3DeleteObject(false));

        physicalPlacement = new MenuItem(resourceBundle.getString("physicalPlacementContextMenu"));
        physicalPlacement.setOnAction(event -> Ds3PanelService.showPhysicalPlacement(ds3Common, workers, resourceBundle));

        metaData = new MenuItem(resourceBundle.getString("metaDataContextMenu"));
        metaData.setOnAction(event -> Ds3PanelService.showMetadata(ds3Common, workers, resourceBundle));

        createBucket = new MenuItem(resourceBundle.getString("createBucketContextMenu"));
        createBucket.setOnAction(event -> CreateService.createBucketPrompt(ds3Common, workers, loggingService, resourceBundle));

        createFolder = new MenuItem(resourceBundle.getString("createFolderContextMenu"));
        createFolder.setOnAction(event -> CreateService.createFolderPrompt(ds3Common, loggingService, resourceBundle));

        contextMenu.getItems().addAll(metaData, physicalPlacement, new SeparatorMenuItem(), deleteFile, deleteFolder, deleteBucket, new SeparatorMenuItem(), createBucket, createFolder);
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

        ds3TreeTable.setOnKeyPressed(event -> {
            final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = ds3TreeTable.getSelectionModel().getSelectedItems();
            if (!Guard.isNullOrEmpty(selectedItems)) {
                if (event.getCode().equals(KeyCode.DELETE)) {
                    ds3Common.getDs3PanelPresenter().ds3DeleteObject(false);
                    event.consume();
                }
            }
        });

        ds3TreeTable.setRowFactory(view -> setTreeTableViewRowBehaviour());
        ds3TreeTable.sortPolicyProperty().set(new SortPolicyCallback(ds3Common.getDs3TreeTableView()));

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
            if (ds3Common.getCurrentSession() != null) {
                LOG.info("Loading Session {}", session.getSessionName());
                loggingService.logMessage("Loading Session " + session.getSessionName(), LogType.INFO);

                final String info = StringBuilderUtil.getSelectedItemCountInfo(ds3TreeTable.getExpandedItemCount(),
                        ds3TreeTable.getSelectionModel().getSelectedItems().size()).toString();
                ds3PanelPresenter.getPaneItems().setVisible(true);
                ds3PanelPresenter.getPaneItems().setText(info);
                //Make Select All menu item disable if current visible item is Bucket or empty else enable it
                if (ds3TreeTable.getExpandedItemCount() == 0 || null == ds3TreeTable.getRoot().getValue()) {
                    deepStorageBrowserPresenter.getSelectAllMenuItem().setDisable(true);
                } else {
                    deepStorageBrowserPresenter.getSelectAllMenuItem().setDisable(false);
                }
            } else {
                LOG.info("No current session.");
                ds3PanelPresenter.setBlank(true);
                ds3PanelPresenter.disableSearch(true);
            }
        });

        final GetServiceTask getServiceTask = new GetServiceTask(rootTreeItem.getChildren(), session, workers, ds3Common, loggingService);
        LOG.info("Getting buckets from {}", session.getEndpoint());
        workers.execute(getServiceTask);

        progress.progressProperty().bind(getServiceTask.progressProperty());

        Platform.runLater(() ->
        getServiceTask.setOnSucceeded(event -> {
            ds3TreeTable.setPlaceholder(oldPlaceHolder);

            final ObservableList<TreeItem<Ds3TreeTableValue>> children = ds3TreeTable.getRoot().getChildren();

            children.forEach(i -> i.expandedProperty().addListener((observable, oldValue, newValue) -> {
                final BooleanProperty bb = (BooleanProperty) observable;
                final TreeItem<Ds3TreeTableValue> bean = (TreeItem<Ds3TreeTableValue>) bb.getBean();
                ((Ds3TreeTableItem) bean).setDs3TreeTable(ds3TreeTable);
                ds3Common.getExpandedNodesInfo().put(session.getSessionName() + StringConstants.SESSION_SEPARATOR + session.getEndpoint(), bean);
            }));

            fileName.setCellFactory(c -> new TreeTableCell<Ds3TreeTableValue, String>() {

                @Override
                protected void updateItem(final String item, final boolean empty) {
                    super.updateItem(item, empty);
                    if (!empty && item.equals(resourceBundle.getString("addMoreButton"))) {
                        this.getStyleClass().add("styleBold");
                    } else if (!empty) {
                        this.getStyleClass().add("styleNormal");
                    }
                    this.getStylesheets().add(getClass().getResource("ds3treetable.css").toExternalForm());
                    setText(item);
                }
            });

            sizeColumn.setCellFactory(c -> new ValueTreeTableCell());

            fileType.setCellFactory(c -> new TreeTableValueTreeTableCell());
            checkInterruptedJob(session.getEndpoint() + ":" + session.getPortNo());
        }));
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

            row.setOnDragDetected(this::handleDragDetectedEvent);

            row.setOnDragOver(event -> setDragOverBehaviour(event, row));

            row.setOnDragEntered(event -> setDragEnteredBehaviour(event, row));

            row.setOnDragExited(event -> {
                row.setEffect(null);
                event.consume();
            });

            row.setOnDragDropped(event -> handleDropEvent(event, row));
            row.setOnMouseClicked(event -> setBehaviorOnMouseClick(event, row));
            row.setContextMenu(contextMenu);

            //Set item property listener on row for maintaining expand property
            row.treeItemProperty().addListener((observable, oldValue, newValue) -> {
                if (ds3TreeTable == null) {
                    ds3TreeTable = ds3Common.getDs3TreeTableView();
                }
                if (null != ds3TreeTable.getRoot() && null != ds3TreeTable.getRoot().getValue()) {
                    //Set disclosure node of row to null if it is child of bucket
                    row.setDisclosureNode(null);
                    isFirstTime = false;
                } else {
                    if (isFirstTime && null != row.getDisclosureNode()) {
                        //Put disclosure nodes into disclosureNodeMap so that
                        // we can use this to set on row to make it expandable
                        disclosureNodeMap.put(row.getIndex(), row.getDisclosureNode());
                    } else if (null != disclosureNodeMap.get(row.getIndex())) {
                        //Set disclosure node on row to make it expandable in case of bucket
                        row.setDisclosureNode(disclosureNodeMap.get(row.getIndex()));
                    }
                }
            });
            return row;
        }
    }

    private void setDragEnteredBehaviour(final DragEvent event, final TreeTableRow<Ds3TreeTableValue> row) {
        final TreeItem<Ds3TreeTableValue> selectedItem = getSelectedItem(row);
        if (selectedItem != null) {
            final InnerShadow is = new InnerShadow();
            is.setOffsetY(1.0f);
            row.setEffect(is);
        }
        event.consume();
    }

    private void setDragOverBehaviour(final DragEvent event, final TreeTableRow<Ds3TreeTableValue> row) {
        final TreeItem<Ds3TreeTableValue> selectedItem = getSelectedItem(row);
        if (selectedItem != null) {
            if (event.getGestureSource() != ds3TreeTable && event.getDragboard().hasFiles()) {
                if (!selectedItem.getValue().isSearchOn())
                    event.acceptTransferModes(TransferMode.COPY);
                else
                    event.acceptTransferModes(TransferMode.NONE);
            } else {
                event.acceptTransferModes(TransferMode.NONE);
            }
            event.consume();
        }
    }

    private TreeItem<Ds3TreeTableValue> getSelectedItem(final TreeTableRow<Ds3TreeTableValue> row) {
        final TreeItem<Ds3TreeTableValue> root = ds3TreeTable.getRoot();
        if ((row == null || row.getTreeItem() == null || row.getTreeItem().getValue() == null) && root.getValue() != null) {
            return root;
        } else if (row != null) {
            return row.getTreeItem();
        }
        return null;
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
            final TreeItem<Ds3TreeTableValue> selectedItem = getSelectedItem(row);
            if (null != selectedItem) {
                if (!selectedItem.isLeaf() && !selectedItem.isExpanded()) {
                    LOG.info("Expanding closed row");
                    selectedItem.setExpanded(true);
                }
            }
            if (null != selectedItem && null != selectedItem.getValue() &&
                    !selectedItem.getValue().isSearchOn()) {
                final Dragboard db = event.getDragboard();
                if (db.hasFiles()) {
                    for (final File file : db.getFiles()) {
                        final Path p = file.toPath();
                        if (!Files.isReadable(p)) {
                            loggingService.logMessage("File " + p.toAbsolutePath().toString() + " is not readable", LogType.ERROR);
                        }
                    }
                    LOG.info("Drop event contains files");
                    // get bucket info and current path
                    final Ds3TreeTableValue value = selectedItem.getValue();
                    final String bucket = value.getBucketName();
                    final String targetDir = value.getDirectoryName();
                    LOG.info("Passing new Ds3PutJob to jobWorkers thread pool to be scheduled");
                    final String priority = (!savedJobPrioritiesStore.getJobSettings().getPutJobPriority().equals(resourceBundle.getString("defaultPolicyText"))) ? savedJobPrioritiesStore.getJobSettings().getPutJobPriority() : null;
                    //TODO There are two places we put jobs. This needs a refactor
                    final Ds3PutJob putJob = new Ds3PutJob(session.getClient(), db.getFiles(), bucket, targetDir, priority,
                            settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads(), jobInterruptionStore,
                            deepStorageBrowserPresenter, session, settingsStore, loggingService, resourceBundle);
                    jobWorkers.execute(putJob);
                    putJob.setOnSucceeded(e -> {
                        LOG.info("Succeed");
                        try {
                            Ds3PanelService.refresh(selectedItem);
                            ds3TreeTable.getSelectionModel().clearSelection();
                            ds3TreeTable.getSelectionModel().select(selectedItem);
                        } catch (final Exception ex) {
                            LOG.error("Failed to save job ID", ex);
                        }

                    });
                    putJob.setOnFailed(e -> {
                        LOG.info("setOnFailed");
                        Ds3PanelService.refresh(selectedItem);
                        ds3TreeTable.getSelectionModel().clearSelection();
                        ds3TreeTable.getSelectionModel().select(selectedItem);
                        RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers, loggingService);
                    });
                    putJob.setOnCancelled(e -> {
                        LOG.info("setOnCancelled");
                        if (putJob.getJobId() != null) {
                            try {
                                session.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(putJob.getJobId()));
                                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, putJob.getJobId().toString(), putJob.getDs3Client().getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter, loggingService);
                            } catch (final IOException e1) {
                                LOG.error("Failed to cancel job", e1);
                            }
                        }
                        Ds3PanelService.refresh(selectedItem);
                        ds3TreeTable.getSelectionModel().clearSelection();
                        ds3TreeTable.getSelectionModel().select(selectedItem);
                    });
                }
            } else {
                alert.showAlert("Operation not allowed here.");
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
        if (event.isControlDown() || event.isShiftDown() || event.isShortcutDown()) {
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
            if (!row.getTreeItem().getValue().getType().equals(Ds3TreeTableValue.Type.Loader)) {
                final ProgressIndicator progress = new ProgressIndicator();
                progress.setMaxSize(90, 90);
                ds3TreeTable.setPlaceholder(new StackPane(progress));
                ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                ds3TreeTable.getSelectionModel().select(row.getIndex());
                ds3Common.getDs3PanelPresenter().setDs3TreeTablePresenter(this);
                ds3Common.setDs3TreeTableView(ds3TreeTable);
                if (row.getTreeItem() != null && !row.getTreeItem().getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
                    if (Ds3PanelService.checkIfBucketEmpty(row.getTreeItem().getValue().getBucketName(), session))
                        ds3TreeTable.setPlaceholder(null);
                    row.getTreeItem().setExpanded(true);
                    ds3TreeTable.setShowRoot(false);
                    ds3TreeTable.setRoot(row.getTreeItem());
                    ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                }
            }
        } else if (event.getButton().name().equals(StringConstants.CLICK_TYPE)) {
            try {
                if (row.getTreeItem().getValue().getType().equals(Ds3TreeTableValue.Type.Loader)) {
                    loadMore(row.getTreeItem());
                }
            } catch (final Exception e) {
                LOG.error("Not able to get tree item", e);
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
            createBucket.setDisable(false);
        } else if (selectedItems.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
            LOG.info("You can not delete from here. Please go to specific location and delete object(s)");
            deleteFile.setDisable(false);
        } else {
            final Optional<TreeItem<Ds3TreeTableValue>> first = selectedItems.stream().findFirst();

            if (first.isPresent()) {
                final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = first.get();
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
                    if (selectedItems.stream().map(TreeItem::getValue).noneMatch(value ->
                            (value.getType() == Ds3TreeTableValue.Type.Directory) || (value.getType() == Ds3TreeTableValue.Type.Bucket))) {
                        deleteFile.setDisable(false);
                    }
                }
            }
        }
    }

    /**
     * To enable/disable context menu items
     *
     * @param disabled disable flag
     */
    private void disableContextMenu(final boolean disabled) {
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
            ds3TreeTableItem.loadMore(ds3TreeTable);
        }
    }

    /**
     * To check whether there is any interrupted job ot not
     *
     * @param endpoint endpoint of the session
     */
    private void checkInterruptedJob(final String endpoint) {
        if (jobInterruptionStore.getJobIdsModel().getEndpoints() != null) {
            final ImmutableList<Map<String, Map<String, FilesAndFolderMap>>> endpoints = jobInterruptionStore.getJobIdsModel().getEndpoints().stream().collect(GuavaCollectors.immutableList());
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(endpoints, endpoint, deepStorageBrowserPresenter.getJobProgressView(), null);
            if (!Guard.isMapNullOrEmpty(jobIDMap)) {
                deepStorageBrowserPresenter.getLblCount().setText(String.valueOf(jobIDMap.size()));
            } else {
                deepStorageBrowserPresenter.getLblCount().setText(StringConstants.EMPTY_STRING);
                deepStorageBrowserPresenter.getJobButton().setDisable(true);
            }
        }
    }

    private static class ValueTreeTableCell extends TreeTableCell<Ds3TreeTableValue, Number> {
        @Override
        protected void updateItem(final Number item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(FileSizeFormat.getFileSizeType(item.longValue()));
            }
        }

    }

    private static class TreeTableValueTreeTableCell extends TreeTableCell<Ds3TreeTableValue, Ds3TreeTableValue.Type> {

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

    }

    private class ChangeListenerInnerClass implements ChangeListener {

        @Override
        public void changed(final ObservableValue observable, final Object oldValue,
                            final Object newValue) {

            if (ds3Common.getCurrentSession() == null) {
                ds3Common.getDs3PanelPresenter().setBlank(true);
                ds3Common.getDs3PanelPresenter().disableSearch(true);
                return;
            }
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
            } else {
                ds3Common.getDs3PanelPresenter().getInfoLabel().setVisible(false);
                ds3Common.getDs3PanelPresenter().getCapacityLabel().setVisible(false);
            }
            final String info = StringBuilderUtil.getSelectedItemCountInfo(ds3TreeTable.getExpandedItemCount(),
                    ds3TreeTable.getSelectionModel().getSelectedItems().size()).toString();
            ds3PanelPresenter.getPaneItems().setVisible(true);
            ds3PanelPresenter.getPaneItems().setText(info);
        }
    }

    private void handleDragDetectedEvent(final Event event) {
        LOG.info("Drag detected...");
        final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = ds3TreeTable.getSelectionModel().getSelectedItems();
        final ImmutableList<Ds3TreeTableValue> selectedI = selectedItems.stream().map(TreeItem::getValue).collect(GuavaCollectors.immutableList());
        final ImmutableList<Ds3TreeTableValueCustom> selected = selectedI.stream().map(v -> new Ds3TreeTableValueCustom(v.getBucketName(), v.getFullName(), v.getType(), v.getSize(), v.getLastModified(), v.getOwner(), v.isSearchOn())).collect(GuavaCollectors.immutableList());
        if (!Guard.isNullOrEmpty(selectedI)) {
            LOG.info("Starting drag and drop event");
            final Dragboard db = ds3TreeTable.startDragAndDrop(TransferMode.COPY);
            final ClipboardContent content = new ClipboardContent();
            content.put(dataFormat, selected);
            content.putString(session.getSessionName() + StringConstants.SESSION_SEPARATOR + session.getEndpoint());
            content.putFilesByPath(selected.stream().map(Ds3TreeTableValueCustom::getName).collect(GuavaCollectors.immutableList()));
            db.setContent(content);
        }
        event.consume();
    }

}
