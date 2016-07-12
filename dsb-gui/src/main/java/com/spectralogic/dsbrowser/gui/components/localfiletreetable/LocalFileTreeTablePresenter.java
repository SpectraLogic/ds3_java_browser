package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.application.Platform;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalFileTreeTablePresenter implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileTreeTablePresenter.class);

    @FXML
    private TreeTableView<FileTreeModel> treeTable;

    @FXML
    private Button homeButton, refreshButton, toMyComputer;

    @FXML
    Label localPathIndicator;

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

    private final Alert alert = new Alert(Alert.AlertType.INFORMATION);

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Starting LocalFileTreeTablePresenter");
            alert.setTitle("Error");
            alert.setHeaderText(null);
            initMenuBar();
            initTableView();
            initListeners();
        } catch (final Throwable e) {
            LOG.error("Encountered an error when creating LocalFileTreeTablePresenter", e);
        }
    }

    private void initListeners() {
        refreshButton.setOnAction(event -> refreshFileTreeView());
        homeButton.setOnAction(event -> navigateToHomeUserDirectory());
        toMyComputer.setOnAction(event -> toMyComputer());
    }

    private void toMyComputer() {
        localPathIndicator.setText("My Computer");
        TreeItem<FileTreeModel> currentSelection = treeTable.getSelectionModel().getSelectedItem();
        final Stream<FileTreeModel> rootItems = provider.getRoot();
        final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTable.setShowRoot(false);
        rootItems.forEach(ftm -> {
            final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, ftm);
            rootTreeItem.getChildren().add(newRootTreeItem);
        });
        treeTable.setRoot(rootTreeItem);
    }

    private void navigateToHomeUserDirectory() {
        localPathIndicator.setText(System.getProperty("user.home"));
        final Stream<FileTreeModel> rootItems = provider.getHomeDirs();
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
        TreeItem<FileTreeModel> currentSelection = treeTable.getSelectionModel().getSelectedItem();
        if (currentSelection == null) {
            alert.setContentText("Select folder to refresh");
            alert.showAndWait();
        } else {
            if(currentSelection.getValue().getType().equals(FileTreeModel.Type.File))
                deepStorageBrowserPresenter.logText("Refreshing "+currentSelection.getParent().getValue().getName());
            else
                deepStorageBrowserPresenter.logText("Refreshing "+currentSelection.getValue().getName());
            refresh(currentSelection);
        }

    }

    private void initTableView() {

        treeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //noinspection unchecked
        treeTable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue,
                                Object newValue) {
                //noinspection unchecked
                TreeItem<FileTreeModel> selectedItem = (TreeItem<FileTreeModel>) newValue;
                if (selectedItem != null) {
                    localPathIndicator.setText(selectedItem.getValue().getPath().toString());
                }
            }
        });

        treeTable.setRowFactory(view -> {
            TreeTableRow<FileTreeModel> row = new TreeTableRow<FileTreeModel>();

            row.setOnDragDropped(event -> {
                LOG.info("Drop detected..");
                final Dragboard db = event.getDragboard();

                if (db.hasContent(dataFormat)) {
                    LOG.info("Drop event contains files");
                    // get bucket info and current path
                    final TreeItem<FileTreeModel> fileTreeItem = row.getTreeItem();
                    @SuppressWarnings("unchecked")
                    List<Ds3TreeTableValue> list = (List<Ds3TreeTableValue>) db.getContent(dataFormat);

                    LOG.info("Passing new Ds3GetJob to jobWorkers thread pool to be scheduled");
                    final Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(db.getString())).findFirst().get();

                    final Ds3GetJob getJob = new Ds3GetJob(list, fileTreeItem.getValue(), session.getClient());
//
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

        final Stream<FileTreeModel> rootItems = provider.getRoot();

        localPathIndicator.setText("My Computer");

        final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTable.setShowRoot(false);

        rootItems.forEach(ftm -> {
            final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, ftm);
            newRootTreeItem.getChildren().sort(Comparator.comparing(t -> t.getValue().getType().name()));
            rootTreeItem.getChildren().add(newRootTreeItem);
        });

        treeTable.setRoot(rootTreeItem);

    }

    private void refresh(TreeItem<FileTreeModel> selectedItem) {
        final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, selectedItem.getValue());
        selectedItem.getChildren().remove(0, selectedItem.getChildren().size());
        selectedItem.getChildren().addAll(newRootTreeItem.getChildren());
        newRootTreeItem.setExpanded(true);
    }

    private void initMenuBar() {
        // homeButton.setGraphic(Icon.getIcon(FontAwesomeIcon.HOME));
        // refreshButton.setGraphic(Icon.getIcon(FontAwesomeIcon.REFRESH));
    }
}
