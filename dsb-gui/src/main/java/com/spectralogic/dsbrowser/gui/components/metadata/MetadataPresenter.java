package com.spectralogic.dsbrowser.gui.components.metadata;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.networking.Metadata;
import com.spectralogic.dsbrowser.gui.util.ByteFormat;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class MetadataPresenter implements Initializable {

    private final Logger LOG = LoggerFactory.getLogger(MetadataPresenter.class);

    @FXML
    Label objectName;

    @FXML
    Label objectSize;

    @FXML
    TableView<MetadataEntry> metadataTable;

    @Inject
    Ds3Metadata ds3Metadata;

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
        metadata.keys().stream().forEach(key -> metadata.get(key).stream().forEach(value -> builder.add(new MetadataEntry(key, value))));

        metadataTable.setItems(FXCollections.observableList(builder.build()));
    }

    private void initLabels() {
        objectName.setText(ds3Metadata.getName());
        objectSize.setText(ByteFormat.humanReadableByteCount(ds3Metadata.getSize(), false));
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
