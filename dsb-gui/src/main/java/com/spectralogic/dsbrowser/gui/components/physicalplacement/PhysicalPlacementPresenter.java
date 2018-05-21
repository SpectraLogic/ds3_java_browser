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


import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

@Presenter
public class PhysicalPlacementPresenter implements Initializable {
    private final static Logger LOG = LoggerFactory.getLogger(PhysicalPlacementPresenter.class);

    @FXML
    private TableView<PoolEntry> physicalPlacementDataTablePool;

    @FXML
    private TableView<TapeEntry> physicalPlacementDataTableTape;

    @FXML
    private TableView<ReplicationEntry> physicalPlacementReplication;

    @FXML
    private VBox parentVBox;

    @ModelContext
    private PhysicalPlacementModel physicalPlacementModel;

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
        parentVBox.setMaxWidth(primaryScreenBounds.getWidth() - 100);
        parentVBox.setPrefWidth(primaryScreenBounds.getWidth() - 100);

        physicalPlacementDataTablePool.setMaxWidth(primaryScreenBounds.getWidth() - 120);
        physicalPlacementDataTableTape.setMaxWidth(primaryScreenBounds.getWidth() - 120);
        physicalPlacementReplication.setMaxWidth(primaryScreenBounds.getWidth() - 120);

        physicalPlacementDataTablePool.setPrefWidth(primaryScreenBounds.getWidth() - 120);
        physicalPlacementDataTableTape.setPrefWidth(primaryScreenBounds.getWidth() - 120);
        physicalPlacementReplication.setPrefWidth(primaryScreenBounds.getWidth() - 120);

        if (physicalPlacementModel != null) {
            physicalPlacementDataTablePool.getItems().addAll(physicalPlacementModel.getPoolEntries());
            physicalPlacementDataTableTape.getItems().addAll(physicalPlacementModel.getTapeEntries());
            physicalPlacementReplication.getItems().addAll(physicalPlacementModel.getReplicationEntries());
        }
    }

}
