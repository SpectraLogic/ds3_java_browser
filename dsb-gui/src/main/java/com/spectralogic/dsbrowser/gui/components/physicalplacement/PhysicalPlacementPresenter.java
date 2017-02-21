package com.spectralogic.dsbrowser.gui.components.physicalplacement;


import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.models.*;
import com.spectralogic.ds3client.utils.Guard;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
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
import java.util.UUID;


public class PhysicalPlacementPresenter implements Initializable {

    private final Logger LOG = LoggerFactory.getLogger(PhysicalPlacementPresenter.class);

    @FXML
    private TableView<PhysicalPlacementPoolEntryModel> physicalPlacementDataTablePool;

    @FXML
    private TableView<PhysicalPlacementTapeEntryModel> physicalPlacementDataTableTape;

    @FXML
    private TableView<PhysicalPlacementReplicationEntryModel> physicalPlacementReplication;


    @Inject
    private PhysicalPlacement ds3PhysicalPlacement;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initTable();
        } catch (final Exception e) {
            LOG.error("Failed to create Physical Placement presenter", e);
        }
    }

    private void initTable() {
        if (ds3PhysicalPlacement != null) {
            physicalPlacementDataTablePool.setItems(getStoragePools(ds3PhysicalPlacement.getPools()));
            physicalPlacementDataTableTape.setItems(getStorageTapes(ds3PhysicalPlacement.getTapes()));
            physicalPlacementReplication.setItems(getStorageReplication(ds3PhysicalPlacement.getDs3Targets()));
        }
    }

    public SortedList getStoragePools(final List<Pool> listPool) {
        final ImmutableList.Builder<PhysicalPlacementPoolEntryModel> physicalPlacementPoolEntryBuilder = ImmutableList.builder();
        SortedList poolSortedList = null;
        if (Guard.isNotNullAndNotEmpty(listPool)) {
            for (final Pool pool : listPool) {
                final String name = pool.getName();
                final String health = pool.getHealth().toString();
                final String S3poolType = pool.getType().toString();
                final String partition = pool.getPartitionId().toString();
                physicalPlacementPoolEntryBuilder.add(new PhysicalPlacementPoolEntryModel(name, health, S3poolType, partition));
            }
            poolSortedList = new SortedList(FXCollections.observableList(physicalPlacementPoolEntryBuilder.build()));
            poolSortedList.comparatorProperty().bind(physicalPlacementDataTablePool.comparatorProperty());
        }
        return poolSortedList;
    }

    public SortedList getStorageTapes(final List<Tape> listTape) {
        final ImmutableList.Builder<PhysicalPlacementTapeEntryModel> physicalPlacementTapeEntryBuilder = ImmutableList.builder();
        SortedList tapeSortedList = null;
        if (Guard.isNotNullAndNotEmpty(listTape)) {
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
                physicalPlacementTapeEntryBuilder.add(new PhysicalPlacementTapeEntryModel(barcode, serialNo, type, state, lastTapeError, writeProtected, available, used, tapePartition, lastModified, ejectLabel, ejectLocation));
            }
            tapeSortedList = new SortedList(FXCollections.observableList(physicalPlacementTapeEntryBuilder.build()));
            tapeSortedList.comparatorProperty().bind(physicalPlacementDataTableTape.comparatorProperty());
        }
        return tapeSortedList;
    }

    public SortedList getStorageReplication(final List<Ds3Target> ds3Targets) {
        final ImmutableList.Builder<PhysicalPlacementReplicationEntryModel> physicalPlacementReplicationEntryBuilder = ImmutableList.builder();
        SortedList replicationSortedList = null;
        if (ds3Targets != null) {
            for (final Ds3Target ds3Target : ds3Targets) {
                final Ds3TargetAccessControlReplication accessControlReplication = ds3Target.getAccessControlReplication();
                final String adminAuthId = ds3Target.getAdminAuthId();
                final String adminSecretKey = ds3Target.getAdminSecretKey();
                final String dataPathEndPoint = ds3Target.getDataPathEndPoint();
                final boolean dataPathHttps = ds3Target.getDataPathHttps();
                final int dataPathPort = ds3Target.getDataPathPort();
                final String dataPathProxy = ds3Target.getDataPathProxy();
                final boolean dataPathVerifyCertificate = ds3Target.getDataPathVerifyCertificate();
                final TargetReadPreferenceType defaultReadPreference = ds3Target.getDefaultReadPreference();
                final UUID id = ds3Target.getId();
                final String name = ds3Target.getName();
                final boolean permitGoingOutOfSync = ds3Target.getPermitGoingOutOfSync();
                final Quiesced quiesced = ds3Target.getQuiesced();
                final String replicatedUserDefaultDataPolicy = ds3Target.getReplicatedUserDefaultDataPolicy();
                final TargetState state = ds3Target.getState();
                physicalPlacementReplicationEntryBuilder.add(new PhysicalPlacementReplicationEntryModel(accessControlReplication, adminAuthId, adminSecretKey, state, dataPathEndPoint, dataPathHttps, dataPathPort, dataPathProxy, dataPathVerifyCertificate, defaultReadPreference, id, name, permitGoingOutOfSync, quiesced, replicatedUserDefaultDataPolicy));
            }
            replicationSortedList = new SortedList(FXCollections.observableList(physicalPlacementReplicationEntryBuilder.build()));
            replicationSortedList.comparatorProperty().bind(physicalPlacementReplication.comparatorProperty());
        }
        return replicationSortedList;
    }
}
