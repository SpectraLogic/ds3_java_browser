package com.spectralogic.dsbrowser.gui.services.savedSessionStore;

import com.spectralogic.ds3client.models.Credentials;

public class SavedSession {
    private final String name;
    private final String endpoint;
    private final Credentials credentials;

    public SavedSession(final String name, final String endpoint, final Credentials credentials) {
        this.name = name;
        this.endpoint = endpoint;
        this.credentials = credentials;
    }

    public String getName() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Credentials getCredentials() {
        return credentials;
    }
}
