/*
 * ****************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *  ****************************************************************************
 */

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
    @JsonProperty("useSSL")
    private final Boolean useSSL;

    @JsonCreator
    public SavedSession(@JsonProperty("name") final String name, @JsonProperty("endpoint") final String endpoint, @JsonProperty("portNo") final String portNo, @JsonProperty("proxyServer") final String proxyServer, @JsonProperty("credentials") final SavedCredentials credentials, @JsonProperty("defaultSession")
    final Boolean defaultSession, @JsonProperty("useSSL") final Boolean useSSL) {
        this.name = name;
        this.endpoint = endpoint;
        this.portNo = portNo;
        this.proxyServer = proxyServer;
        this.credentials = credentials;
        this.defaultSession = defaultSession;
        this.useSSL = useSSL;
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

    public Boolean isDefaultSession() { return defaultSession; }

    public Boolean isUseSSL() { return useSSL; }

}
