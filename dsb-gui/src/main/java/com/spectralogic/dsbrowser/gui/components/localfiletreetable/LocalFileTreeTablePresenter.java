package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.SortPolicyCallback;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3CancelSingleJobTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3GetJob;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.services.tasks.GetMediaDeviceTask;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class LocalFileTreeTablePresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(LocalFileTreeTablePresenter.class);

    @FXML
    private TreeTableView<FileTreeModel> treeTable;

    @FXML
    private TreeTableColumn<FileTreeModel, Number> sizeColumn;

    @FXML
    private Button homeButton, refreshButton, toMyComputer, transferButton, parentDirectoryButton;

    @FXML
    private Tooltip homeButtonTooltip, refreshButtonTooltip, toMyComputerTooltip, transferButtonTooltip, parentDirectoryButtonTooltip;

    @FXML
    private Label localPathIndicator;

    @Inject
    private FileTreeTableProvider provider;

    @Inject
    private DataFormat dataFormat;

    @Inject
    private Workers workers;

    @Inject
    private JobWorkers jobWorkers;

    @Inject
    private Ds3SessionStore store;

    @Inject
    private ResourceBundle resourceBundle;

    @Inject
    private SavedJobPrioritiesStore savedJobPrioritiesStore;

    @Inject
    private JobInterruptionStore jobInterruptionStore;

    @Inject
    private SettingsStore settingsStore;

    @Inject
    private Ds3Common ds3Common;

    @Inject
    private EndpointInfo endpointInfo;

    private String fileRootItem = StringConstants.ROOT_LOCATION;

    private TreeItem<FileTreeModel> lastExpandedNode;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Starting LocalFileTreeTablePresenter");
            initToolTips();
            initTableView();
            initListeners();
            initProgressAndPathIndicators();
        } catch (final Throwable e) {
            LOG.error("Encountered an error when creating LocalFileTreeTablePresenter", e);
        }
    }

    private void initToolTips() {
        transferButtonTooltip.setText(resourceBundle.getString("transferButtonTooltip"));
        homeButtonTooltip.setText(resourceBundle.getString("homeButtonTooltip"));
        refreshButtonTooltip.setText(resourceBundle.getString("refreshButtonTooltip"));
        toMyComputerTooltip.setText(resourceBundle.getString("toMyComputerTooltip"));
        parentDirectoryButtonTooltip.setText(resourceBundle.getString("parentDirectoryButtonTooltip"));
    }

    private void initTableView() {
        treeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeTable.setOnDragEntered(event -> event.acceptTransferModes(TransferMode.COPY));
        treeTable.setOnDragOver(event -> event.acceptTransferModes(TransferMode.COPY));
        treeTable.setOnDragDropped(event -> {
            setDragDropEvent(null, event);
            event.consume();
        });
        treeTable.setRowFactory(view -> {
                    final TreeTableRow<FileTreeModel> row = new TreeTableRow<>();
                    final List<String> rowNameList = new ArrayList<>();
                    row.setOnMouseClicked(event -> {
                        LOG.info("Mouse Clicked..");
                        if (event.isControlDown() || event.isShiftDown()) {
                            selectMultipleItems(rowNameList, row);
                        } else if (event.getClickCount() == 2) {
                            if (row.getTreeItem() != null && row.getTreeItem().getValue() != null && !row.getTreeItem().getValue().getType().equals(FileTreeModel.Type.File)) {
                                changeRootDir(treeTable.getSelectionModel().getSelectedItem().getValue().getPath().toString());
                            }
                        } else {
                            treeTable.getSelectionModel().clearAndSelect(row.getIndex());
                        }
                    });
                    row.setOnDragDropped(event -> {
                        LOG.info("Drop detected..");
                        if (row.getTreeItem() != null) {
                            if (!row.getTreeItem().isLeaf() && !row.getTreeItem().isExpanded()) {
                                LOG.info("Expanding closed row");
                                row.getTreeItem().setExpanded(true);
                            }
                        }
                        setDragDropEvent(row, event);
                        event.consume();

                    });
                    row.setOnDragOver(event -> {
                        final TreeItem<FileTreeModel> treeItem = row.getTreeItem();
                        if (event.getGestureSource() != treeTable && event.getDragboard().hasFiles()) {
                            event.acceptTransferModes(TransferMode.COPY);
                            if (treeItem == null) {
                                if (fileRootItem.equals(StringConstants.ROOT_LOCATION)) {
                                    event.acceptTransferModes(TransferMode.NONE);
                                }
                            }
                            event.consume();
                        }
                    });
                    row.setOnDragEntered(event -> {
                        final TreeItem<FileTreeModel> treeItem = row.getTreeItem();
                        if (treeItem != null) {
                            final InnerShadow is = new InnerShadow();
                            is.setOffsetY(1.0f);
                            row.setEffect(is);
                        } else {
                            event.acceptTransferModes(TransferMode.NONE);
                        }
                        event.consume();
                    });
                    row.setOnDragExited(event -> {
                        row.setEffect(null);
                        event.consume();
                    });
                    row.setOnDragDetected(event -> {
                        LOG.info("Drag detected...");
                        final ObservableList<TreeItem<FileTreeModel>> selectedItems = treeTable.getSelectionModel().getSelectedItems();
                        if (!Guard.isNullOrEmpty(selectedItems)) {
                            LOG.info("Starting drag and drop event");
                            final Dragboard db = treeTable.startDragAndDrop(TransferMode.COPY);
                            final ClipboardContent content = new ClipboardContent();
                            content.putFilesByPath(selectedItems
                                    .stream()
                                    .filter(item -> item.getValue() != null)
                                    .map(i -> i.getValue().getPath().toString())
                                    .collect(Collectors.toList()));
                            db.setContent(content);
                        }
                        event.consume();
                    });
                    return row;
                }
        );

    }

    private void initListeners() {
        refreshButton.setOnAction(event -> refreshFileTreeView());
        homeButton.setOnAction(event -> changeRootDir(System.getProperty(StringConstants.SETTING_FILE_PATH)));
        toMyComputer.setOnAction(event -> changeRootDir(StringConstants.ROOT_LOCATION));
        transferButton.setOnAction(event -> transferToBlackPearl());
        parentDirectoryButton.setOnAction(event -> goToParentDirectory());
    }

    private void initProgressAndPathIndicators() {
        final Stream<FileTreeModel> rootItems = provider.getRoot(fileRootItem);
        localPathIndicator.setText(StringConstants.ROOT_LOCATION);
        final Node oldPlaceHolder = treeTable.getPlaceholder();
        final ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(Constants.PROGRESS_BAR_SIZE, Constants.PROGRESS_BAR_SIZE);
        treeTable.setPlaceholder(new StackPane(progress));
        final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTable.setShowRoot(false);
        startMediaTask(rootItems, rootTreeItem, oldPlaceHolder);
        ds3Common.setLocalTreeTableView(treeTable);
        ds3Common.setLocalFilePathIndicator(localPathIndicator);
    }

    private void goToParentDirectory() {
        if (!localPathIndicator.getText().equals(StringConstants.ROOT_LOCATION)) {
            if (Paths.get(fileRootItem).getParent() != null) {
                changeRootDir(Paths.get(fileRootItem).getParent().toString());
            } else {
                changeRootDir(StringConstants.ROOT_LOCATION);
            }
        }
    }

    private void transferToBlackPearl() {
        try {
            final ObservableList<TreeItem<FileTreeModel>> currentSelection = treeTable.getSelectionModel().getSelectedItems();
            if (currentSelection.isEmpty()) {
                Ds3Alert.show(resourceBundle.getString("information"), resourceBundle.getString("fileSelect"), Alert.AlertType.INFORMATION);
                return;
            }
            final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = ds3Common.getDs3TreeTableView();
            ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView.getSelectionModel().getSelectedItems()
                    .stream().collect(GuavaCollectors.immutableList());
            //Finding root in case of double click to get selected items
            final TreeItem<Ds3TreeTableValue> root = ds3TreeTableView.getRoot();
            if (root == null && Guard.isNullOrEmpty(values)) {
                    Ds3Alert.show(resourceBundle.getString("information"), resourceBundle.getString("selectDestination"), Alert.AlertType.INFORMATION);
                    return;
            }
            //If values is empty we have to assign it with root
            else if (Guard.isNullOrEmpty(values)) {
                final ImmutableList.Builder<TreeItem<Ds3TreeTableValue>> builder = ImmutableList.builder();
                values = builder.add(root).build().asList();
            }
            if (values.size() > 1) {
                Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("multipleDestError"), Alert.AlertType.ERROR);
                return;
            }
            if (values.stream().findFirst().get().getValue().isSearchOn()) {
                Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("operationNotAllowed"), Alert.AlertType.ERROR);
                return;
            }
            final TreeItem<Ds3TreeTableValue> treeItem = values.stream().findFirst().orElse(null);
            if (null != treeItem) {
                if (!treeItem.isExpanded()) {
                    treeItem.setExpanded(true);
                }
                final Ds3TreeTableValue value = treeItem.getValue();
                final String bucket = value.getBucketName();
                final String targetDir = value.getDirectoryName();
                LOG.info("Passing new Ds3PutJob to jobWorkers thread pool to be scheduled");
                final Session session = ds3Common.getCurrentSession();
                final List<File> files = currentSelection
                        .stream()
                        .map(i -> new File(i.getValue().getPath().toString()))
                        .collect(Collectors.toList());
                final String priority = (!savedJobPrioritiesStore.getJobSettings().getPutJobPriority().equals(resourceBundle.getString("defaultPolicyText"))) ? savedJobPrioritiesStore.getJobSettings().getPutJobPriority() : null;
                startPutJob(session.getClient(), files, bucket, targetDir,
                        ds3Common.getDeepStorageBrowserPresenter(), priority,
                        jobInterruptionStore, ds3Common, settingsStore, treeItem);
            } else {
                LOG.info("No item selected from server side");
            }
        } catch (final Exception e) {
            LOG.error("Failed to transfer data to black pearl", e);
            ds3Common.getDeepStorageBrowserPresenter().logText("Failed to transfer data to black pearl: " + e, LogType.ERROR);
        }
    }

    private void refreshBlackPearlSideItem(final TreeItem<Ds3TreeTableValue> treeItem) {
        if (treeItem instanceof Ds3TreeTableItem) {
            LOG.info("Refresh row");
            final Ds3TreeTableItem ds3TreeTableItem = (Ds3TreeTableItem) treeItem;
            ds3TreeTableItem.refresh();
        }
        else {
            LOG.info("selected tree item is not the instance of Ds3TreeItem");
        }
    }

    private void changeRootDir(final String rootDir) {
        localPathIndicator.setText(rootDir);
        fileRootItem = rootDir;
        final Stream<FileTreeModel> rootItems = provider.getRoot(rootDir);
        if (rootItems != null) {
            try {
                final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
                rootTreeItem.setExpanded(true);
                treeTable.setShowRoot(false);
                rootItems.forEach(ftm -> {
                    final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, ftm, workers);
                    rootTreeItem.getChildren().add(newRootTreeItem);
                });
                treeTable.setRoot(rootTreeItem);
            } catch (final Exception e) {
                LOG.error("Unable to get root directory", e);
            }
            treeTable.getSelectionModel().clearSelection();
        } else {
            LOG.info("Already at root directory");
        }
    }

    private void refreshFileTreeView() {
        LOG.info("Starting refreshing local file tree");
        final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTable.setShowRoot(false);
        final Stream<FileTreeModel> rootItems = provider.getRoot(fileRootItem);
        localPathIndicator.setText(fileRootItem);
        rootItems.forEach(ftm -> {
            final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, ftm, workers);
            rootTreeItem.getChildren().add(newRootTreeItem);
        });
        treeTable.setRoot(rootTreeItem);
        setExpandBehaviour(treeTable);
        if (lastExpandedNode != null) {
            if (treeTable.getRoot().getChildren().stream().filter(i -> i.getValue().getName().equals(lastExpandedNode.getValue().getName())).findFirst().isPresent()) {
                final TreeItem<FileTreeModel> ds3TreeTableValueTreeItem = treeTable.getRoot().getChildren().stream().filter(i -> i.getValue().getName().equals(lastExpandedNode.getValue().getName())).findFirst().get();
                ds3TreeTableValueTreeItem.setExpanded(false);
                if (!ds3TreeTableValueTreeItem.isLeaf() && !ds3TreeTableValueTreeItem.isExpanded()) {
                    LOG.info("Expanding closed row");
                    ds3TreeTableValueTreeItem.setExpanded(true);
                    treeTable.getSelectionModel().select(ds3TreeTableValueTreeItem);
                }
            }
        }
    }

    private void setExpandBehaviour(final TreeTableView<FileTreeModel> treeTable) {
        final ObservableList<TreeItem<FileTreeModel>> children = treeTable.getRoot().getChildren();
        children.forEach(i -> i.expandedProperty().addListener((observable, oldValue, newValue) -> {
            final BooleanProperty bb = (BooleanProperty) observable;
            final TreeItem<FileTreeModel> bean = (TreeItem<FileTreeModel>) bb.getBean();
            if (newValue) {
                lastExpandedNode = bean;
            }
        }));
    }

    /**
     * Select multiple items using shift and ctrl
     *
     * @param rowNameList selectMultipleItemslist of selected Items
     * @param row         select row
     */
    private void selectMultipleItems(final List<String> rowNameList, final TreeTableRow<FileTreeModel> row) {
        treeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if (!rowNameList.contains(row.getTreeItem().getValue().getName())) {
            rowNameList.add(row.getTreeItem().getValue().getName());
            treeTable.getSelectionModel().select(row.getIndex());
        } else {
            treeTable.getSelectionModel().clearSelection(row.getIndex());
            rowNameList.remove(row.getTreeItem().getValue().getName());
        }
    }

    /**
     * Start transfer from bp to Local
     *
     * @param listFiles    list of files selected for drag and drop
     * @param session      session
     * @param localPath    path where selected files need to transfer
     * @param fileTreeItem selected item
     */
    private void startGetJob(final List<Ds3TreeTableValueCustom> listFiles, final Session session, final Path localPath, final String priority, final TreeItem<FileTreeModel> fileTreeItem) {
        final Ds3GetJob getJob = new Ds3GetJob(listFiles, localPath, session.getClient(),
                priority, settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads(), jobInterruptionStore, ds3Common);
        jobWorkers.execute(getJob);
        getJob.setOnSucceeded(e -> {
            LOG.info("Get Job completed successfully");
            refresh(fileTreeItem);

        });
        getJob.setOnFailed(e -> {
            LOG.error("Get Job failed");
            refresh(fileTreeItem);

        });
        getJob.setOnCancelled(e -> {
            LOG.info("Get Job cancelled");
            try {
                //Cancellation of a job started
                final Ds3CancelSingleJobTask ds3CancelSingleJobTask = new Ds3CancelSingleJobTask(getJob.getJobId().toString(), endpointInfo, jobInterruptionStore, JobRequestType.GET.toString());
                workers.execute(ds3CancelSingleJobTask);
                ds3CancelSingleJobTask.setOnFailed(event -> LOG.error("Failed to cancel job"));
                ds3CancelSingleJobTask.setOnSucceeded(event -> {
                    ds3Common.getDeepStorageBrowserPresenter().logText("GET Job Cancelled", LogType.INFO);
                    refresh(fileTreeItem);
                });

            } catch (final Exception e1) {
                LOG.error("Failed to cancel job", e1);
                ds3Common.getDeepStorageBrowserPresenter().logText(" Failed to cancel job. ", LogType.ERROR);
            }
        });
    }

    private void startPutJob(final Ds3Client client, final List<File> files, final String bucket, final String targetDir, final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
                             final String priority,
                             final JobInterruptionStore jobInterruptionStore, final Ds3Common ds3Common, final SettingsStore settingsStore, final TreeItem<Ds3TreeTableValue> treeItem) {
        final Ds3PutJob putJob = new Ds3PutJob(client, files, bucket, targetDir, priority, settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads(), jobInterruptionStore, ds3Common, settingsStore);
        jobWorkers.execute(putJob);
        putJob.setOnSucceeded(event -> {
            LOG.info("Succeed");
            refreshBlackPearlSideItem(treeItem);
        });
        putJob.setOnFailed(e -> {
            LOG.error("Get Job failed");
            refreshBlackPearlSideItem(treeItem);
        });
        putJob.setOnCancelled(e -> {
            LOG.info("Get Job cancelled");
            try {
                if (putJob.getJobId() != null) {
                    client.cancelJobSpectraS3(new CancelJobSpectraS3Request(putJob.getJobId()));
                    ds3Common.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("putJobCancelled"), LogType.SUCCESS);
                    ParseJobInterruptionMap.removeJobID(jobInterruptionStore, putJob.getJobId().toString(), putJob.getDs3Client().getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
                    refreshBlackPearlSideItem(treeItem);
                }
            } catch (final IOException e1) {
                LOG.error("Failed to cancel job", e1);
            }
        });

    }

    private void startMediaTask(final Stream<FileTreeModel> rootItems, final TreeItem<FileTreeModel> rootTreeItem, final Node oldPlaceHolder) {
        final GetMediaDeviceTask getMediaDeviceTask = new GetMediaDeviceTask(rootItems, rootTreeItem, provider, workers);
        workers.execute(getMediaDeviceTask);
        getMediaDeviceTask.setOnSucceeded(event -> {
            treeTable.setRoot(rootTreeItem);
            treeTable.setPlaceholder(oldPlaceHolder);
            setExpandBehaviour(treeTable);
            sizeColumn.setCellFactory(c -> new TreeTableCell<FileTreeModel, Number>() {

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
            treeTable.sortPolicyProperty().set(new SortPolicyCallback(ds3Common.getLocalTreeTableView()));
        });

    }

    /**
     * Refresh selected item
     *
     * @param selectedItem selectedItem
     */
    private void refresh(TreeItem<FileTreeModel> selectedItem) {
        if (selectedItem == null || selectedItem.getValue() == null) {
            refreshFileTreeView();
        } else if (selectedItem.getValue().getType().equals(FileTreeModel.Type.File)) {
            if (selectedItem.getParent().getValue() != null) {
                ds3Common.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("refreshing")
                        + StringConstants.SPACE
                        + selectedItem.getParent().getValue().getName(), LogType.SUCCESS);
                selectedItem = selectedItem.getParent();
            } else {
                refreshFileTreeView();
            }
        } else {
            ds3Common.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("refreshing")
                    + StringConstants.SPACE
                    + selectedItem.getValue().getName(), LogType.SUCCESS);
        }
        if (selectedItem instanceof FileTreeTableItem) {
            final FileTreeTableItem fileTreeTableItem = (FileTreeTableItem) selectedItem;
            fileTreeTableItem.refresh();
        }

    }

    private Session getSession(final String sessionName) {
        return store.getSessions().filter(sessions -> (sessions.getSessionName()
                + StringConstants.SESSION_SEPARATOR + sessions.getEndpoint()).equals(sessionName)).findFirst().orElse(null);
    }

    /**
     * Drop Event action
     *
     * @param row   row
     * @param event event
     */
    private void setDragDropEvent(final TreeTableRow<FileTreeModel> row, final DragEvent event) {
        final Path localPath = getSelectedLocalPath(row);
        if (localPath == null) {
            return;
        }
        final Dragboard db = event.getDragboard();
        if (db.hasContent(dataFormat)) {
            LOG.info("Drop event contains files");

            @SuppressWarnings("unchecked")
            final List<Ds3TreeTableValueCustom> list = (List<Ds3TreeTableValueCustom>) db.getContent(dataFormat);
            final Session session = getSession(db.getString());
            final String priority = (!savedJobPrioritiesStore.getJobSettings().getGetJobPriority().equals(resourceBundle.getString("defaultPolicyText"))) ? savedJobPrioritiesStore.getJobSettings().getGetJobPriority() : null;
            startGetJob(list, session, localPath, priority, null);
        }
    }

    /**
     * Get the location where selected files needs to be dropped
     *
     * @param row selected row
     * @return Path
     */
    private Path getSelectedLocalPath(final TreeTableRow<FileTreeModel> row) {
        Path localPath = null;
        if (row != null) {
            final TreeItem<FileTreeModel> fileTreeItem = row.getTreeItem();
            if (fileTreeItem != null && !fileTreeItem.getValue().getType().equals(FileTreeModel.Type.File)) {
                localPath = fileTreeItem.getValue().getPath();
            } else if (!fileRootItem.equals(StringConstants.ROOT_LOCATION)) {
                localPath = Paths.get(fileRootItem);
            } else if (fileTreeItem != null && fileTreeItem.getValue().getType().equals(FileTreeModel.Type.File)) {
                localPath = fileTreeItem.getParent().getValue().getPath();
            }

        } else {
            if (!fileRootItem.equals(StringConstants.ROOT_LOCATION)) {
                localPath = Paths.get(fileRootItem);
            }
        }
        return localPath;
    }
}
