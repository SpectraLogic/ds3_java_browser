package com.spectralogic.dsbrowser.gui.components.metadata;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.networking.Metadata;
import com.spectralogic.dsbrowser.gui.util.ByteFormat;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
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

public class MetadataPresenter implements Initializable {

    private final Logger LOG = LoggerFactory.getLogger(MetadataPresenter.class);
    private final SimpleDateFormat formatter = new SimpleDateFormat(StringConstants.DATE_FORMAT);
    private final Calendar calendar = Calendar.getInstance();
    @FXML
    private Label objectName;
    @FXML
    private Label objectSize;

    @FXML
    private Label lastModified;
    @FXML
    private Tooltip nameTooltip;
    @FXML
    private TableView<MetadataEntry> metadataTable;
    @FXML
    private TableColumn<MetadataEntry, String> metadataTableColValue;

    @FXML
    private TableColumn metadataTableColName;

    @Inject
    private Ds3Metadata ds3Metadata;

    @Inject
    private ResourceBundle resourceBundle;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initLabels();
            initTable();

        } catch (final Exception e) {
            LOG.error("Failed to create ds3Metadata presenter", e);
        }
    }

    private void initTable() {
        final ImmutableList.Builder<MetadataEntry> builder = ImmutableList.builder();
        final Metadata metadata = ds3Metadata.getMetadata();
        createMetadataBuilder(metadata, builder);
        metadataTable.setItems(FXCollections.observableList(builder.build()));
        metadataTable.getSelectionModel().setCellSelectionEnabled(true);
        metadataTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        //showing tooltip for every column
        metadataTableColValue.setCellFactory
                (
                        column ->
                                new TableCell<MetadataEntry, String>() {
                                    @Override
                                    protected void updateItem(final String item, final boolean empty) {
                                        if (item != null) {
                                            super.updateItem(item, empty);
                                            setText(item);
                                            setTooltip(new Tooltip(item));
                                        }
                                    }
                                });
    }

    private void initLabels() {
        metadataTableColName.setText(resourceBundle.getString("metadataTableColName"));
        metadataTableColValue.setText(resourceBundle.getString("metadataTableColValue"));
        objectName.setText(ds3Metadata.getName());
        objectSize.setText(ByteFormat.humanReadableByteCount(ds3Metadata.getSize(), false));
        lastModified.setText(ds3Metadata.getLastModified());
        nameTooltip.setText(ds3Metadata.getName());
    }

    //create metadata keys for showing on server
    public ImmutableList.Builder<MetadataEntry> createMetadataBuilder(final Metadata metadata, final ImmutableList.Builder<MetadataEntry> builder) {
        try {
            //if metadata does not contains creation time key then show all metadata got from bp server without any processing
            if (metadata.get(StringConstants.CREATION_TIME_KEY).size() > 0) {
                //get the creation time from server
                String creationTime = metadata.get(StringConstants.CREATION_TIME_KEY).stream().findFirst().orElse(null);
                if (creationTime.contains(StringConstants.STR_T)) {
                    creationTime = creationTime.replace(StringConstants.STR_T, StringConstants.SPACE);
                    builder.add(new MetadataEntry(StringConstants.CREATION_TIME_KEY, creationTime));
                } else {
                    final long creationTimeLong = Long.parseLong(creationTime);
                    calendar.setTimeInMillis(creationTimeLong);
                    builder.add(new MetadataEntry(StringConstants.CREATION_TIME_KEY, formatter.format(calendar.getTime())));
                }
                //get the access time from server
                if (metadata.get(StringConstants.ACCESS_TIME_KEY).size() > 0) {
                    String accessTime = metadata.get(StringConstants.ACCESS_TIME_KEY).stream().findFirst().orElse(null);
                    if (accessTime.contains(StringConstants.STR_T)) {
                        accessTime = accessTime.replace(StringConstants.STR_T, StringConstants.SPACE);
                        builder.add(new MetadataEntry(StringConstants.ACCESS_TIME_KEY, accessTime));
                    } else {
                        final long accessTimeLong = Long.parseLong(accessTime);
                        calendar.setTimeInMillis(accessTimeLong);
                        builder.add(new MetadataEntry(StringConstants.ACCESS_TIME_KEY, formatter.format(calendar.getTime())));
                    }
                }
                //get the last modified time from server
                if (metadata.get(StringConstants.LAST_MODIFIED_KEY).size() > 0) {
                    String modifiedTime = metadata.get(StringConstants.LAST_MODIFIED_KEY).stream().findFirst().orElse(null);
                    if (modifiedTime.contains(StringConstants.STR_T)) {
                        modifiedTime = modifiedTime.replace(StringConstants.STR_T, StringConstants.SPACE);
                        builder.add(new MetadataEntry(StringConstants.LAST_MODIFIED_KEY, modifiedTime));
                    } else {
                        final long modifiedTimeLong = Long.parseLong(modifiedTime);
                        calendar.setTimeInMillis(modifiedTimeLong);
                        builder.add(new MetadataEntry(StringConstants.LAST_MODIFIED_KEY, formatter.format(calendar.getTime())));
                    }
                }
                //get owner sid(Windows)
                if (metadata.get(StringConstants.OWNER).size() > 0) {
                    builder.add(new MetadataEntry(StringConstants.OWNER, metadata.get(StringConstants.OWNER).stream().findFirst().orElse(null)));
                }
                //get group sid(Windows)
                if (metadata.get(StringConstants.GROUP).size() > 0) {
                    builder.add(new MetadataEntry(StringConstants.GROUP, metadata.get(StringConstants.GROUP).stream().findFirst().orElse(null)));
                }
                //get User Id (Linux and MAC)
                if (metadata.get(StringConstants.UID).size() > 0) {
                    builder.add(new MetadataEntry(StringConstants.UID, metadata.get(StringConstants.UID).stream().findFirst().orElse(null)));
                }
                //get group Id (Linux and MAC)
                if (metadata.get(StringConstants.GID).size() > 0) {
                    builder.add(new MetadataEntry(StringConstants.GID, metadata.get(StringConstants.GID).stream().findFirst().orElse(null)));
                }
                //get Flag(Windows)
                if (metadata.get(StringConstants.FLAG).size() > 0) {
                    builder.add(new MetadataEntry(StringConstants.FLAG, metadata.get(StringConstants.FLAG).stream().findFirst().orElse(null)));
                }
                //get dacl (Windows)
                if (metadata.get(StringConstants.DACL).size() > 0) {
                    builder.add(new MetadataEntry(StringConstants.DACL, metadata.get(StringConstants.DACL).stream().findFirst().orElse(null)));
                }
                //get Mode(Linux and MAC)
                if (metadata.get(StringConstants.MODE).size() > 0) {
                    builder.add(new MetadataEntry(StringConstants.MODE, metadata.get(StringConstants.MODE).stream().findFirst().orElse(null)));
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


}
