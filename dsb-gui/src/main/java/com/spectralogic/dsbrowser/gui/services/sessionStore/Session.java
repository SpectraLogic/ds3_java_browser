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

package com.spectralogic.dsbrowser.gui.services.sessionStore;

import com.spectralogic.ds3client.Ds3Client;

import java.io.Closeable;
import java.io.IOException;

public class Session implements Closeable {
    private final Ds3Client client;
    private final String sessionName;
    private final String endpoint;
    private final String portNo;
    private final String proxyServer;

    // Needed for injection
    public Session() {
        this(null, null, null, null, null);
    }

    public Session(final String sessionName, final String endpoint, final String portNo, final String proxyServer, final Ds3Client client) {
        this.sessionName = sessionName;
        this.endpoint = endpoint;
        this.portNo = portNo;
        this.proxyServer = proxyServer;
        this.client = client;
    }

    public Ds3Client getClient() {
        return client;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    public String getSessionName() {
        return sessionName;
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
}
