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

import org.apache.commons.io.FileUtils;

import java.util.Date;

public class PhysicalPlacementTapeEntryModel {
    private final String barcode;
    private final String serialNO;
    private final String type;
    private final String state;
    private final Date lastTapeError;
    private final boolean writeProtected;
    private final boolean assignedToStorageDomain;
    private final String availableCapacity;
    private final String usedCapacity;
    private final String tapePartition;
    private final String lastModified;
    private final String ejectLabel;
    private final String ejectLocation;

    PhysicalPlacementTapeEntryModel(final String barcode,
                                    final String serialNO,
                                    final String type,
                                    final String state,
                                    final Date lastTapeError,
                                    final boolean writeProtected,
                                    final boolean assignedToStorageDomain,
                                    final long availableCapacity,
                                    final long usedCapacity,
                                    final String tapePartition,
                                    final String lastModified,
                                    final String ejectLabel,
                                    final String ejectLocation) {
        this.barcode = barcode;
        this.serialNO = serialNO;
        this.type = type;
        this.state = state;
        this.lastTapeError = lastTapeError;
        this.writeProtected = writeProtected;
        this.assignedToStorageDomain = assignedToStorageDomain;
        this.availableCapacity = FileUtils.byteCountToDisplaySize(availableCapacity);
        this.usedCapacity = FileUtils.byteCountToDisplaySize(usedCapacity);
        this.tapePartition = tapePartition;
        this.lastModified = lastModified;
        this.ejectLabel = ejectLabel;
        this.ejectLocation = ejectLocation;

    }

    public boolean isAssignedToStorageDomain() {
        return assignedToStorageDomain;
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

    public String getAvailableCapacity() {
        return availableCapacity;
    }

    public String getUsedCapacity() {
        return usedCapacity;
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
