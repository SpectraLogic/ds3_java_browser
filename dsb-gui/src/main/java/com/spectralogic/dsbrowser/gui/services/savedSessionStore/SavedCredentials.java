package com.spectralogic.dsbrowser.gui.services.savedSessionStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spectralogic.ds3client.models.common.Credentials;

public class SavedCredentials {
    @JsonProperty("accessId")
    private final String accessId;
    @JsonProperty("secretKey")
    private final String secretKey;

    @JsonCreator
    public SavedCredentials(@JsonProperty("accessId") final String accessId, @JsonProperty("secretKey") final String secretKey) {
        this.accessId = accessId;
        this.secretKey = secretKey;
    }

    public static SavedCredentials fromCredentials(final Credentials credentials) {
        return new SavedCredentials(credentials.getClientId(), credentials.getKey());
    }

    public String getAccessId() {
        return accessId;
    }

    public String getSecretKey() {
        return secretKey;
    }
    public Credentials toCredentials() {
        return new Credentials(this.accessId, this.secretKey);
    }

}
