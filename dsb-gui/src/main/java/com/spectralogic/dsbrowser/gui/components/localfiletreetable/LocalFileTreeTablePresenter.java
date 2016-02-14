package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

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


            final Stream<FileTreeModel> rootItems = provider.getRoot();

            final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
            rootTreeItem.setExpanded(true);
            treeTable.setShowRoot(false);

            rootItems.forEach(ftm -> {
                final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, ftm);
                rootTreeItem.getChildren().add(newRootTreeItem);
            });

            treeTable.setRoot(rootTreeItem);
        } catch (final Throwable e) {
            LOG.error("Encountered an error when creating LocalFileTreeTablePresenter", e);
        }
    }

    private void initMenuBar() {
        homeButton.setGraphic(Icon.getIcon(FontAwesomeIcon.HOME));
        refreshButton.setGraphic(Icon.getIcon(FontAwesomeIcon.REFRESH));
    }
}
