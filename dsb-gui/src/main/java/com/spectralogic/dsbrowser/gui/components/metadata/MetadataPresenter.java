/*
 * ****************************************************************************
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
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.components.metadata;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.networking.Metadata;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.gui.util.ByteFormat;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ResourceBundle;

@Presenter
public class MetadataPresenter implements Initializable {

    private final Logger LOG = LoggerFactory.getLogger(MetadataPresenter.class);
    private final String creationTimeKey = "ds3-creation-time";
    private final String accessTimeKey = "ds3-last-access-time";
    private final String lastModifiedKey = "ds3-last-modified-time";
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSSSS'Z'");
    private final Calendar calendar = Calendar.getInstance();
    @FXML
    private Label objectName;
    @FXML
    private Label objectSize;
    @FXML
    private Tooltip nameTooltip;
    @FXML
    private TableView<MetadataEntry> metadataTable;
    @FXML
    private TableColumn<MetadataEntry, String> metadataTableColValue;

    @FXML
    private TableColumn metadataTableColName;

    @ModelContext
    private Ds3Metadata ds3Metadata;

    @Inject
    private ResourceBundle resourceBundle;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initLabels();
            initTable();

        } catch (final Throwable e) {
            LOG.error("Failed to create ds3Metadata presenter", e);
        }
    }

    private void initTable() {
        final ImmutableList.Builder<MetadataEntry> builder = ImmutableList.builder();
        final Metadata metadata = ds3Metadata.getMetadata();
        createMetaDataBuilder(metadata, builder);
        metadataTable.setItems(FXCollections.observableList(builder.build()));
        metadataTable.getSelectionModel().setCellSelectionEnabled(true);
        metadataTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        //showing tooltip for every column
        metadataTableColValue.setCellFactory
                (
                        column ->
                        {
                            return new TableCell<MetadataEntry, String>() {
                                @Override
                                protected void updateItem(final String item, final boolean empty) {
                                    if (item != null) {
                                        super.updateItem(item, empty);
                                        setText(item);
                                        setTooltip(new Tooltip(item));
                                    }
                                }
                            };
                        });
    }

    //create meta data keys for showing on server
    private ImmutableList.Builder<MetadataEntry> createMetaDataBuilder(final Metadata metadata, final ImmutableList.Builder<MetadataEntry> builder) {
        try {
            //if meta data does not contains creation time key thens show all metadata got from bp server without any processing
            if (metadata.get(creationTimeKey).size() > 0) {
                //get the creation time from server
                String creationTime = metadata.get(creationTimeKey).stream().findFirst().orElse(null);
                if (creationTime.contains("T")) {
                    creationTime = creationTime.replace("T", " ");
                    builder.add(new MetadataEntry(creationTimeKey, creationTime));
                } else {
                    final long creationTimeLong = Long.parseLong(creationTime);
                    calendar.setTimeInMillis(creationTimeLong);
                    builder.add(new MetadataEntry(creationTimeKey, formatter.format(calendar.getTime())));
                }
                //get the access time from server
                if (metadata.get(accessTimeKey).size() > 0) {
                    String accessTime = metadata.get(accessTimeKey).stream().findFirst().orElse(null);
                    if (accessTime.contains("T")) {
                        accessTime = accessTime.replace("T", " ");
                        builder.add(new MetadataEntry(accessTimeKey, accessTime));
                    } else {
                        final long accessTimeLong = Long.parseLong(accessTime);
                        calendar.setTimeInMillis(accessTimeLong);
                        builder.add(new MetadataEntry(accessTimeKey, formatter.format(calendar.getTime())));
                    }
                }
                //get the last modified time from server
                if (metadata.get(lastModifiedKey).size() > 0) {
                    String modifiedTime = metadata.get(lastModifiedKey).stream().findFirst().orElse(null);
                    if (modifiedTime.contains("T")) {
                        modifiedTime = modifiedTime.replace("T", " ");
                        builder.add(new MetadataEntry(lastModifiedKey, modifiedTime));
                    } else {
                        final long modifiedTimeLong = Long.parseLong(modifiedTime);
                        calendar.setTimeInMillis(modifiedTimeLong);
                        builder.add(new MetadataEntry(lastModifiedKey, formatter.format(calendar.getTime())));
                    }
                }
                //get owner -sid(Windows)
                if (metadata.get("ds3-owner").size() > 0) {
                    builder.add(new MetadataEntry("ds3-owner", metadata.get("ds3-owner").stream().findFirst().orElse(null)));
                }
                //get group sid(Windows)
                if (metadata.get("ds3-group").size() > 0) {
                    builder.add(new MetadataEntry("ds3-group", metadata.get("ds3-group").stream().findFirst().orElse(null)));
                }
                //get User Id (Linux and MAC)
                if (metadata.get("ds3-uid").size() > 0) {
                    builder.add(new MetadataEntry("ds3-uid", metadata.get("ds3-uid").stream().findFirst().orElse(null)));
                }
                //get group Id (Linux and MAC)
                if (metadata.get("ds3-gid").size() > 0) {
                    builder.add(new MetadataEntry("ds3-gid", metadata.get("ds3-gid").stream().findFirst().orElse(null)));
                }
                //get Flag(Windows)
                if (metadata.get("ds3-flags").size() > 0) {
                    builder.add(new MetadataEntry("ds3-flags", metadata.get("ds3-flags").stream().findFirst().orElse(null)));
                }
                //get dacl (Windows)
                if (metadata.get("ds3-dacl").size() > 0) {
                    builder.add(new MetadataEntry("ds3-dacl", metadata.get("ds3-dacl").stream().findFirst().orElse(null)));
                }
                //get Mode(Linux and MAC)
                if (metadata.get("ds3-mode").size() > 0) {
                    builder.add(new MetadataEntry("ds3-mode", metadata.get("ds3-mode").stream().findFirst().orElse(null)));
                }

            } else {
                metadata.keys().forEach(key -> metadata.get(key).forEach(value -> builder.add(new MetadataEntry(key, value))));
            }
            return builder;
        } catch (final Exception e) {
            LOG.error("Failed to create metadata", e);
        }
        return builder;
    }

    private void initLabels() {
        metadataTableColName.setText(resourceBundle.getString("metadataTableColName"));
        metadataTableColValue.setText(resourceBundle.getString("metadataTableColValue"));
        objectName.setText(ds3Metadata.getName());
        objectSize.setText(ByteFormat.humanReadableByteCount(ds3Metadata.getSize(), false));
        nameTooltip.setText(ds3Metadata.getName());
    }


    public static class MetadataEntry {
        private final String key;
        private final String value;

        private MetadataEntry(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
