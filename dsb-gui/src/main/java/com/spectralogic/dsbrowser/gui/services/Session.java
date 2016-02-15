package com.spectralogic.dsbrowser.gui.services;

import com.spectralogic.ds3client.Ds3Client;

public class Session {
    private final Ds3Client client;

    public Session(final Ds3Client client) {
        this.client = client;
    }

    public Ds3Client getClient() {
        return client;
    }
}
