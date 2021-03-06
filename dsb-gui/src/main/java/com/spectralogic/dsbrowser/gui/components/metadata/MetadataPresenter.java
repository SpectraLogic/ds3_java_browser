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

package com.spectralogic.dsbrowser.gui.components.metadata;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.networking.Metadata;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormatKt;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

@Presenter
public class MetadataPresenter implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataPresenter.class);

    @FXML
    private TextField objectName;

    @FXML
    private TextField objectSize;

    @FXML
    private TextField lastModified;

    @FXML
    private TableView<MetadataEntry> metadataTable;

    @FXML
    private TableColumn<MetadataEntry, String> metadataTableColValue;

    @FXML
    private TableColumn metadataTableColName;

    @ModelContext
    private Ds3Metadata ds3Metadata;

    private final ResourceBundle resourceBundle;

    @Inject
    public MetadataPresenter(final ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initLabels();
            initTable();
        } catch (final Throwable t) {
            LOG.error("Encountered an error when initializing MetadataPresenter", t);
        }
    }

    private void initTable() {
        final Metadata metadata = ds3Metadata.getMetadata();
        metadataTable.setItems(FXCollections.observableList(createMetadataEntries(metadata)));
        metadataTable.getSelectionModel().setCellSelectionEnabled(true);
        metadataTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        //showing tooltip for every column
        metadataTableColValue.setCellFactory(column -> {
            final javafx.scene.control.TableCell cell = new TableCell();
            final MenuItem copyMenuItem = new MenuItem("Copy");
            copyMenuItem.setOnAction(event -> copyTexToClipboard(event, cell.getText()));
            final ContextMenu contextMenu = new ContextMenu(copyMenuItem);
            cell.setContextMenu(contextMenu);
            return cell;
        });
    }

    private void copyTexToClipboard(final Event event, final String cellText) {
        final ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(cellText.trim());
        Clipboard.getSystemClipboard().setContent(clipboardContent);
        event.consume();
    }

    private void initLabels() {
        metadataTableColName.setText(resourceBundle.getString("metadataTableColName"));
        metadataTableColValue.setText(resourceBundle.getString("metadataTableColValue"));
        objectName.setText(ds3Metadata.getName());
        objectSize.setText(FileSizeFormatKt.toByteRepresentation(ds3Metadata.getSize()));
        lastModified.setText(ds3Metadata.getLastModified());
    }

    //create metadata keys for showing on server
    static ImmutableList<MetadataEntry> createMetadataEntries(final Metadata metadata) {
        return metadata.keys()
                .stream()
                .flatMap(key -> metadata.get(key).stream().map(value -> new MetadataEntry(key, value)))
                .collect(GuavaCollectors.immutableList());

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
