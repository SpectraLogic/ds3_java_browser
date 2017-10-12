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
import com.spectralogic.ds3client.Ds3Client;
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
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.*;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Presenter
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

    private final ResourceBundle resourceBundle;
    private final Ds3Common ds3Common;
    private final FileTreeTableProvider fileTreeTableProvider;
    private final DataFormat dataFormat;
    private final Workers workers;
    private final JobWorkers jobWorkers;
    private final JobInterruptionStore jobInterruptionStore;
    private final EndpointInfo endpointInfo;
    private final LoggingService loggingService;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final Ds3GetJob.Ds3GetJobFactory ds3GetJobFactory;
    private final DateTimeUtils dateTimeUtils;
    private final DataFormat local = new DataFormat("local");
    private final LazyAlert alert;
    private final Ds3PutJob.Ds3PutJobFactory ds3PutJobFactory;

    private String fileRootItem = StringConstants.ROOT_LOCATION;

    private TreeItem<FileTreeModel> lastExpandedNode;

    @Inject
    public LocalFileTreeTablePresenter(final ResourceBundle resourceBundle,
            final Ds3Common ds3Common,
            final FileTreeTableProvider fileTreeTableProvider,
            final DataFormat dataFormat,
            final Workers workers,
            final JobWorkers jobWorkers,
            final JobInterruptionStore jobInterruptionStore,
            final EndpointInfo endpointInfo,
            final Ds3GetJob.Ds3GetJobFactory ds3GetJobFactory,
            final LoggingService loggingService,
            final DateTimeUtils dateTimeUtils,
            final Ds3PutJob.Ds3PutJobFactory ds3PutJobFactory,
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        this.resourceBundle = resourceBundle;
        this.ds3Common = ds3Common;
        this.fileTreeTableProvider = fileTreeTableProvider;
        this.dataFormat = dataFormat;
        this.workers = workers;
        this.jobWorkers = jobWorkers;
        this.jobInterruptionStore = jobInterruptionStore;
        this.endpointInfo = endpointInfo;
        this.loggingService = loggingService;
        this.ds3GetJobFactory = ds3GetJobFactory;
        this.ds3PutJobFactory = ds3PutJobFactory;
        this.dateTimeUtils = dateTimeUtils;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.alert = new LazyAlert(resourceBundle);
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.debug("Initialize...");
            initToolTips();
            initTableView();
            initListeners();
            initProgressAndPathIndicators();
        } catch (final Throwable t) {
            LOG.error("Encountered an error when initializing LocalFileTreeTablePresenter", t);
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
        treeTable.setOnDragEntered(SafeHandler.logHandle(event -> event.acceptTransferModes(TransferMode.COPY)));
        treeTable.setOnDragOver(SafeHandler.logHandle(event -> event.acceptTransferModes(TransferMode.COPY)));
        treeTable.setOnDragDropped(SafeHandler.logHandle(event -> {
            setDragDropEvent(null, event);
            event.consume();
        }));
        treeTable.setRowFactory(view -> {
                    final TreeTableRow<FileTreeModel> row = new TreeTableRow<>();
                    final List<String> rowNameList = new ArrayList<>();
                    row.setOnMouseClicked(SafeHandler.logHandle(event -> {
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
                    }));
                    row.setOnDragDropped(SafeHandler.logHandle(event -> {
                        LOG.info("Drop detected..");
                        if (row.getTreeItem() != null && !row.getTreeItem().isLeaf() && !row.getTreeItem().isExpanded()) {
                            LOG.info("Expanding closed row");
                            row.getTreeItem().setExpanded(true);
                        }
                        setDragDropEvent(row, event);
                        event.consume();

                    }));
                    row.setOnDragOver(SafeHandler.logHandle(event -> {
                        final TreeItem<FileTreeModel> treeItem = row.getTreeItem();
                        if (event.getGestureSource() != treeTable && event.getDragboard().hasFiles()) {
                            event.acceptTransferModes(TransferMode.COPY);
                            if (treeItem == null && fileRootItem.equals(StringConstants.ROOT_LOCATION)) {
                                event.acceptTransferModes(TransferMode.NONE);
                            }
                            event.consume();
                        }
                    }));
                    row.setOnDragEntered(SafeHandler.logHandle(event -> {
                        final TreeItem<FileTreeModel> treeItem = row.getTreeItem();
                        if (treeItem != null) {
                            final InnerShadow is = new InnerShadow();
                            is.setOffsetY(1.0f);
                            row.setEffect(is);
                        } else {
                            event.acceptTransferModes(TransferMode.NONE);
                        }
                        event.consume();
                    }));
                    row.setOnDragExited(SafeHandler.logHandle(event -> {
                        row.setEffect(null);
                        event.consume();
                    }));
                    row.setOnDragDetected(SafeHandler.logHandle(event -> {
                        LOG.info("Drag detected...");
                        final ObservableList<TreeItem<FileTreeModel>> selectedItems = treeTable.getSelectionModel().getSelectedItems();
                        if (!Guard.isNullOrEmpty(selectedItems)) {
                            LOG.info("Starting drag and drop event");
                            final Dragboard db = treeTable.startDragAndDrop(TransferMode.COPY);
                            final ClipboardContent content = new ClipboardContent();
                            final ImmutableList<Pair<String, String>> selectedModels = selectedItems.stream()
                                    .map(TreeItem::getValue)
                                    .filter(siValue -> siValue.getName() != null)
                                    .filter(siValue -> siValue.getPath() != null)
                                    .map(si -> new Pair<>(si.getName(), si.getPath().toAbsolutePath().toString()))
                                    .collect(GuavaCollectors.immutableList());
                            content.put(local, selectedModels);
                            db.setContent(content);
                        }
                        event.consume();
                    }));
                    return row;
                }
        );

        treeTable.focusedProperty().addListener((observable, oldValue, newValue) -> {
            this.deepStorageBrowserPresenter.getSelectAllMenuItem().setDisable(oldValue);
        });
    }

    private void initListeners() {
        refreshButton.setOnAction(SafeHandler.logHandle(event -> refreshFileTreeView()));
        homeButton.setOnAction(SafeHandler.logHandle(event -> changeRootDir(System.getProperty(StringConstants.SETTING_FILE_PATH))));
        toMyComputer.setOnAction(SafeHandler.logHandle(event -> changeRootDir(StringConstants.ROOT_LOCATION)));
        transferButton.setOnAction(SafeHandler.logHandle(event -> transferToBlackPearl()));
        parentDirectoryButton.setOnAction(SafeHandler.logHandle(event -> goToParentDirectory()));
    }

    private void initProgressAndPathIndicators() {
        final Stream<FileTreeModel> rootItems = fileTreeTableProvider.getRoot(fileRootItem, dateTimeUtils);
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

    private ImmutableList<Pair<String, Path>> getLocalFilesToPut() {
        final ObservableList<TreeItem<FileTreeModel>> currentLocalSelection = treeTable.getSelectionModel().getSelectedItems();
        final ImmutableList<Pair<String, Path>> files = currentLocalSelection
                .stream()
                .map(TreeItem::getValue)
                .filter(Objects::nonNull)
                .map(i -> new Pair<>(i.getName(), i.getPath()))
                .collect(GuavaCollectors.immutableList());
        if (files.isEmpty()) {
            ds3Common.getDs3TreeTableView().refresh();
            return null;
        }
        return files;
    }

    private TreeItem<Ds3TreeTableValue> getRemoteDestination() {
        final ImmutableList<TreeItem<Ds3TreeTableValue>> currentRemoteSelection = ds3Common.getDs3TreeTableView().getSelectionModel().getSelectedItems()
                .stream().collect(GuavaCollectors.immutableList());

        //Finding root in case of double click to get selected items
        if (Guard.isNullOrEmpty(currentRemoteSelection)) {
            //If no destination selected, attempt to use current remote root
            final TreeItem<Ds3TreeTableValue> root = ds3Common.getDs3TreeTableView().getRoot();
            if (root != null && root.getValue() != null) {
                LOG.info("No remote selection, using remote root");
                return root;
            } else {
                return null;
            }
        } else if (currentRemoteSelection.size() > 1) {
            alert.error("multipleDestError");
            return null;
        }

        return currentRemoteSelection.stream().findFirst().orElse(null);
    }

    private void transferToBlackPearl() {
        if (ds3Common.getCurrentSession() == null) {
            LOG.error("No valid session to initiate BULK_PUT");
            alert.error("noSession");
            return;
        }
        final Session session = ds3Common.getCurrentSession();

        final TreeItem<Ds3TreeTableValue> remoteDestination = getRemoteDestination(); // The TreeItem is required to refresh the view
        if (remoteDestination == null || remoteDestination.getValue() == null) {
            alert.info("selectDestination");
            return;
        } else if (remoteDestination.getValue().isSearchOn()) {
            alert.info("operationNotAllowed");
            return;
        } else if (!remoteDestination.isExpanded()) {
            remoteDestination.setExpanded(true);
        }

        final Ds3TreeTableValue remoteDestinationValue = remoteDestination.getValue();
        final String bucket = remoteDestinationValue.getBucketName();
        final String targetDir = remoteDestinationValue.getDirectoryName();
        LOG.info("Passing new Ds3PutJob to jobWorkers thread pool to be scheduled");

        // Get local files to PUT
        final ImmutableList<Pair<String, Path>> filesToPut = getLocalFilesToPut();
        if (Guard.isNullOrEmpty(filesToPut)) {
            alert.info("fileSelect");
            return;
        }

        startPutJob(session, filesToPut, bucket, targetDir, jobInterruptionStore, remoteDestination);
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
        final Stream<FileTreeModel> rootItems = fileTreeTableProvider.getRoot(rootDir, dateTimeUtils);
        if (rootItems != null) {
            final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
            rootTreeItem.setExpanded(true);
            treeTable.setShowRoot(false);
            rootItems.forEach(ftm -> {
                final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(fileTreeTableProvider, ftm, dateTimeUtils, workers);
                rootTreeItem.getChildren().add(newRootTreeItem);
            });
            treeTable.setRoot(rootTreeItem);
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
        final Stream<FileTreeModel> rootItems = fileTreeTableProvider.getRoot(fileRootItem, dateTimeUtils);
        localPathIndicator.setText(fileRootItem);
        rootItems.forEach(ftm -> {
            final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(fileTreeTableProvider, ftm, dateTimeUtils, workers);
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
            treeTable.requestFocus(); // Update panel and scroll bar if focus shifted
        }));
    }

    /**
     * Select multiple items using shift and ctrl
     *
     * @param rowNameList selectMultipleItemslist of selected Items
     * @param row         select row
     */
    private void selectMultipleItems(final List<String> rowNameList, final TreeTableRow<FileTreeModel> row) {
        if(row == null || row.getTreeItem() == null || row.getTreeItem().getValue() == null || row.getTreeItem().getValue().getName() == null) {
            return;
        }
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
     * @param listFiles list of files selected for drag and drop
     * @param localPath path where selected files need to transfer
     */
    private void startGetJob(final List<Ds3TreeTableValueCustom> listFiles,
            final Path localPath) {
        final Ds3GetJob getJob = ds3GetJobFactory.createDs3GetJob(listFiles, localPath);
        getJob.setOnSucceeded(SafeHandler.logHandle(event -> {
            LOG.info("Get Job completed successfully");
            refreshFileTreeView();
        }));
        getJob.setOnFailed(SafeHandler.logHandle(event -> {
            final Throwable exception = event.getSource().getException();
            LOG.error("Get Job failed", exception);
            loggingService.logMessage("Get Job failed with message: " + exception.getMessage(), LogType.ERROR);
            refreshFileTreeView();
        }));
        getJob.setOnCancelled(SafeHandler.logHandle(cancelEvent -> {
            //Cancellation of a job started
            final Ds3CancelSingleJobTask ds3CancelSingleJobTask = new Ds3CancelSingleJobTask(getJob.getJobId().toString(), endpointInfo, jobInterruptionStore, JobRequestType.GET.toString(), loggingService);
            ds3CancelSingleJobTask.setOnFailed(SafeHandler.logHandle(event -> LOG.error("Failed to cancel job")));
            ds3CancelSingleJobTask.setOnSucceeded(SafeHandler.logHandle(event -> {
                LOG.info("Get Job cancelled");
                loggingService.logMessage("GET Job Cancelled", LogType.INFO);
                refreshFileTreeView();
            }));
            workers.execute(ds3CancelSingleJobTask);
        }));
        jobWorkers.execute(getJob);
    }

    private void startPutJob(final Session session,
            final List<Pair<String, Path>> files,
            final String bucket,
            final String targetDir,
            final JobInterruptionStore jobInterruptionStore,
            final TreeItem<Ds3TreeTableValue> remoteDestination) {
        final Ds3Client client = session.getClient();
        final Ds3PutJob putJob = ds3PutJobFactory.createDs3PutJob(files, bucket, targetDir, remoteDestination);
        putJob.setOnSucceeded(SafeHandler.logHandle(event -> {
            LOG.info("BULK_PUT job {} Succeed.", putJob.getJobId());
            refreshBlackPearlSideItem(remoteDestination);
        }));
        putJob.setOnFailed(SafeHandler.logHandle(failEvent -> {
            final Throwable throwable = failEvent.getSource().getException();
            LOG.error("Put Job Failed", throwable);
            loggingService.logMessage("Put Job Failed with message: " + throwable.getClass().getName() + ": " + throwable.getMessage(), LogType.ERROR);
        }));
        putJob.setOnCancelled(SafeHandler.logHandle(cancelEvent -> {
            final UUID jobId = putJob.getJobId();
            if (jobId != null) {
                try {
                    client.cancelJobSpectraS3(new CancelJobSpectraS3Request(putJob.getJobId()));
                } catch (final IOException e) {
                    LOG.error("Failed to cancel job", e);
                }
                LOG.info("BULK_PUT job {} Cancelled.", jobId);
                loggingService.logMessage(resourceBundle.getString("putJobCancelled"), LogType.SUCCESS);

                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(),
                        client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter, loggingService);

                refreshBlackPearlSideItem(remoteDestination);
            } else {
                LOG.error("Failed to cancel job with invalid ID");
            }
        }));
        jobWorkers.execute(putJob);
    }

    private void startMediaTask(final Stream<FileTreeModel> rootItems, final TreeItem<FileTreeModel> rootTreeItem, final Node oldPlaceHolder) {
        final GetMediaDeviceTask getMediaDeviceTask = new GetMediaDeviceTask(rootItems, rootTreeItem, fileTreeTableProvider, dateTimeUtils, workers);
        getMediaDeviceTask.setOnSucceeded(SafeHandler.logHandle(event -> {
            treeTable.setRoot(rootTreeItem);
            treeTable.setPlaceholder(oldPlaceHolder);
            setExpandBehaviour(treeTable);
            sizeColumn.setCellFactory(c -> new ValueTreeTableCell<>());
            treeTable.sortPolicyProperty().set(new SortPolicyCallback(treeTable));
        }));
        workers.execute(getMediaDeviceTask);

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

            @SuppressWarnings("unchecked") final List<Ds3TreeTableValueCustom> list = (List<Ds3TreeTableValueCustom>) db.getContent(dataFormat);
            startGetJob(list, localPath);
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
