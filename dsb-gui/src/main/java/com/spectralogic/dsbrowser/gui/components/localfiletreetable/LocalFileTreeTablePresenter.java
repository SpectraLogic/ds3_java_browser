package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalFileTreeTablePresenter implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileTreeTablePresenter.class);

    private final Alert ALERT = new Alert(Alert.AlertType.INFORMATION);

    @FXML
    public TreeTableView<FileTreeModel> treeTable;

    @FXML
    private Button homeButton, refreshButton, toMyComputer, transferButton;

    @FXML
    private Tooltip homeButtonTooltip, refreshButtonTooltip, toMyComputerTooltip, transferButtonTooltip;

    @FXML
    private Label localPathIndicator;

    @Inject
    private LocalFileTreeTableProvider provider;

    @Inject
    private DataFormat dataFormat;

    @Inject
    private JobWorkers jobWorkers;

    @Inject
    private Ds3SessionStore store;

    @Inject
    private DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    @Inject
    private ResourceBundle resourceBundle;

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
    }

    private void initListeners() {
        refreshButton.setOnAction(event -> refreshFileTreeView());
        homeButton.setOnAction(event -> changeRootDir(System.getProperty("user.home")));
        toMyComputer.setOnAction(event -> changeRootDir("My Computer"));
        transferButton.setOnAction(event -> transferToBlackPearl());
    }

    private void transferToBlackPearl() {
        try {
            final ObservableList<TreeItem<FileTreeModel>> currentSelection = treeTable.getSelectionModel().getSelectedItems();
            if (currentSelection.isEmpty()) {
                ALERT.setContentText("Select files to transfer");
                ALERT.showAndWait();
                return;
            }

            final ObservableList<javafx.scene.Node> list = deepStorageBrowserPresenter.blackPearl.getChildren();
            final VBox vbox = (VBox) list.stream().filter(i -> i instanceof VBox).findFirst().get();
            final ObservableList<javafx.scene.Node> children = vbox.getChildren();
//      @SuppressWarnings("unchecked")
            final TabPane tabPane = (TabPane) children.stream().filter(i -> i instanceof TabPane).findFirst().get();
            @SuppressWarnings("unchecked")
            final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = (TreeTableView<Ds3TreeTableValue>) tabPane.getSelectionModel().getSelectedItem().getContent();
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

            final TreeItem<Ds3TreeTableValue> treeItem = values.get(0);
            if (!treeItem.isExpanded()) {
                treeItem.setExpanded(true);
            }
            final Ds3TreeTableValue value = treeItem.getValue();
            final String bucket = value.getBucketName();
            final String targetDir = value.getDirectoryName();
            LOG.info("Passing new Ds3PutJob to jobWorkers thread pool to be scheduled");

            final Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(tabPane.getSelectionModel().getSelectedItem().getText())).
                    findFirst().get();

            final List<File> files = currentSelection
                    .stream()
                    .map(i -> new File(i.getValue().getPath().toString()))
                    .collect(Collectors.toList());

            final Ds3PutJob putJob = new Ds3PutJob(session.getClient(), files, bucket, targetDir, deepStorageBrowserPresenter);

            putJob.setOnSucceeded(e -> {
                LOG.info("job completed successfully");
                if (treeItem instanceof Ds3TreeTableItem) {
                    LOG.info("Refresh row");
                    final Ds3TreeTableItem ds3TreeTableItem = (Ds3TreeTableItem) treeItem;
                    ds3TreeTableItem.refresh();
                }
            });

            jobWorkers.execute(putJob);
        } catch (final Exception e) {
            deepStorageBrowserPresenter.logText("Something went wrong. Reason: " + e.toString(), LogType.ERROR);
        }
    }

    private void changeRootDir(final String rootDir) {
        localPathIndicator.setText(rootDir);
        final Stream<FileTreeModel> rootItems = provider.getRoot(rootDir);
        final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTable.setShowRoot(false);
        rootItems.forEach(ftm -> {
            final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, ftm);
            rootTreeItem.getChildren().add(newRootTreeItem);
        });
        treeTable.setRoot(rootTreeItem);
    }

    private void refreshFileTreeView() {
        final TreeItem<FileTreeModel> currentSelection = treeTable.getSelectionModel().getSelectedItem();
        if (currentSelection == null) {
            ALERT.setContentText("Select folder to refresh");
            ALERT.showAndWait();
        } else {
            refresh(currentSelection);
            treeTable.getSelectionModel().clearSelection();
            treeTable.getSelectionModel().select(currentSelection);
        }
    }

    private void initTableView() {

        treeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //noinspection unchecked
        treeTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(final ObservableValue observable, final Object oldValue,
                                Object newValue) {
                //noinspection unchecked
                final TreeItem<FileTreeModel> selectedItem = (TreeItem<FileTreeModel>) newValue;
                if (selectedItem != null) {
                    localPathIndicator.setText(selectedItem.getValue().getPath().toString());
                }
            }
        });

        treeTable.setRowFactory(view -> {

            final TreeTableRow<FileTreeModel> row = new TreeTableRow<FileTreeModel>();

            row.setOnDragDropped(event -> {
                LOG.info("Drop detected..");
                final Dragboard db = event.getDragboard();

                if (db.hasContent(dataFormat)) {
                    LOG.info("Drop event contains files");
                    // get bucket info and current path
                    final TreeItem<FileTreeModel> fileTreeItem = row.getTreeItem();
                    @SuppressWarnings("unchecked")
                    final List<Ds3TreeTableValue> list = (List<Ds3TreeTableValue>) db.getContent(dataFormat);

                    final Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(db.getString())).findFirst().get();

                    final Ds3GetJob getJob = new Ds3GetJob(list, fileTreeItem.getValue(), session.getClient(), deepStorageBrowserPresenter);

                    getJob.setOnSucceeded(e -> {
                        LOG.info("Get Job completed successfully");
                        if (fileTreeItem.getValue().getType().equals(FileTreeModel.Type.File))
                            refresh(fileTreeItem.getParent());
                        else
                            refresh(fileTreeItem);
                    });

                    jobWorkers.execute(getJob);
                }
                event.consume();
            });

            row.setOnDragOver(event -> {
                if (event.getGestureSource() != treeTable && event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY);
                }

                event.consume();
            });

            row.setOnDragEntered(event -> {
                final TreeItem<FileTreeModel> treeItem = row.getTreeItem();

                if (treeItem != null) {
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
        });

        final Stream<FileTreeModel> rootItems = provider.getRoot("My Computer");

        localPathIndicator.setText("My Computer");

        final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTable.setShowRoot(false);

        rootItems.forEach(ftm -> {
            final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, ftm);
            newRootTreeItem.getChildren().sort(Comparator.comparing(t -> t.getValue().getType().toString()));
            rootTreeItem.getChildren().add(newRootTreeItem);
        });

        treeTable.setRoot(rootTreeItem);

    }

    private void refresh(TreeItem<FileTreeModel> selectedItem) {
        if (selectedItem.getValue().getType().equals(FileTreeModel.Type.File)) {
            deepStorageBrowserPresenter.logText("Refreshing " + selectedItem.getParent().getValue().getName(), LogType.SUCCESS);
            selectedItem = selectedItem.getParent();
        } else {
            deepStorageBrowserPresenter.logText("Refreshing " + selectedItem.getValue().getName(), LogType.SUCCESS);
        }

        final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, selectedItem.getValue());
        selectedItem.getChildren().remove(0, selectedItem.getChildren().size());
        selectedItem.getChildren().addAll(newRootTreeItem.getChildren());
        newRootTreeItem.setExpanded(true);

    }

}
