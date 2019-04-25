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
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.components.metadata.MetadataEntry;
import com.spectralogic.dsbrowser.gui.services.jobService.factories.GetJobFactory;
import com.spectralogic.dsbrowser.gui.util.BaseTreeModel;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import kotlin.Pair;
import kotlin.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class VersionPresenter implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(VersionPresenter.class);

    private final Ds3Common ds3Common;
    private final GetJobFactory getJobFactory;
    private final LoggingService loggingService;
    private final DateTimeUtils dateTimeUtils;

    @Inject
    public VersionPresenter(
            final Ds3Common ds3Common,
            final GetJobFactory getJobFactory,
            final LoggingService loggingService,
            final DateTimeUtils dateTimeUtils
    ) {
        this.ds3Common = ds3Common;
        this.getJobFactory = getJobFactory;
        this.loggingService = loggingService;
        this.dateTimeUtils = dateTimeUtils;
    }

    @FXML
    private Button download;

    @FXML
    private TableView<VersionItem> versions;

    @FXML
    private BorderPane window;

    @FXML
    TableColumn<VersionItem, String> created;

    @FXML
    TableColumn<VersionItem, String> versionId;

    @ModelContext
    private VersionModel versionModel;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {

            created.setComparator(Comparator.comparing(dateTimeUtils::stringAsDate));
            download.setDisable(true);
            versions.getItems().addAll(versionModel.getVersionItems());
            versions.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            versions.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                        download.setDisable(newValue == null);
                    }
            );
            download.setOnMouseClicked(SafeHandler.logHandle(this::transfer));

            versionId.setCellFactory(column -> {
                final javafx.scene.control.TableCell cell = new TableCell();
                final MenuItem copyMenuItem = new MenuItem("Copy");
                copyMenuItem.setOnAction(event -> copyTexToClipboard(event, cell.getText()));
                final ContextMenu contextMenu = new ContextMenu(copyMenuItem);
                cell.setContextMenu(contextMenu);
                return cell;
            });

        } catch (final Throwable t) {
            LOG.error("Unable to show version presenter", t);
            loggingService.logInternationalMessage("unableToShowVersionPresenter", LogType.ERROR);
        }
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
                ds3Common.getCurrentSession(),
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

    private void copyTexToClipboard(final Event event, final String cellText) {
        final ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(cellText.trim());
        Clipboard.getSystemClipboard().setContent(clipboardContent);
        event.consume();
    }

    private static class TableCell extends javafx.scene.control.TableCell<MetadataEntry, String> {
        @Override
        protected void updateItem(final String item, final boolean empty) {
            if (item != null) {
                super.updateItem(item, empty);
                setText(item);
            }
        }
    }
}
