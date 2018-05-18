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

import com.spectralogic.ds3client.commands.spectrads3.GetTapePartitionSpectraS3Request;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormatKt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PhysicalPlacementTapeEntryModel {
    private final static Logger LOG = LoggerFactory.getLogger(PhysicalPlacementTapeEntryModel.class);

    private final String barcode;
    private final String serialNumber;
    private final String type;
    private final String state;
    private final boolean writeProtected;
    private final String availableCapacity;
    private final String usedCapacity;
    private final String tapePartition;
    private final String lastModified;
    private final String ejectLabel;
    private final String ejectLocation;

    private final Ds3Common ds3Common;
    private final LoggingService loggingService;

    PhysicalPlacementTapeEntryModel(
            final Ds3Common ds3Common,
            final LoggingService loggingService,
            final String barcode,
            final String serialNumber,
            final String type,
            final String state,
            final boolean writeProtected,
            final long availableCapacity,
            final long usedCapacity,
            final String tapePartition,
            final String lastModified,
            final String ejectLabel,
            final String ejectLocation
    ) {
        this.ds3Common = ds3Common;
        this.loggingService = loggingService;
        this.barcode = barcode;
        this.serialNumber = serialNumber;
        this.type = type;
        this.state = state;
        this.writeProtected = writeProtected;
        this.availableCapacity = FileSizeFormatKt.toByteRepresentation(availableCapacity);
        this.usedCapacity = FileSizeFormatKt.toByteRepresentation(usedCapacity);
        this.tapePartition = tapePartition;
        this.lastModified = lastModified;
        this.ejectLabel = ejectLabel;
        this.ejectLocation = ejectLocation;

    }

    public boolean isWriteProtected() {
        return writeProtected;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getSerialNumber() {
        return serialNumber;
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
        try {
            return ds3Common
                    .getCurrentSession()
                    .getClient()
                    .getTapePartitionSpectraS3(new GetTapePartitionSpectraS3Request(tapePartition))
                    .getTapePartitionResult()
                    .getName();
        } catch (final IOException e) {
            LOG.error("Unable to get name of tape Partition", e);
            loggingService.logInternationalMessage("unableToLookUpTapePartition", LogType.ERROR);
            return tapePartition;
        }
    }

    public String getEjectLabel() {
        return ejectLabel;
    }

    public String getEjectLocation() {
        return ejectLocation;
    }
}
