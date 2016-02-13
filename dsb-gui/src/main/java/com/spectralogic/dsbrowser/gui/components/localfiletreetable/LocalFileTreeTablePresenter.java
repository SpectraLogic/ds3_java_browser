package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.spectralogic.dsbrowser.api.FileTreeTableProvider;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeTableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class LocalFileTreeTablePresenter implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(LocalFileTreeTablePresenter.class);

    public LocalFileTreeTablePresenter() {}

    @FXML
    TreeTableView treeTable;

    @Inject
    FileTreeTableProvider provider;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        LOG.info("Starting LocalFileTreeTablePresenter");
    }
}
