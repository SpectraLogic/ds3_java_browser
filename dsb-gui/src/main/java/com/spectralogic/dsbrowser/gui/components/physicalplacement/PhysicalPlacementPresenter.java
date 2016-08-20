package com.spectralogic.dsbrowser.gui.components.physicalplacement;


import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.models.PhysicalPlacement;
import com.spectralogic.ds3client.models.Pool;
import com.spectralogic.ds3client.models.Tape;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;


public class PhysicalPlacementPresenter implements Initializable {

    private final Logger LOG = LoggerFactory.getLogger(PhysicalPlacementPresenter.class);

    @FXML
    private TableView<PhysicalPlacementPoolEntry> physicalPlacementDataTable;

    @FXML
    private TableView<PhysicalPlacementTapeEntry> physicalPlacementDataTableTape;

    @Inject
    private PhysicalPlacement ds3PhysicalPlacement;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initTable();
        } catch (final Throwable e) {
            LOG.error("Failed to create Physical Placement presenter", e);
        }
    }

    private void initTable() {
        final ImmutableList.Builder<PhysicalPlacementPoolEntry> physicalPlacementPoolEntryBuilder = ImmutableList.builder();
        final ImmutableList.Builder<PhysicalPlacementTapeEntry> physicalPlacementTapeEntryBuilder = ImmutableList.builder();

        if (ds3PhysicalPlacement != null) {
            final List<Pool> listPool = ds3PhysicalPlacement.getPools();
            if (listPool != null) {
                for (final Pool pool : listPool) {
                    physicalPlacementPoolEntryBuilder.add(new PhysicalPlacementPoolEntry(pool.getName(), pool.getHealth().toString(), pool.getType().toString(), pool.getPartitionId().toString()));
                }
                physicalPlacementDataTable.setItems(FXCollections.observableList(physicalPlacementPoolEntryBuilder.build()));
            }
            final List<Tape> listTape = ds3PhysicalPlacement.getTapes();
            if (listTape != null) {
                for (final Tape tape : listTape) {
                    final String barcode = tape.getBarCode();
                    final String serialNo = tape.getSerialNumber();
                    final String type = tape.getType().toString();
                    final String state = tape.getState().toString();
                    final Date lastTapeError = tape.getLastVerified();
                    final boolean writeProtected = tape.getWriteProtected();
                    final boolean available = tape.getAssignedToStorageDomain();
                    final long used = (tape.getTotalRawCapacity() - tape.getAvailableRawCapacity());
                    final String tapePartition = tape.getPartitionId().toString();
                    final String lastModified = tape.getLastModified().toString();
                    final String ejectLabel = tape.getEjectLabel();
                    final String ejectLocation = tape.getEjectLocation();
                    physicalPlacementTapeEntryBuilder.add(new PhysicalPlacementTapeEntry(barcode, serialNo, type, state, lastTapeError, writeProtected, available, used, tapePartition, lastModified, ejectLabel, ejectLocation));
                }
                physicalPlacementDataTableTape.setItems(FXCollections.observableList(physicalPlacementTapeEntryBuilder.build()));
            }
        }
    }

    public static class PhysicalPlacementPoolEntry {
        private final String name;
        private final String health;
        private final String S3poolType;
        private final String partition;

        private PhysicalPlacementPoolEntry(final String name, final String health, final String S3poolType, final String partition) {
            this.name = name;
            this.S3poolType = S3poolType;
            this.health = health;
            this.partition = partition;
        }

        public String getHealth() {
            return health;
        }

        public String getName() {
            return name;
        }

        public String getS3poolType() {
            return S3poolType;
        }

        public String getPartition() {
            return partition;
        }
    }

    public static class PhysicalPlacementTapeEntry {
        private final String barcode;
        private final String serialNO;
        private final String type;
        private final String state;
        private final Date lastTapeError;
        private final boolean writeProtected;
        private final boolean available;
        private final long used;
        private final String tapePartition;
        private final String lastModified;
        private final String ejectLabel;
        private final String ejectLocation;

        private PhysicalPlacementTapeEntry(final String barcode, final String serialNO, final String type, final String state, final Date lastTapeError, final boolean writeProtected,
                                           final boolean available, final long used, final String tapePartition, final String lastModified, final String ejectLabel, final String ejectLocation) {
            this.barcode = barcode;
            this.serialNO = serialNO;
            this.type = type;
            this.state = state;
            this.lastTapeError = lastTapeError;
            this.writeProtected = writeProtected;
            this.available = available;
            this.used = used;
            this.tapePartition = tapePartition;
            this.lastModified = lastModified;
            this.ejectLabel = ejectLabel;
            this.ejectLocation = ejectLocation;

        }

        public boolean isAvailable() {
            return available;
        }

        public boolean isWriteProtected() {
            return writeProtected;
        }

        public String getBarcode() {
            return barcode;
        }

        public Date getLastTapeError() {
            return lastTapeError;
        }

        public String getSerialNO() {
            return serialNO;
        }

        public String getState() {
            return state;
        }

        public long getUsed() {
            return used;
        }

        public String getType() {
            return type;
        }

        public String getLastModified() {
            return lastModified;
        }

        public String getTapePartition() {
            return tapePartition;
        }

        public String getEjectLabel() {
            return ejectLabel;
        }

        public String getEjectLocation() {
            return ejectLocation;
        }
    }
}
