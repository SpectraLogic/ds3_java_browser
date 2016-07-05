package com.spectralogic.dsbrowser.gui.components.physicalplacement;


import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.models.PhysicalPlacement;
import com.spectralogic.ds3client.models.Pool;
import com.spectralogic.ds3client.models.Tape;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;


public class PhysicalPlacementPresenter implements Initializable {

    private final Logger LOG = LoggerFactory.getLogger(PhysicalPlacementPresenter.class);

    @Inject
    private Ds3PhysicalPlacement ds3PhysicalPlacement;

    @FXML
    private TableView<PhysicalPlacementPoolEntry> physicalPlacementDataTable;

    @FXML
    private TableView<PhysicalPlacementTapeEntry> physicalPlacementDataTableTape;


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
        final ImmutableList.Builder<PhysicalPlacementTapeEntry> physicalPlacementTapeEntryBuilder  = ImmutableList.builder();
        final PhysicalPlacement physicalPlacement = ds3PhysicalPlacement.getPhysicalPlacement();
        List<Pool> listPool = physicalPlacement.getPools();
        for (Pool pool : listPool) {
            String name = pool.getName();
            String health = pool.getHealth().toString();
            String poolType = pool.getType().toString();
            String partition = pool.getPartitionId().toString();
            physicalPlacementPoolEntryBuilder.add(new PhysicalPlacementPoolEntry(name, health, poolType, partition));
        }
        physicalPlacementDataTable.setItems(FXCollections.observableList(physicalPlacementPoolEntryBuilder.build()));

        List<Tape> listTape = physicalPlacement.getTapes();
        for (Tape tape : listTape) {
            String barcode = tape.getBarCode();
            String serialNo = tape.getSerialNumber();
            String type = tape.getType().toString();
            String state = tape.getState().toString();
            String lastTapeError = tape.getLastVerified().toString();
            boolean writeProtected = tape.getWriteProtected();
            boolean available = tape.getAssignedToStorageDomain();
            long used = (tape.getTotalRawCapacity() - tape.getAvailableRawCapacity());
            String tapePartition = tape.getPartitionId().toString();
            String lastModified = tape.getLastModified().toString();
            physicalPlacementTapeEntryBuilder.add(new PhysicalPlacementTapeEntry(barcode, serialNo, type, state, lastTapeError, writeProtected, available, used, tapePartition, lastModified));
        }
        physicalPlacementDataTableTape.setItems(FXCollections.observableList(physicalPlacementTapeEntryBuilder.build()));
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
        private final String seriolNO;
        private final String type;
        private final String state;
        private final String lastTapeError;
        private final boolean writeProtected;
        private final boolean available;
        private final long used;
        private final String tapePartition;
        private final String lastModified;

        private PhysicalPlacementTapeEntry(final String barcode, final String seriolNO, final String type, final String state, final String lastTapeError, final boolean writeProtected,
                                           final boolean available, final long used, final String tapePartition, final String lastModified) {
            this.barcode = barcode;
            this.seriolNO = seriolNO;
            this.type = type;
            this.state = state;
            this.lastTapeError = lastTapeError;
            this.writeProtected = writeProtected;
            this.available = available;
            this.used = used;
            this.tapePartition = tapePartition;
            this.lastModified = lastModified;

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

        public String getLastTapeError() {
            return lastTapeError;
        }

        public String getSeriolNO() {
            return seriolNO;
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
    }
}
