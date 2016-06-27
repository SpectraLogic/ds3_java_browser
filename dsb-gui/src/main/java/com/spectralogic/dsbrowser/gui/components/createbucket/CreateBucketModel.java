package com.spectralogic.dsbrowser.gui.components.createbucket;

/**
 * Created by Rahul on 6/16/2016.
 */
public class CreateBucketModel {

    private final String dataPolicy;

    //to be added more

    public CreateBucketModel() {
            this("");
    }

    public CreateBucketModel(String dataPolicy) {
        this.dataPolicy = dataPolicy;
    }

    public String getDataPolicy() {
        return dataPolicy;
    }
}
