package com.spectralogic.dsbrowser.gui.components.metadata;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.networking.Metadata;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
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
import java.util.Optional;
import java.util.ResourceBundle;

@Presenter
public class MetadataPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(MetadataPresenter.class);
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

    private MetadataEntry getTime(final String time, final String key) {
        if (time.contains(StringConstants.STR_T)) {
            return new MetadataEntry(key, time.replace(StringConstants.STR_T, StringConstants.SPACE));
        } else {
            final long creationTimeLong = Long.parseLong(time);
            calendar.setTimeInMillis(creationTimeLong);
            return new MetadataEntry(key, formatter.format(calendar.getTime()));
        }
    }

    //create metadata keys for showing on server
    public ImmutableList.Builder<MetadataEntry> createMetadataBuilder(final Metadata metadata, final ImmutableList.Builder<MetadataEntry> builder) {
        try {
            //if metadata does not contains creation time key then show all metadata got from bp server without any processing
            if (metadata.get(StringConstants.CREATION_TIME_KEY).size() > 0) {
                //get the creation time from server
                final Optional<String> creationTimeElement = metadata.get(StringConstants.CREATION_TIME_KEY).stream().findFirst();
                if (creationTimeElement.isPresent()) {
                    builder.add(getTime(creationTimeElement.get(), StringConstants.CREATION_TIME_KEY));
                }
                //get the access time from server
                if (metadata.get(StringConstants.ACCESS_TIME_KEY).size() > 0) {
                    final Optional<String> accessTimeElement = metadata.get(StringConstants.ACCESS_TIME_KEY).stream().findFirst();
                    if (accessTimeElement.isPresent()) {
                        builder.add(getTime(accessTimeElement.get(), StringConstants.ACCESS_TIME_KEY));
                    }
                }
                //get the last modified time from server
                if (metadata.get(StringConstants.LAST_MODIFIED_KEY).size() > 0) {
                    final Optional<String> lastModifiedElement = metadata.get(StringConstants.LAST_MODIFIED_KEY).stream().findFirst();
                    if (lastModifiedElement.isPresent()) {
                        builder.add(getTime(lastModifiedElement.get(), StringConstants.LAST_MODIFIED_KEY));
                    }
                }
                //get owner sid(Windows)
                if (metadata.get(StringConstants.OWNER).size() > 0) {
                    final Optional<String> ownerElement = metadata.get(StringConstants.OWNER).stream().findFirst();
                    if (ownerElement.isPresent()) {
                        builder.add(new MetadataEntry(StringConstants.OWNER, ownerElement.get()));
                    }

                }
                //get group sid(Windows)
                if (metadata.get(StringConstants.GROUP).size() > 0) {
                    final Optional<String> groupElement = metadata.get(StringConstants.GROUP).stream().findFirst();
                    if (groupElement.isPresent()) {
                        builder.add(new MetadataEntry(StringConstants.GROUP, groupElement.get()));
                    }
                }
                //get User Id (Linux and MAC)
                if (metadata.get(StringConstants.UID).size() > 0) {
                    final Optional<String> uidElement = metadata.get(StringConstants.UID).stream().findFirst();
                    if (uidElement.isPresent()) {
                        builder.add(new MetadataEntry(StringConstants.UID, uidElement.get()));
                    }
                }
                //get group Id (Linux and MAC)
                if (metadata.get(StringConstants.GID).size() > 0) {
                    final Optional<String> gidElement = metadata.get(StringConstants.GID).stream().findFirst();
                    if (gidElement.isPresent()) {
                        builder.add(new MetadataEntry(StringConstants.GID, gidElement.get()));
                    }
                }
                //get Flag(Windows)
                if (metadata.get(StringConstants.FLAG).size() > 0) {
                    final Optional<String> flagElement = metadata.get(StringConstants.FLAG).stream().findFirst();
                    if (flagElement.isPresent()) {
                        builder.add(new MetadataEntry(StringConstants.FLAG, flagElement.get()));
                    }
                }
                //get dacl (Windows)
                if (metadata.get(StringConstants.DACL).size() > 0) {
                    final Optional<String> daclElement = metadata.get(StringConstants.DACL).stream().findFirst();
                    if (daclElement.isPresent()) {
                        builder.add(new MetadataEntry(StringConstants.DACL, daclElement.get()));
                    }
                }
                //get Mode(Linux and MAC)
                if (metadata.get(StringConstants.MODE).size() > 0) {
                    final Optional<String> modeElement = metadata.get(StringConstants.MODE).stream().findFirst();
                    if (modeElement.isPresent()) {
                        builder.add(new MetadataEntry(StringConstants.MODE, modeElement.get()));
                    }
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
