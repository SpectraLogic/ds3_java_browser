package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalFileTreeTablePresenter implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(LocalFileTreeTablePresenter.class);

    @FXML
    TreeTableView<FileTreeModel> treeTable;

    @FXML
    Button homeButton;

    @FXML
    Button refreshButton;

    @Inject
    LocalFileTreeTableProvider provider;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Starting LocalFileTreeTablePresenter");
            initMenuBar();
            initTableView();


        } catch (final Throwable e) {
            LOG.error("Encountered an error when creating LocalFileTreeTablePresenter", e);
        }
    }

    private void initTableView() {
        treeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeTable.setOnDragDetected(event -> {
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

    private void initMenuBar() {
        homeButton.setGraphic(Icon.getIcon(FontAwesomeIcon.HOME));
        refreshButton.setGraphic(Icon.getIcon(FontAwesomeIcon.REFRESH));
    }
}
