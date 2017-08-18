/*
 * ******************************************************************************
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
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.integration;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.util.JsonMapping;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class JsonMappingTest {
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();

    @Test
    public void fromJson() throws Exception {
        final Path PATH = Paths.get(System.getProperty("user.home"), ".dsbrowser", "sessions.json");
        final InputStream inputStream = Files.newInputStream(PATH);
        final SavedSessionStore.SerializedSessionStore serializedSessionStore = JsonMapping.fromJson(inputStream, SavedSessionStore.SerializedSessionStore.class);

        final SavedSession savedSession = new SavedSession(
                "SM2U-11",
                client.getConnectionDetails().getEndpoint(),
                "80",
                null,
                new SavedCredentials(
                        client.getConnectionDetails().getCredentials().getClientId(),
                        client.getConnectionDetails().getCredentials().getKey()),
                false,
                false);

        assertThat(serializedSessionStore.getSessions().get(0).getEndpoint(), is(savedSession.getEndpoint()));
        assertThat(serializedSessionStore.getSessions().get(0).getName(), is(savedSession.getName()));
        assertThat(serializedSessionStore.getSessions().get(0).getCredentials().getAccessId(), is(savedSession.getCredentials().getAccessId()));
        assertThat(serializedSessionStore.getSessions().get(0).getPortNo(), is(savedSession.getPortNo()));

    }

    @Test
    public void toJson() throws Exception {
        final Path PATH = Paths.get(System.getProperty("user.home"), ".dsbrowser", "sessions.json");
        final InputStream inputStream = Files.newInputStream(PATH);
        final SavedSessionStore.SerializedSessionStore serializedSessionStore = JsonMapping.fromJson(inputStream, SavedSessionStore.SerializedSessionStore.class);
        final SavedSession savedSession = new SavedSession(
                "SM2U-11",
                client.getConnectionDetails().getEndpoint(),
                "80",
                null,
                new SavedCredentials(
                        client.getConnectionDetails().getCredentials().getClientId(),
                        client.getConnectionDetails().getCredentials().getKey()),
                false,
                false);

        assertThat(serializedSessionStore.getSessions().get(0).getEndpoint(), is(savedSession.getEndpoint()));
        assertThat(serializedSessionStore.getSessions().get(0).getName(), is(savedSession.getName()));
        assertThat(serializedSessionStore.getSessions().get(0).getCredentials().getAccessId(), is(savedSession.getCredentials().getAccessId()));
        assertThat(serializedSessionStore.getSessions().get(0).getPortNo(), is(savedSession.getPortNo()));
    }

}