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

import com.spectralogic.ds3client.models.Ds3TargetAccessControlReplication;
import com.spectralogic.ds3client.models.Quiesced;
import com.spectralogic.ds3client.models.TargetState;

import java.util.UUID;

public class PhysicalPlacementReplicationEntryModel {
    private final String dataPathEndPoint;
    private final int dataPathPort;
    private final String authId;
    private final UUID id;
    private final String name;
    private final Ds3TargetAccessControlReplication accessControlReplication;
    private final TargetState state;
    private final boolean permitGoingOutOfSync;
    private final Quiesced quiesced;

    public PhysicalPlacementReplicationEntryModel(
            final String dataPathEndPoint,
            final int dataPathPort,
            final String authId,
            final UUID id,
            final String name,
            final Ds3TargetAccessControlReplication accessControlReplication,
            final TargetState state,
            final boolean permitGoingOutOfSync,
            final Quiesced quiesced) {
        this.accessControlReplication = accessControlReplication;
        this.authId = authId;
        this.dataPathEndPoint = dataPathEndPoint;
        this.state = state;
        this.dataPathPort = dataPathPort;
        this.id = id;
        this.name = name;
        this.permitGoingOutOfSync = permitGoingOutOfSync;
        this.quiesced = quiesced;
    }

    public Ds3TargetAccessControlReplication getAccessControlReplication() {
        return accessControlReplication;
    }

    public String getAuthId() {
        return authId;
    }

    public String getDataPathEndPoint() {
        return dataPathEndPoint;
    }

    public TargetState getState() {
        return state;
    }

    public int getDataPathPort() {
        return dataPathPort;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isPermitGoingOutOfSync() {
        return permitGoingOutOfSync;
    }

    public Quiesced getQuiesced() {
        return quiesced;
    }
}

