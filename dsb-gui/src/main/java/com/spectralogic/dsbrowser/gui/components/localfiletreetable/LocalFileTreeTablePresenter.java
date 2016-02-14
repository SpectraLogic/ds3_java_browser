package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.api.FileTreeModel;
import com.spectralogic.dsbrowser.local.LocalFileTreeTableProvider;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class LocalFileTreeTablePresenter implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(LocalFileTreeTablePresenter.class);

    @FXML
    TreeTableView<FileTreeModel> treeTable;

    @FXML
    Button homeButton;

    @Inject
    LocalFileTreeTableProvider provider;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        initMenuBar();

        LOG.info("Starting LocalFileTreeTablePresenter");

        final ImmutableList<FileTreeModel> rootItems = provider.getRoot();

        final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTable.setShowRoot(false);

        rootItems.stream().forEach(ftm -> {
            final TreeItem<FileTreeModel> newRootTreeItem = new TreeItem<>(ftm);
            if (ftm.getType() == FileTreeModel.Type.DIRECTORY) {
                newRootTreeItem.setGraphic(Icon.getIcon(FontAwesomeIcon.HDD_ALT));
            }
            rootTreeItem.getChildren().add(newRootTreeItem);
        });

        treeTable.setRoot(rootTreeItem);
    }

    private void initMenuBar() {
        homeButton.setGraphic(Icon.getIcon(FontAwesomeIcon.HOME));
    }
}
