package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Response;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalFileTreeTablePresenter implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileTreeTablePresenter.class);

    private static final String ROOT_LOCATION = "My Computer";

    private final Alert ALERT = new Alert(Alert.AlertType.INFORMATION);

    @FXML
    private TreeTableView<FileTreeModel> treeTable;

    @FXML
    private Button homeButton, refreshButton, toMyComputer, transferButton, parentDirectoryButton;

    @FXML
    private Tooltip homeButtonTooltip, refreshButtonTooltip, toMyComputerTooltip, transferButtonTooltip, parentDirectoryButtonTooltip;

    @FXML
    private Label localPathIndicator;

    @Inject
    private LocalFileTreeTableProvider provider;

    @Inject
    private DataFormat dataFormat;

    @Inject
    private Workers workers;

    @Inject
    private JobWorkers jobWorkers;

    @Inject
    private Ds3SessionStore store;

    @Inject
    private DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    @Inject
    private ResourceBundle resourceBundle;

    @Inject
    private SavedJobPrioritiesStore savedJobPrioritiesStore;

    @Inject
    private SettingsStore settingsStore;

    private String fileRootItem = ROOT_LOCATION;

    private TreeItem<FileTreeModel> currentRootTreeItem;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Starting LocalFileTreeTablePresenter");
            ALERT.setTitle("Error");
            ALERT.setHeaderText(null);
            initGUIElements();
            initTableView();
            initListeners();
        } catch (final Throwable e) {
            LOG.error("Encountered an error when creating LocalFileTreeTablePresenter", e);
        }
    }

    private void initGUIElements() {
        transferButtonTooltip.setText(resourceBundle.getString("transferButtonTooltip"));
        homeButtonTooltip.setText(resourceBundle.getString("homeButtonTooltip"));
        refreshButtonTooltip.setText(resourceBundle.getString("refreshButtonTooltip"));
        toMyComputerTooltip.setText(resourceBundle.getString("toMyComputerTooltip"));
        parentDirectoryButtonTooltip.setText(resourceBundle.getString("parentDirectoryButtonTooltip"));
    }

    private void initListeners() {
        refreshButton.setOnAction(event -> refreshFileTreeView());
        homeButton.setOnAction(event -> changeRootDir(System.getProperty("user.home")));
        toMyComputer.setOnAction(event -> changeRootDir(ROOT_LOCATION));
        transferButton.setOnAction(event -> transferToBlackPearl());
        parentDirectoryButton.setOnAction(event -> goToParentDirectory());
    }

    private void goToParentDirectory() {

        if (!localPathIndicator.getText().equals(ROOT_LOCATION)) {
            if (Paths.get(fileRootItem).getParent() != null) {
                changeRootDir(Paths.get(fileRootItem).getParent().toString());
            } else {
                changeRootDir(ROOT_LOCATION);
            }
        }
    }

    private void transferToBlackPearl() {
        try {
            final ObservableList<TreeItem<FileTreeModel>> currentSelection = treeTable.getSelectionModel().getSelectedItems();
            if (currentSelection.isEmpty()) {
                ALERT.setContentText("Select files to transfer");
                ALERT.showAndWait();
                return;
            }

            final ObservableList<javafx.scene.Node> list = deepStorageBrowserPresenter.getBlackPearl().getChildren();
            final VBox vbox = (VBox) list.stream().filter(i -> i instanceof VBox).findFirst().get();
            final ObservableList<javafx.scene.Node> children = vbox.getChildren();
            @SuppressWarnings("unchecked")
            final TabPane tabPane = (TabPane) children.stream().filter(i -> i instanceof TabPane).findFirst().get();
            final VBox vboxBP = (VBox) tabPane.getSelectionModel().getSelectedItem().getContent();
            @SuppressWarnings("unchecked")
            final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = (TreeTableView<Ds3TreeTableValue>) vboxBP.getChildren().stream().filter(i -> i instanceof TreeTableView).findFirst().get();
            final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView.getSelectionModel().getSelectedItems()
                    .stream().collect(GuavaCollectors.immutableList());

            if (values.isEmpty()) {
                ALERT.setContentText("Select destination location.");
                ALERT.showAndWait();
                return;
            }

            if (values.size() > 1) {
                ALERT.setContentText("Multiple destination not allowed. Please select one.");
                ALERT.showAndWait();
                return;
            }

            if (values.stream().findFirst().get().getValue().isSearchOn()) {
                ALERT.setContentText("Operation not allowed here");
                ALERT.showAndWait();
                return;
            }

            final TreeItem<Ds3TreeTableValue> treeItem = values.get(0);
            if (!treeItem.isExpanded()) {
                treeItem.setExpanded(true);
            }
            final Ds3TreeTableValue value = treeItem.getValue();
            final String bucket = value.getBucketName();
            final String targetDir = value.getDirectoryName();
            LOG.info("Passing new Ds3PutJob to jobWorkers thread pool to be scheduled");

            final Session session = getSession(tabPane.getSelectionModel().getSelectedItem().getText());

            final List<File> files = currentSelection
                    .stream()
                    .map(i -> new File(i.getValue().getPath().toString()))
                    .collect(Collectors.toList());

            final String priority = (!savedJobPrioritiesStore.getJobSettings().getPutJobPriority().equals(resourceBundle.getString("defaultPolicyText"))) ? savedJobPrioritiesStore.getJobSettings().getPutJobPriority() : null;

            final Ds3PutJob putJob = new Ds3PutJob(session.getClient(), files, bucket, targetDir, deepStorageBrowserPresenter, priority, settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads());

            jobWorkers.execute(putJob);
            putJob.setOnSucceeded(event -> {
                LOG.info("Succeed");
                refreshBlackPearlSideItem(treeItem);
            });

            putJob.setOnFailed(e -> {
                LOG.info("Get Job failed");
                refreshBlackPearlSideItem(treeItem);
            });

            putJob.setOnCancelled(e -> {
                LOG.info("Get Job cancelled");
                try {
                    final CancelJobSpectraS3Response cancelJobSpectraS3Response = session.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(putJob.getJobId()).withForce(true));
                    deepStorageBrowserPresenter.logText("PUT Job Cancelled. Response code:" + cancelJobSpectraS3Response.getResponse().getStatusCode(), LogType.SUCCESS);
                    refreshBlackPearlSideItem(treeItem);
                } catch (IOException e1) {
                    deepStorageBrowserPresenter.logText("Failed to cancel job", LogType.ERROR);
                }
            });

        } catch (final Exception e) {
            deepStorageBrowserPresenter.logText("Something went wrong. Reason: " + e.toString(), LogType.ERROR);
        }
    }

    private void refreshBlackPearlSideItem(final TreeItem<Ds3TreeTableValue> treeItem) {
        if (treeItem instanceof Ds3TreeTableItem) {
            LOG.info("Refresh row");
            final Ds3TreeTableItem ds3TreeTableItem = (Ds3TreeTableItem) treeItem;
            ds3TreeTableItem.refresh();
        }
    }

    private void changeRootDir(final String rootDir) {
        localPathIndicator.setText(rootDir);
        fileRootItem = rootDir;
        localPathIndicator.setText(rootDir);
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
                e.printStackTrace();
            }
            treeTable.getSelectionModel().clearSelection();
        }
    }

    private void refreshFileTreeView() {
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
    }

    public LocalFileTreeTablePresenter() {
        super();
    }

    public TreeTableView<FileTreeModel> getTreeTable() {
        return treeTable;
    }

    private void initTableView() {

        treeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        treeTable.setOnDragEntered(event -> {
            event.acceptTransferModes(TransferMode.COPY);

        });

        treeTable.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.COPY);

        });

        treeTable.setOnDragDropped(event -> {

            final Dragboard db = event.getDragboard();

            if (db.hasContent(dataFormat)) {
                LOG.info("Drop event contains files");

                Path localPath = null;

                if (!fileRootItem.equals(ROOT_LOCATION)) {
                    localPath = Paths.get(fileRootItem);
                } else {
                    return;
                }

                @SuppressWarnings("unchecked")
                final List<Ds3TreeTableValueCustom> list = (List<Ds3TreeTableValueCustom>) db.getContent(dataFormat);

                final Session session = getSession(db.getString());

                final String priority = (!savedJobPrioritiesStore.getJobSettings().getGetJobPriority().equals(resourceBundle.getString("defaultPolicyText"))) ? savedJobPrioritiesStore.getJobSettings().getGetJobPriority() : null;

                final Ds3GetJob getJob = new Ds3GetJob(list, localPath, session.getClient(),
                        deepStorageBrowserPresenter, priority, settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads());

                jobWorkers.execute(getJob);

                getJob.setOnSucceeded(e -> {
                    LOG.info("Get Job completed successfully");
                    refreshFileTreeView();
                });

                getJob.setOnCancelled(e -> {
                    try {
                        final CancelJobSpectraS3Response cancelJobSpectraS3Response = session.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(getJob.getJobId()).withForce(true));
                        deepStorageBrowserPresenter.logText("GET Job Cancelled. Response code:" + cancelJobSpectraS3Response.getResponse().getStatusCode(), LogType.ERROR);
                        refreshFileTreeView();
                    } catch (IOException e1) {
                        deepStorageBrowserPresenter.logText(" Failed to cancel job. ", LogType.ERROR);
                    }
                });
            }
            event.consume();
        });

        treeTable.setRowFactory(view -> {
                    final TreeTableRow<FileTreeModel> row = new TreeTableRow<FileTreeModel>();

                    row.setOnMouseClicked(event -> {

                        if (event.isControlDown()) {
                            if (!row.isSelected()) {
                                treeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                                treeTable.getSelectionModel().clearSelection(row.getIndex());
                            }
                        } else if (event.getClickCount() == 2) {
                            if (!row.getTreeItem().getValue().getType().equals(FileTreeModel.Type.File)) {
                                changeRootDir(treeTable.getSelectionModel().getSelectedItem().getValue().getPath().toString());
                            }
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

                        final Dragboard db = event.getDragboard();

                        if (db.hasContent(dataFormat)) {
                            LOG.info("Drop event contains files");
                            // get bucket info and current path
                            final TreeItem<FileTreeModel> fileTreeItem = row.getTreeItem();

                            Path localPath = null;
                            if (fileTreeItem != null && !fileTreeItem.getValue().getType().equals(FileTreeModel.Type.File)) {
                                localPath = fileTreeItem.getValue().getPath();
                            } else {
                                if (!fileRootItem.equals(ROOT_LOCATION)) {
                                    localPath = Paths.get(fileRootItem);
                                } else {
                                    localPath = fileTreeItem.getParent().getValue().getPath();
                                }
                            }

                            @SuppressWarnings("unchecked")
                            final List<Ds3TreeTableValueCustom> list = (List<Ds3TreeTableValueCustom>) db.getContent(dataFormat);

                            final Session session = getSession(db.getString());

                            final String priority = (!savedJobPrioritiesStore.getJobSettings().getGetJobPriority().equals(resourceBundle.getString("defaultPolicyText"))) ? savedJobPrioritiesStore.getJobSettings().getGetJobPriority() : null;

                            final Ds3GetJob getJob = new Ds3GetJob(list, localPath, session.getClient(),
                                    deepStorageBrowserPresenter, priority, settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads());

                            jobWorkers.execute(getJob);

                            getJob.setOnSucceeded(e -> {
                                LOG.info("Get Job completed successfully");
                                if (fileTreeItem != null)
                                    refresh(fileTreeItem);
                                else
                                    refreshFileTreeView();
                            });

                            getJob.setOnFailed(e -> {
                                LOG.info("Get Job failed");
                                if (fileTreeItem != null)
                                    refresh(fileTreeItem);
                                else
                                    refreshFileTreeView();
                            });

                            getJob.setOnCancelled(e -> {
                                LOG.info("Get Job cancelled");
                                try {
                                    final CancelJobSpectraS3Response cancelJobSpectraS3Response = session.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(getJob.getJobId()).withForce(true));
                                    deepStorageBrowserPresenter.logText("GET Job Cancelled. Response code:" + cancelJobSpectraS3Response.getResponse().getStatusCode(), LogType.ERROR);
                                    if (fileTreeItem != null)
                                        refresh(fileTreeItem);
                                    else
                                        refreshFileTreeView();
                                } catch (IOException e1) {
                                    deepStorageBrowserPresenter.logText(" Failed to cancel job. ", LogType.ERROR);
                                }
                            });
                        }
                        event.consume();

                    });

                    row.setOnDragOver(event -> {

                        final TreeItem<FileTreeModel> treeItem = row.getTreeItem();

                        if (event.getGestureSource() != treeTable && event.getDragboard().hasFiles()) {
                            event.acceptTransferModes(TransferMode.COPY);
                            if (treeItem == null) {
                                if (fileRootItem.equals(ROOT_LOCATION)) {
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

                        if (!selectedItems.isEmpty()) {
                            LOG.info("Starting drag and drop event");
                            final Dragboard db = treeTable.startDragAndDrop(TransferMode.COPY);
                            final ClipboardContent content = new ClipboardContent();

                            content.putFilesByPath(selectedItems
                                    .stream()
                                    .map(i -> i.getValue().getPath().toString())
                                    .collect(Collectors.toList()));

                            db.setContent(content);
                        }
                        event.consume();
                    });

                    return row;
                }

        );

        final Stream<FileTreeModel> rootItems = provider.getRoot(fileRootItem);

        localPathIndicator.setText(ROOT_LOCATION);
        final Node oldPlaceHolder = treeTable.getPlaceholder();

        final ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(90, 90);
        treeTable.setPlaceholder(new StackPane(progress));

        final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTable.setShowRoot(false);

        final Task getMediaDevices = new Task() {
            @Override
            protected Object call() throws Exception {
                rootItems.forEach(ftm ->
                        {
                            final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, ftm, workers);
                            rootTreeItem.getChildren().add(newRootTreeItem);
                        }
                );
                return null;
            }
        };

        workers.execute(getMediaDevices);

        getMediaDevices.setOnSucceeded(event -> {
            treeTable.setRoot(rootTreeItem);
            treeTable.setPlaceholder(oldPlaceHolder);
        });

    }

    private Session getSession(String sessionName) {
        return store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(sessionName)).findFirst().get();
    }

    private void refresh(TreeItem<FileTreeModel> selectedItem) {

        if (selectedItem.getValue().getType().equals(FileTreeModel.Type.File)) {
            if (selectedItem.getParent().getValue() != null) {
                deepStorageBrowserPresenter.logText("Refreshing " + selectedItem.getParent().getValue().getName(), LogType.SUCCESS);
                selectedItem = selectedItem.getParent();
            } else {
                refreshFileTreeView();
            }
        } else {
            deepStorageBrowserPresenter.logText("Refreshing " + selectedItem.getValue().getName(), LogType.SUCCESS);
        }

        if (selectedItem instanceof FileTreeTableItem) {
            final FileTreeTableItem fileTreeTableItem = (FileTreeTableItem) selectedItem;
            fileTreeTableItem.refresh();
        }

    }

}
