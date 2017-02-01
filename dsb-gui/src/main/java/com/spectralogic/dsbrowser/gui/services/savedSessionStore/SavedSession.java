package com.spectralogic.dsbrowser.gui.services.savedSessionStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SavedSession {
    @JsonProperty("name")
    private final String name;
    @JsonProperty("endpoint")
    private final String endpoint;
    @JsonProperty("credentials")
    private final SavedCredentials credentials;
    @JsonProperty("portNo")
    private final String portNo;
    @JsonProperty
    private final String proxyServer;
    @JsonProperty("defaultSession")
    private final Boolean defaultSession;

    @JsonCreator
    public SavedSession(@JsonProperty("name") final String name, @JsonProperty("endpoint") final String endpoint, @JsonProperty("portNo") final String portNo, @JsonProperty("proxyServer") final String proxyServer, @JsonProperty("credentials") final SavedCredentials credentials, @JsonProperty("defaultSession")
    final Boolean defaultSession) {
        this.name = name;
        this.endpoint = endpoint;
        this.portNo = portNo;
        this.proxyServer = proxyServer;
        this.credentials = credentials;
        this.defaultSession=defaultSession;
    }

    public String getName() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getPortNo() {
        return portNo;
    }

    public String getProxyServer() {
        return proxyServer;
    }

    public SavedCredentials getCredentials() {
        return credentials;
    }


    public Boolean getDefaultSession() {
        return defaultSession;
    }

}
