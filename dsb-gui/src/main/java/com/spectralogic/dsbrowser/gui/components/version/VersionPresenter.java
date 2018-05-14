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

package com.spectralogic.dsbrowser.gui.components.version;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.services.jobService.factories.GetJobFactory;
import com.spectralogic.dsbrowser.gui.util.BaseTreeModel;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
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
    private BorderPane window;

    @FXML
    TableColumn<VersionItem, String> created;

    @ModelContext
    private VersionModel versionModel;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        final DateTimeUtils dateTimeUtils = new DateTimeUtils();
        created.setComparator(Comparator.comparing(dateTimeUtils::stringAsDate));
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
        final VersionItem versionItem = versions.getSelectionModel().getSelectedItem();
        final Ds3Client client = ds3Common.getCurrentSession().getClient();
        final Path path = getDirectoryOrRoot();
        startJob(path, versionItem, client);
        versionModel.closePopup();
    }

    private Path getDirectoryOrRoot() {
        final Path path;
        final TreeTableView<FileTreeModel> localTreeTableView = ds3Common.getLocalTreeTableView();
        final TreeTableView.TreeTableViewSelectionModel<FileTreeModel> selectionModel = localTreeTableView.getSelectionModel();
        final TreeItem<FileTreeModel> selectedItem = selectionModel.getSelectedItem();
        if (selectedItem == null || selectedItem.getValue().getType() != BaseTreeModel.Type.Directory) {
            path = Paths.get(ds3Common.getLocalFilePathIndicator().getText());
        } else {
            path = selectedItem.getValue().getPath();
        }
        return path;
    }

    private void startJob(final Path path, final VersionItem versionItem, final Ds3Client client) {
        getJobFactory.create(
                ImmutableList.of(new Pair<>(versionItem.getKey(), getParent(versionItem.getKey()))),
                versionModel.getBucket(),
                path,
                client,
                () -> {
                    ds3Common.getLocalFileTreeTablePresenter().refreshFileTreeView();
                    return Unit.INSTANCE;
                },
                versionItem.getVersionId());
    }

    private String getParent(final String path) {
        final int lastIndex = path.lastIndexOf("/");
        if (lastIndex == -1) {
            return "";
        } else {
            return path.substring(0, lastIndex);
        }
    }

}
