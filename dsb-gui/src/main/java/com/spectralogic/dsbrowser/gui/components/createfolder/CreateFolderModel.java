package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.spectralogic.ds3client.Ds3Client;

public class CreateFolderModel {

    private final Ds3Client client;

    private final String location;

    private final String bucketName;

    public String getBucketName() {
        return bucketName;
    }

    public String getLocation() {
        return location;
    }

    public Ds3Client getClient() {
        return client;
    }

    public CreateFolderModel() {
        this(null,"","");

    }

    public CreateFolderModel(Ds3Client client, String location, String bucketName) {
        this.client=client;
        this.location=location;
        this.bucketName=bucketName;
    }
}
