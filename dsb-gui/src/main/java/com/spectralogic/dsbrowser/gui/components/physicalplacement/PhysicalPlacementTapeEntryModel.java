package com.spectralogic.dsbrowser.gui.components.physicalplacement;

import java.util.Date;

public class PhysicalPlacementTapeEntryModel {
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

    public PhysicalPlacementTapeEntryModel(final String barcode, final String serialNO, final String type, final String state, final Date lastTapeError, final boolean writeProtected,
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
