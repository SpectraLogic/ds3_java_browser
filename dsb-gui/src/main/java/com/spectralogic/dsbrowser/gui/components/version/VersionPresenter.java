package com.spectralogic.dsbrowser.gui.components.version;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.services.jobService.factories.GetJobFactory;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import kotlin.Pair;
import kotlin.Unit;

import javax.inject.Inject;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class VersionPresenter implements Initializable {

    private final Ds3Common ds3Common;
    private final GetJobFactory getJobFactory;

    @Inject
    public VersionPresenter(
            final Ds3Common ds3Common,
            final GetJobFactory getJobFactory
    ) {
        this.ds3Common = ds3Common;
        this.getJobFactory = getJobFactory;
    }

    @FXML
    private Button download;

    @FXML
    private TableView<VersionItem> versions;

    @FXML
    private HBox window;

    @ModelContext
    private VersionModel versionModel;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        download.setDisable(true);
        versions.getItems().addAll(versionModel.getVersionItems());
        versions.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        versions.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    download.setDisable(newValue == null);
                }
        );
        download.setOnMouseClicked(SafeHandler.logHandle(this::transfer));
    }

    private void transfer(final javafx.scene.input.MouseEvent mouseEvent) {
        final Path path;
        final VersionItem versionItem = versions.getSelectionModel().getSelectedItem();
        final Ds3Client client = ds3Common.getCurrentSession().getClient();
        final TreeTableView<FileTreeModel> localTreeTableView = ds3Common.getLocalTreeTableView();
        final TreeTableView.TreeTableViewSelectionModel<FileTreeModel> selectionModel = localTreeTableView.getSelectionModel();
        final TreeItem<FileTreeModel> selectedItem = selectionModel.getSelectedItem();
        if (selectedItem == null) {
            path = Paths.get(ds3Common.getLocalFilePathIndicator().getText());
        } else {
            path = selectedItem.getValue().getPath();
        }
        getJobFactory.create(
                ImmutableList.of(new Pair<>(versionItem.getName(), getParent(versionItem.getName()))),
                versionModel.getBucket(),
                path,
                client,
                () -> {
                    ds3Common.getLocalFileTreeTablePresenter().refreshFileTreeView();
                    return Unit.INSTANCE;
                },
                versionItem.getVersionId());
        versionModel.closePopup();
    }

    private String getParent(String path) {
        final int lastIndex = path.lastIndexOf("/");
        return path.substring(0, lastIndex);
    }

}
