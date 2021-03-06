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
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobService.factories.GetJobFactory;
import com.spectralogic.dsbrowser.gui.services.jobService.factories.PutJobFactory;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.*;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
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
import javafx.stage.Window;
import javafx.util.Pair;
import kotlin.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
@Singleton
@Presenter
public class LocalFileTreeTablePresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(LocalFileTreeTablePresenter.class);
    private static final String LOCAL = "local";

    @FXML
    private TreeTableView<FileTreeModel> treeTable;

    @FXML
    private TreeTableColumn<FileTreeModel, Number> sizeColumn;

    @FXML
    private TreeTableColumn<FileTreeModel, String> dateModified;

    @FXML
    private TreeTableColumn<FileTreeModel, String> nameColumn;

    @FXML
    private Button homeButton, refreshButton, toMyComputer, transferButton, parentDirectoryButton, createFolderButton;

    @FXML
    private Tooltip homeButtonTooltip, refreshButtonTooltip, toMyComputerTooltip, transferButtonTooltip, parentDirectoryButtonTooltip, createFolderButtonTooltip;

    @FXML
    private Label localPathIndicator;

    private final ResourceBundle resourceBundle;
    private final Ds3Common ds3Common;
    private final FileTreeTableProvider fileTreeTableProvider;
    private final DataFormat dataFormat;
    private final Workers workers;
    private final LoggingService loggingService;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final DataFormat local = new DataFormat(LOCAL);
    private final AlertService alert;
    private final DateTimeUtils dateTimeUtils;
    private final PutJobFactory putJobFactory;
    private final GetJobFactory getJobFactory;
    private final FileTreeTableItem.FileTreeTableItemFactory fileTreeTableItemFactory;
    private final GetMediaDeviceTask.GetMediaDeviceTaskFactory getMediaDeviceTaskFactory;

    private String fileRootItem = StringConstants.ROOT_LOCATION;

    private TreeItem<FileTreeModel> lastExpandedNode;

    @Inject
    public LocalFileTreeTablePresenter(final ResourceBundle resourceBundle,
            final Ds3Common ds3Common,
            final FileTreeTableProvider fileTreeTableProvider,
            final DataFormat dataFormat,
            final Workers workers,
            final LoggingService loggingService,
            final PutJobFactory putJobFactory,
            final FileTreeTableItem.FileTreeTableItemFactory fileTreeTableItemFactory,
            final GetJobFactory getJobFactory,
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
            final DateTimeUtils dateTimeUtils,
            final AlertService alertService,
            final GetMediaDeviceTask.GetMediaDeviceTaskFactory getMediaDeviceTaskFactory) {
        this.resourceBundle = resourceBundle;
        this.ds3Common = ds3Common;
        this.dateTimeUtils = dateTimeUtils;
        this.fileTreeTableProvider = fileTreeTableProvider;
        this.dataFormat = dataFormat;
        this.workers = workers;
        this.loggingService = loggingService;
        this.fileTreeTableItemFactory = fileTreeTableItemFactory;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.putJobFactory = putJobFactory;
        this.getJobFactory = getJobFactory;
        this.getMediaDeviceTaskFactory = getMediaDeviceTaskFactory;
        this.alert = alertService;
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
        createFolderButtonTooltip.setText(resourceBundle.getString("ds3NewFolderToolTip"));
    }

    private void initTableView() {
        ds3Common.setLocalFileTreeTablePresenter(this);
        treeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeTable.setOnDragEntered(SafeHandler.logHandle(event -> event.acceptTransferModes(TransferMode.COPY)));
        treeTable.setOnDragOver(SafeHandler.logHandle(event -> event.acceptTransferModes(TransferMode.COPY)));
        treeTable.setOnDragDropped(SafeHandler.logHandle(event -> {
            setDragDropEvent(null, event);
            event.consume();
        }));
        dateModified.setComparator(Comparator.comparing(dateTimeUtils::stringAsDate));
        nameColumn.setComparator(Comparator.comparing(String::toLowerCase));
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
                                    .filter(Objects::nonNull)
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
        homeButton.setOnAction(SafeHandler.logHandle(event -> changeRootDir(System.getProperty(StringConstants.USER_HOME))));
        toMyComputer.setOnAction(SafeHandler.logHandle(event -> changeRootDir(StringConstants.ROOT_LOCATION)));
        transferButton.setOnAction(SafeHandler.logHandle(event -> transferToBlackPearl()));
        parentDirectoryButton.setOnAction(SafeHandler.logHandle(event -> goToParentDirectory()));
        createFolderButton.setOnAction(SafeHandler.logHandle(event -> createFolder()));
    }

    private void createFolder() {
        if (fileRootItem.equals("My Computer")) {
            alert.error("specifyDirectory", getWindow());
            return;
        }
        final Path rootPath = Paths.get(fileRootItem);
        final TextInputDialog inputDialog = new TextInputDialog();
        inputDialog.initOwner(ds3Common.getWindow());
        inputDialog.setContentText(resourceBundle.getString("createLocalFolder"));
        inputDialog.setGraphic(null);
        inputDialog.setHeaderText(StringConstants.EMPTY_STRING);
        inputDialog.setTitle(resourceBundle.getString("createFolder"));
        final Optional<String> results = inputDialog.showAndWait();
        if (results.isPresent()) {
            final String folderName = results.get();
            if (Guard.isStringNullOrEmpty(folderName)) {
                alert.error("cannotCreateFolderWithoutName", getWindow());
                return;
            }
            try {
                final Path path;
                final ObservableList<TreeItem<FileTreeModel>> selectedItems = ds3Common.getLocalTreeTableView().getSelectionModel().getSelectedItems();
                if(selectedItems.isEmpty()) {
                    path = rootPath.resolve(folderName);
                } else if (selectedItems.size() == 1) {
                    final FileTreeModel fileTreeModel = selectedItems.get(0).getValue();
                    if (fileTreeModel.getType() == BaseTreeModel.Type.Directory) {
                        path = fileTreeModel.getPath().resolve(folderName);
                    } else {
                        path = fileTreeModel.getPath().getParent().resolve(folderName);
                    }
                } else {
                   alert.error("tooManyItemsSelected", getWindow());
                   return;
                }
                Files.createDirectories(path);
                refreshFileTreeView();
            } catch (final IOException e) {
                alert.error("couldNotCreateLocalDirectory", getWindow());
                loggingService.logMessage(resourceBundle.getString("couldNotCreateLocalDirectory"), LogType.ERROR);
                LOG.error("Could not create directory in " + rootPath.toString(), LogType.ERROR);
            }
        }
    }

    private void initProgressAndPathIndicators() {
        final String userHome = System.getProperty(StringConstants.USER_HOME);
        fileRootItem = userHome;
        final Stream<FileTreeModel> rootItems = fileTreeTableProvider.getRoot(userHome);
        localPathIndicator.setText(userHome);
        final Node oldPlaceHolder = treeTable.getPlaceholder();
        final ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(Constants.PROGRESS_BAR_SIZE, Constants.PROGRESS_BAR_SIZE);
        treeTable.setPlaceholder(new StackPane(progress));
        final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTable.setShowRoot(false);
        UIThreadUtil.runInFXThread(() -> {
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

    private ImmutableList<kotlin.Pair<String, Path>> getLocalFilesToPut() {
        final ObservableList<TreeItem<FileTreeModel>> currentLocalSelection = treeTable.getSelectionModel().getSelectedItems();
        final ImmutableList<kotlin.Pair<String, Path>> files = currentLocalSelection
                .stream()
                .filter(Objects::nonNull)
                .map(TreeItem::getValue)
                .filter(Objects::nonNull)
                .map(i -> new kotlin.Pair<>(i.getName(), i.getPath()))
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
            alert.error("multipleDestError", getWindow());
            return null;
        }

        return currentRemoteSelection.stream().findFirst().orElse(null);
    }

    private void transferToBlackPearl() {
        final Session session = ds3Common.getCurrentSession();
        if (session == null) {
            LOG.error("No valid session to initiate Put");
            alert.error("noSession", getWindow());
            return;
        }

        final TreeItem<Ds3TreeTableValue> remoteDestination = getRemoteDestination(); // The TreeItem is required to refresh the view
        if (remoteDestination == null || remoteDestination.getValue() == null) {
            alert.info("selectDestination", getWindow());
            return;
        } else if (remoteDestination.getValue().isSearchOn()) {
            alert.info("operationNotAllowed", getWindow());
            return;
        } else if (!remoteDestination.isExpanded()) {
            remoteDestination.setExpanded(true);
        }

        final Ds3TreeTableValue remoteDestinationValue = remoteDestination.getValue();
        final String bucket = remoteDestinationValue.getBucketName();
        final String targetDir = remoteDestinationValue.getDirectoryName();
        LOG.info("Passing new Ds3PutJob to jobWorkers thread pool to be scheduled");

        // Get local files to PUT
        final ImmutableList<kotlin.Pair<String, Path>> filesToPut = getLocalFilesToPut();
        if (Guard.isNullOrEmpty(filesToPut)) {
            alert.info("fileSelect", getWindow());
            return;
        }

        startPutJob(session.getClient(), filesToPut, bucket, targetDir, remoteDestination);
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
        final ImmutableList<TreeTableColumn<FileTreeModel, ?>> sortOrder = treeTable.getSortOrder().stream().collect(GuavaCollectors.immutableList());
        localPathIndicator.setText(rootDir);
        fileRootItem = rootDir;
        final Stream<FileTreeModel> rootItems = fileTreeTableProvider.getRoot(rootDir);
        if (rootItems != null) {
            final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
            rootTreeItem.setExpanded(true);
            treeTable.setShowRoot(false);
            rootItems.forEach(ftm -> {
                final TreeItem<FileTreeModel> newRootTreeItem = fileTreeTableItemFactory.create(ftm);
                rootTreeItem.getChildren().add(newRootTreeItem);
            });
            treeTable.setRoot(rootTreeItem);
            treeTable.getSelectionModel().clearSelection();
            treeTable.getSortOrder().addAll(sortOrder);
        } else {
            LOG.info("Already at root directory");
        }
    }

    public void refreshFileTreeView() {
        LOG.info("Starting refreshing local file tree");
        final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTable.setShowRoot(false);
        final Stream<FileTreeModel> rootItems = fileTreeTableProvider.getRoot(fileRootItem);
        localPathIndicator.setText(fileRootItem);
        rootItems.forEach(ftm -> {
            final TreeItem<FileTreeModel> newRootTreeItem = fileTreeTableItemFactory.create(ftm);
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
        if (row == null || row.getTreeItem() == null || row.getTreeItem().getValue() == null || row.getTreeItem().getValue().getName() == null) {
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
            final Path localPath, final Ds3Client client) {
        if (listFiles.stream().anyMatch(value -> value.getType() == BaseTreeModel.Type.Bucket)) {
            LOG.error("Cannot perform GET on bucket");
            alert.error("noGetBucket", getWindow());
            return;
        }
        listFiles.stream()
                .map(Ds3TreeTableValueCustom::getBucketName)
                .distinct()
                .forEach(bucket -> {
                    final ImmutableList<kotlin.Pair<String, String>> fileAndParent = listFiles.stream()
                            .filter(ds3TreeTableValueCustom -> Objects.equals(ds3TreeTableValueCustom.getBucketName(), bucket))
                            .map(ds3 -> new kotlin.Pair<>( ds3.getFullName(), ds3.getParent())) .collect(GuavaCollectors.immutableList());
                    getJobFactory.create(ds3Common.getCurrentSession(), fileAndParent, bucket, localPath, client, () -> { refreshFileTreeView(); return Unit.INSTANCE;}, null);
                });
    }

    private void startPutJob(final Ds3Client client,
            final List<kotlin.Pair<String, Path>> files,
            final String bucket,
            final String targetDir,
            final TreeItem<Ds3TreeTableValue> remoteDestination) {
        putJobFactory.create(ds3Common.getCurrentSession(), files, bucket, targetDir, client, () -> {
            refreshBlackPearlSideItem(remoteDestination);
            return Unit.INSTANCE;
        });
    }

    private void startMediaTask(final Stream<FileTreeModel> rootItems, final TreeItem<FileTreeModel> rootTreeItem, final Node oldPlaceHolder) {
        final GetMediaDeviceTask getMediaDeviceTask = getMediaDeviceTaskFactory.create(rootItems, rootTreeItem);
        getMediaDeviceTask.setOnSucceeded(SafeHandler.logHandle(event -> {
            treeTable.setRoot(rootTreeItem);
            treeTable.setPlaceholder(oldPlaceHolder);
            setExpandBehaviour(treeTable);
            sizeColumn.setCellFactory(c -> new ValueTreeTableCell<>());
            treeTable.setSortMode(TreeSortMode.ALL_DESCENDANTS);
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
            startGetJob(list, localPath, ds3Common.getCurrentSession().getClient());
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
            } else if (fileTreeItem != null && fileTreeItem.getValue().getType().equals(FileTreeModel.Type.File)) {
                localPath = fileTreeItem.getParent().getValue().getPath();
            } else if (!fileRootItem.equals(StringConstants.ROOT_LOCATION)) {
                localPath = Paths.get(fileRootItem);
            }

        } else {
            if (!fileRootItem.equals(StringConstants.ROOT_LOCATION)) {
                localPath = Paths.get(fileRootItem);
            }
        }
        return localPath;
    }

    public Window getWindow() {
        return treeTable.getScene().getWindow();
    }
}
