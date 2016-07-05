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
    //ADDED BY VVDN TEAM
    @JsonProperty("portNo")
    private final String portNo;
    @JsonProperty
    private final String proxyServer;

    @JsonCreator
    public SavedSession(@JsonProperty("name") final String name, @JsonProperty("endpoint") final String endpoint, @JsonProperty("portNo") final String portNo, @JsonProperty("proxyServer") final String proxyServer, @JsonProperty("credentials") final SavedCredentials credentials) {
        this.name = name;
        this.endpoint = endpoint;
        this.portNo = portNo;
        this.proxyServer = proxyServer;
        this.credentials = credentials;
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
}
