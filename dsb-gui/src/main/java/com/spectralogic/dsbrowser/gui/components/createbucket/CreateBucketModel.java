package com.spectralogic.dsbrowser.gui.components.createbucket;

import java.util.UUID;

public class CreateBucketModel {

    private final String dataPolicy;

    private final UUID id;

    //to be added more

    public CreateBucketModel() {
        this("", null);
    }

    public CreateBucketModel(final String dataPolicy, final UUID id) {
        this.dataPolicy = dataPolicy;
        this.id = id;
    }

    public String getDataPolicy() {
        return dataPolicy;
    }

    public UUID getId() {
        return id;
    }
}
