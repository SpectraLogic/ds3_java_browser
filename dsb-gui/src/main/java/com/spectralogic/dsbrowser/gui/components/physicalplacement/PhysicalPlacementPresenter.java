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

package com.spectralogic.dsbrowser.gui.components.physicalplacement;


import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.models.*;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import javax.inject.Inject;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

@Presenter
public class PhysicalPlacementPresenter implements Initializable {
    private final static Logger LOG = LoggerFactory.getLogger(PhysicalPlacementPresenter.class);

    @FXML
    private TableView<PhysicalPlacementPoolEntryModel> physicalPlacementDataTablePool;

    @FXML
    private TableView<PhysicalPlacementTapeEntryModel> physicalPlacementDataTableTape;

    @FXML
    private TableView<PhysicalPlacementReplicationEntryModel> physicalPlacementReplication;

    @FXML
    private VBox parentVBox;

    @ModelContext
    private PhysicalPlacement ds3PhysicalPlacement;

    private final Ds3Common ds3Common;
    private final LoggingService loggingService;

    @Inject
    public PhysicalPlacementPresenter(
            final Ds3Common ds3Common,
            final LoggingService loggingService
    ) {
        this.ds3Common = ds3Common;
        this.loggingService = loggingService;
    }


    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initTable();
        } catch (final Throwable t) {
            LOG.error("Failed to initialize PhysicalPlacementPresenter", t);
        }
    }

    private void initTable() {
        final Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        parentVBox.setMaxWidth(primaryScreenBounds.getWidth()-100);
        parentVBox.setPrefWidth(primaryScreenBounds.getWidth()-100);

        physicalPlacementDataTablePool.setMaxWidth(primaryScreenBounds.getWidth()-120);
        physicalPlacementDataTableTape.setMaxWidth(primaryScreenBounds.getWidth()-120);
        physicalPlacementReplication.setMaxWidth(primaryScreenBounds.getWidth()-120);

        physicalPlacementDataTablePool.setPrefWidth(primaryScreenBounds.getWidth()-120);
        physicalPlacementDataTableTape.setPrefWidth(primaryScreenBounds.getWidth()-120);
        physicalPlacementReplication.setPrefWidth(primaryScreenBounds.getWidth()-120);

        if (ds3PhysicalPlacement != null) {
            physicalPlacementDataTablePool.setItems(getStoragePools(ds3PhysicalPlacement.getPools()));
            Platform.runLater(() -> {
                physicalPlacementDataTableTape.setItems(getStorageTapes(ds3PhysicalPlacement.getTapes()));
            });
            physicalPlacementReplication.setItems(getStorageReplication(ds3PhysicalPlacement.getDs3Targets()));
        }
    }

    private SortedList<PhysicalPlacementPoolEntryModel> getStoragePools(final List<Pool> listPool) {
        if (Guard.isNullOrEmpty(listPool)) {
            return null;
        }

        final ImmutableList.Builder<PhysicalPlacementPoolEntryModel> physicalPlacementPoolEntryBuilder = ImmutableList.builder();
        for (final Pool pool : listPool) {
            final String name = pool.getName();
            final String health = pool.getHealth().toString();
            final String S3poolType = pool.getType().toString();
            final String partition = pool.getPartitionId().toString();
            physicalPlacementPoolEntryBuilder.add(new PhysicalPlacementPoolEntryModel(name, health, S3poolType, partition));
        }
        final SortedList<PhysicalPlacementPoolEntryModel> poolSortedList = new SortedList<>(FXCollections.observableList(physicalPlacementPoolEntryBuilder.build()));
        poolSortedList.comparatorProperty().bind(physicalPlacementDataTablePool.comparatorProperty());
        return poolSortedList;
    }

    private SortedList<PhysicalPlacementTapeEntryModel> getStorageTapes(final List<Tape> listTape) {
        if (Guard.isNullOrEmpty(listTape)) {
            return null;
        }

        final ImmutableList.Builder<PhysicalPlacementTapeEntryModel> physicalPlacementTapeEntryBuilder = ImmutableList.builder();
        for (final Tape tape : listTape) {
            final String barcode = tape.getBarCode();
            final String serialNumber = tape.getSerialNumber();
            final String type = tape.getType();
            final String state = tape.getState().toString();
            final boolean writeProtected = tape.getWriteProtected();
            final long availableCapacity = tape.getAvailableRawCapacity();
            final long usedCapacity = (tape.getTotalRawCapacity() - tape.getAvailableRawCapacity());
            final String tapePartition = tape.getPartitionId().toString();
            final String lastModified = tape.getLastModified().toString();
            final String ejectLabel = tape.getEjectLabel();
            final String ejectLocation = tape.getEjectLocation();
            physicalPlacementTapeEntryBuilder.add(new PhysicalPlacementTapeEntryModel(ds3Common, loggingService, barcode, serialNumber, type, state, writeProtected, availableCapacity, usedCapacity, tapePartition, lastModified, ejectLabel, ejectLocation));
        }
        final SortedList<PhysicalPlacementTapeEntryModel> tapeSortedList = new SortedList<>(FXCollections.observableList(physicalPlacementTapeEntryBuilder.build()));
        tapeSortedList.comparatorProperty().bind(physicalPlacementDataTableTape.comparatorProperty());
        return tapeSortedList;
    }

    private SortedList<PhysicalPlacementReplicationEntryModel> getStorageReplication(final List<Ds3Target> ds3Targets) {
        if (Guard.isNullOrEmpty(ds3Targets)) {
            return null;
        }

        final ImmutableList.Builder<PhysicalPlacementReplicationEntryModel> physicalPlacementReplicationEntryBuilder = ImmutableList.builder();
        for (final Ds3Target ds3Target : ds3Targets) {
            final Ds3TargetAccessControlReplication accessControlReplication = ds3Target.getAccessControlReplication();
            final String adminAuthId = ds3Target.getAdminAuthId();
            final String dataPathEndPoint = ds3Target.getDataPathEndPoint();
            final int dataPathPort = ds3Target.getDataPathPort();
            final UUID id = ds3Target.getId();
            final String name = ds3Target.getName();
            final boolean permitGoingOutOfSync = ds3Target.getPermitGoingOutOfSync();
            final Quiesced quiesced = ds3Target.getQuiesced();
            final TargetState state = ds3Target.getState();
            physicalPlacementReplicationEntryBuilder.add(new PhysicalPlacementReplicationEntryModel(dataPathEndPoint, dataPathPort, adminAuthId, id, name, accessControlReplication, state, permitGoingOutOfSync, quiesced));
        }
        final SortedList<PhysicalPlacementReplicationEntryModel> replicationSortedList = new SortedList<>(FXCollections.observableList(physicalPlacementReplicationEntryBuilder.build()));
        replicationSortedList.comparatorProperty().bind(physicalPlacementReplication.comparatorProperty());
        return replicationSortedList;
    }
}
