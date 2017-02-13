package com.spectralogic.dsbrowser.gui.components.physicalplacement;

public class PhysicalPlacementPoolEntryModel {
    private final String name;
    private final String health;
    private final String S3poolType;
    private final String partition;

    public PhysicalPlacementPoolEntryModel(final String name, final String health, final String S3poolType, final String partition) {
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

