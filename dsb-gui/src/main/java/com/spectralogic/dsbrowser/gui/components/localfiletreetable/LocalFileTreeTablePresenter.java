/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
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
import com.spectralogic.dsbrowser.gui.services.tasks.*;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Presenter
public class LocalFileTreeTablePresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(LocalFileTreeTablePresenter.class);

    private final LazyAlert alert = new LazyAlert("Error");

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

    private final ResourceBundle resourceBundle;
    private final Ds3Common ds3Common;
    private final FileTreeTableProvider fileTreeTableProvider;
    private final DataFormat dataFormat;
    private final Workers workers;
    private final JobWorkers jobWorkers;
    private final Ds3SessionStore ds3SessionStore;
    private final SavedJobPrioritiesStore savedJobPrioritiesStore;
    private final JobInterruptionStore jobInterruptionStore;
    private final SettingsStore settingsStore;
    private final EndpointInfo endpointInfo;
    private final LoggingService loggingService;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    private String fileRootItem = StringConstants.ROOT_LOCATION;

    private TreeItem<FileTreeModel> lastExpandedNode;

    @Inject
    public LocalFileTreeTablePresenter(final ResourceBundle resourceBundle,
                                       final Ds3Common ds3Common,
                                       final FileTreeTableProvider fileTreeTableProvider,
                                       final DataFormat dataFormat,
                                       final Workers workers,
                                       final JobWorkers jobWorkers,
                                       final Ds3SessionStore ds3SessionStore,
                                       final SavedJobPrioritiesStore savedJobPrioritiesStore,
                                       final JobInterruptionStore jobInterruptionStore,
                                       final SettingsStore settingsStore,
                                       final EndpointInfo endpointInfo,
                                       final LoggingService loggingService,
                                       final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        this.resourceBundle = resourceBundle;
        this.ds3Common = ds3Common;
        this.fileTreeTableProvider = fileTreeTableProvider;
        this.dataFormat = dataFormat;
        this.workers = workers;
        this.jobWorkers = jobWorkers;
        this.ds3SessionStore = ds3SessionStore;
        this.savedJobPrioritiesStore = savedJobPrioritiesStore;
        this.jobInterruptionStore = jobInterruptionStore;
        this.settingsStore = settingsStore;
        this.endpointInfo = endpointInfo;
        this.loggingService = loggingService;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.debug("Initialize...");
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
                        if (event.isControlDown() || event.isShiftDown() || event.isShortcutDown()) {
                            selectMultipleItems(rowNameList, row);
                        } else if (event.getClickCount() == 2) {
                            if (row.getTreeItem() != null
                                    && row.getTreeItem().getValue() != null
                                    && !row.getTreeItem().getValue().getType().equals(FileTreeModel.Type.File)) {
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

        treeTable.focusedProperty().addListener((observable, oldValue, newValue) -> {
            this.deepStorageBrowserPresenter.getSelectAllMenuItem().setDisable(oldValue);
        });
    }

    private void initListeners() {
        refreshButton.setOnAction(event -> refreshFileTreeView());
        homeButton.setOnAction(event -> changeRootDir(System.getProperty(StringConstants.SETTING_FILE_PATH)));
        toMyComputer.setOnAction(event -> changeRootDir(StringConstants.ROOT_LOCATION));
        transferButton.setOnAction(event -> transferToBlackPearl());
        parentDirectoryButton.setOnAction(event -> goToParentDirectory());
    }

    private void initProgressAndPathIndicators() {
        final Stream<FileTreeModel> rootItems = fileTreeTableProvider.getRoot(fileRootItem);
        localPathIndicator.setText(StringConstants.ROOT_LOCATION);
        final Node oldPlaceHolder = treeTable.getPlaceholder();
        final ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(Constants.PROGRESS_BAR_SIZE, Constants.PROGRESS_BAR_SIZE);
        treeTable.setPlaceholder(new StackPane(progress));
        final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTable.setShowRoot(false);
        Platform.runLater(() -> {
            startMediaTask(rootItems, rootTreeItem, oldPlaceHolder);
            ds3Common.setLocalTreeTableView(treeTable);
            ds3Common.setLocalFilePathIndicator(localPathIndicator);
        });

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
            if (ds3Common.getCurrentSession() == null) {
                alert.showAlert(resourceBundle.getString("noSession"));
                return;
            }
            final ObservableList<TreeItem<FileTreeModel>> currentSelection = treeTable.getSelectionModel().getSelectedItems();

            if (currentSelection.isEmpty()) {
                alert.showAlert(resourceBundle.getString("fileSelect"));
                return;
            }
            final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = ds3Common.getDs3TreeTableView();
            ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView.getSelectionModel().getSelectedItems()
                    .stream().collect(GuavaCollectors.immutableList());
            //Finding root in case of double click to get selected items
            final TreeItem<Ds3TreeTableValue> root = ds3TreeTableView.getRoot();
            if ((root == null || null == root.getValue()) && Guard.isNullOrEmpty(values)) {
                alert.showAlert(resourceBundle.getString("selectDestination"));
                return;
            }
            //If values is empty we have to assign it with root
            else if (Guard.isNullOrEmpty(values)) {
                final ImmutableList.Builder<TreeItem<Ds3TreeTableValue>> builder = ImmutableList.builder();
                values = builder.add(root).build().asList();
            }
            if (values.size() > 1) {
                alert.showAlert(resourceBundle.getString("multipleDestError"));
                return;
            }

            final Optional<TreeItem<Ds3TreeTableValue>> first = values.stream().findFirst();

            if (first.isPresent()) {

                final TreeItem<Ds3TreeTableValue> treeItem = first.get();

                if (treeItem.getValue().isSearchOn()) {
                    alert.showAlert(resourceBundle.getString("operationNotAllowed"));
                    return;
                }

                if (!treeItem.isExpanded()) {
                    treeItem.setExpanded(true);
                }
                final Ds3TreeTableValue value = treeItem.getValue();
                final String bucket = value.getBucketName();
                final String targetDir = value.getDirectoryName();
                LOG.info("Passing new Ds3PutJob to jobWorkers thread pool to be scheduled");
                final Session session = ds3Common.getCurrentSession();

                for(final TreeItem<FileTreeModel> selection : currentSelection) {
                    final Path path = selection.getValue().getPath();
                    if(Files.isDirectory(path) && Files.list(path).count() == 0) {
                        final CreateFolderTask task = new CreateFolderTask(session.getClient(),bucket, path.getFileName().toString(), null, loggingService, resourceBundle);
                        workers.execute(task);
                        task.setOnSucceeded(e -> {loggingService.logMessage("Created folder", LogType.INFO);});
                        task.setOnFailed(e -> {loggingService.logMessage("failed to create folder", LogType.ERROR);});
                    }
                }

                final ImmutableList<File> files = currentSelection
                        .stream()
                        .map(i -> i.getValue().getPath())
                        .filter(path -> !isEmptyFolder(path))
                        .map(i -> new File(i.toString()))
                        .collect(GuavaCollectors.immutableList());

                if (files.isEmpty()) {
                    ds3Common.getDs3TreeTableView().refresh();
                    return;
                }

                final String priority = (!savedJobPrioritiesStore.getJobSettings().getPutJobPriority().equals(resourceBundle.getString("defaultPolicyText"))) ? savedJobPrioritiesStore.getJobSettings().getPutJobPriority() : null;
                startPutJob(session, files, bucket, targetDir, priority,
                        jobInterruptionStore, treeItem);
            } else {
                LOG.info("No item selected from server side");
            }

        } catch (final Exception e) {
            LOG.error("Failed to transfer data to black pearl: ", e);
            loggingService.logMessage("Failed to transfer data to black pearl.", LogType.ERROR);
        }
    }

    private static boolean isEmptyFolder(final Path path) {
        if (Files.isDirectory(path)) {
            try {
                return Files.list(path).count() == 0;
            } catch (final IOException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    static private void refreshBlackPearlSideItem(final TreeItem<Ds3TreeTableValue> treeItem) {
        if (treeItem instanceof Ds3TreeTableItem) {
            LOG.info("Refresh row");
            final Ds3TreeTableItem ds3TreeTableItem = (Ds3TreeTableItem) treeItem;
            ds3TreeTableItem.refresh();
        } else {
            LOG.info("selected tree item is not the instance of Ds3TreeItem");
        }
    }

    private void changeRootDir(final String rootDir) {
        localPathIndicator.setText(rootDir);
        fileRootItem = rootDir;
        final Stream<FileTreeModel> rootItems = fileTreeTableProvider.getRoot(rootDir);
        if (rootItems != null) {
            try {
                final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
                rootTreeItem.setExpanded(true);
                treeTable.setShowRoot(false);
                rootItems.forEach(ftm -> {
                    final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(fileTreeTableProvider, ftm, workers);
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
        final Stream<FileTreeModel> rootItems = fileTreeTableProvider.getRoot(fileRootItem);
        localPathIndicator.setText(fileRootItem);
        rootItems.forEach(ftm -> {
            final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(fileTreeTableProvider, ftm, workers);
            rootTreeItem.getChildren().add(newRootTreeItem);
        });
        treeTable.setRoot(rootTreeItem);
        setExpandBehaviour(treeTable);
        if (lastExpandedNode != null) {
            if (treeTable.getRoot().getChildren().stream().anyMatch(i -> i.getValue().getName().equals(lastExpandedNode.getValue().getName()))) {
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
    private void startGetJob(final List<Ds3TreeTableValueCustom> listFiles,
                             final Session session,
                             final Path localPath,
                             final String priority,
                             final TreeItem<FileTreeModel> fileTreeItem) {
        final Ds3GetJob getJob = new Ds3GetJob(listFiles, localPath, session.getClient(),
                priority, settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads(), jobInterruptionStore,
                deepStorageBrowserPresenter, session, resourceBundle, loggingService);
        jobWorkers.execute(getJob);
        getJob.setOnSucceeded(e -> {
            LOG.info("Get Job completed successfully");
            refresh(fileTreeItem);
        });
        getJob.setOnFailed(event -> {
            LOG.error("Get Job failed");
            refresh(fileTreeItem);
        });
        getJob.setOnCancelled(cancelEvent -> {
            LOG.info("Get Job cancelled");
            try {
                //Cancellation of a job started
                final Ds3CancelSingleJobTask ds3CancelSingleJobTask = new Ds3CancelSingleJobTask(getJob.getJobId().toString(), endpointInfo, jobInterruptionStore, JobRequestType.GET.toString(), loggingService);
                workers.execute(ds3CancelSingleJobTask);
                ds3CancelSingleJobTask.setOnFailed(event -> LOG.error("Failed to cancel job"));
                ds3CancelSingleJobTask.setOnSucceeded(event -> {
                    loggingService.logMessage("GET Job Cancelled", LogType.INFO);
                    refresh(fileTreeItem);
                });

            } catch (final Exception e) {
                LOG.error("Failed to cancel job: ", e);
                loggingService.logMessage("Failed to cancel job.", LogType.ERROR);
            }
        });
    }

    private void startPutJob(final Session session,
                             final List<File> files,
                             final String bucket,
                             final String targetDir,
                             final String priority,
                             final JobInterruptionStore jobInterruptionStore,
                             final TreeItem<Ds3TreeTableValue> treeItem) {
        final Ds3PutJob putJob = new Ds3PutJob(session.getClient(), files, bucket, targetDir, priority,
                settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads(), jobInterruptionStore, deepStorageBrowserPresenter,
                session, settingsStore, loggingService, resourceBundle);
        jobWorkers.execute(putJob);
        putJob.setOnSucceeded(event -> {
            LOG.info("Succeed");
            refreshBlackPearlSideItem(treeItem);
        });
        putJob.setOnFailed(failEvent -> {
            LOG.error("Get Job failed");
            refreshBlackPearlSideItem(treeItem);
        });
        putJob.setOnCancelled(cancelEvent -> {
            LOG.info("Get Job cancelled");
            try {
                if (putJob.getJobId() != null) {
                    session.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(putJob.getJobId()));
                    loggingService.logMessage(resourceBundle.getString("putJobCancelled"), LogType.SUCCESS);
                    ParseJobInterruptionMap.removeJobID(jobInterruptionStore, putJob.getJobId().toString(), putJob.getDs3Client().getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter, loggingService);
                    refreshBlackPearlSideItem(treeItem);
                }
            } catch (final IOException e) {
                LOG.error("Failed to cancel job", e);
            }
        });

    }

    private void startMediaTask(final Stream<FileTreeModel> rootItems, final TreeItem<FileTreeModel> rootTreeItem, final Node oldPlaceHolder) {
        final GetMediaDeviceTask getMediaDeviceTask = new GetMediaDeviceTask(rootItems, rootTreeItem, fileTreeTableProvider, workers);
        workers.execute(getMediaDeviceTask);
        getMediaDeviceTask.setOnSucceeded(event -> {
            treeTable.setRoot(rootTreeItem);
            treeTable.setPlaceholder(oldPlaceHolder);
            setExpandBehaviour(treeTable);
            sizeColumn.setCellFactory(c -> new ValueTreeTableCell<FileTreeModel>());
            treeTable.sortPolicyProperty().set(new SortPolicyCallback(treeTable));
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
                loggingService.logMessage(resourceBundle.getString("refreshing")
                        + StringConstants.SPACE
                        + selectedItem.getParent().getValue().getName(), LogType.SUCCESS);
                selectedItem = selectedItem.getParent();
            } else {
                refreshFileTreeView();
            }
        } else {
            loggingService.logMessage(resourceBundle.getString("refreshing")
                    + StringConstants.SPACE
                    + selectedItem.getValue().getName(), LogType.SUCCESS);
        }
        if (selectedItem instanceof FileTreeTableItem) {
            final FileTreeTableItem fileTreeTableItem = (FileTreeTableItem) selectedItem;
            fileTreeTableItem.refresh();
        }

    }

    private Session getSession(final String sessionName) {

        final Optional<Session> first = ds3SessionStore.getSessions().filter(sessions -> (sessions.getSessionName()
                + StringConstants.SESSION_SEPARATOR + sessions.getEndpoint()).equals(sessionName)).findFirst();
        return first.orElse(null);
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
        } else if (!Files.isWritable(localPath)) {
            loggingService.logMessage("Canceling Job: Cannot write to folder " + localPath.toString(), LogType.ERROR);
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
