package com.spectralogic.dsbrowser.gui.components.physicalplacement

data class TapeEntry(
    val barcode: String,
    val serialNumber: String,
    val type: String,
    val state: String,
    val writeProtected: Boolean,
    val availableCapacity: String,
    val usedCapacity: String,
    val tapePartition: String,
    val lastModified: String,
    val ejectLabel: String,
    val ejectLocation: String
)
