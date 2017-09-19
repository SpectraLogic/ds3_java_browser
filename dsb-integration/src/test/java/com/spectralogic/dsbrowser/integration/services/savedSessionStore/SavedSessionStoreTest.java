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

package com.spectralogic.dsbrowser.integration.services.savedSessionStore;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactfx.collection.LiveArrayList;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class SavedSessionStoreTest {
    private static Session session;
    private static SavedSession savedSession;
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    final private static String testSessionName = "SavedSesionsToResest";
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();

    @BeforeClass
    public static void setUp() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        new JFXPanel();
        Platform.runLater(() -> {
            savedSession = new SavedSession(
                    testSessionName,
                    client.getConnectionDetails().getEndpoint(),
                    "80",
                    null,
                    new SavedCredentials(
                            client.getConnectionDetails().getCredentials().getClientId(),
                            client.getConnectionDetails().getCredentials().getKey()),
                    false,
                    false);
            session = CreateConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle, buildInfoService);
            latch.countDown();
        });
        latch.await();
    }

    @Test
    public void loadSavedSessionStoreTest() throws Exception {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore(resourceBundle, buildInfoService);
        assertNotNull(savedSessionStore);
    }

    @Test
    public void saveSavedSessionStoreTest() throws Exception {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore(resourceBundle, buildInfoService);
        savedSession = new SavedSession(
                "NewSession1",
                client.getConnectionDetails().getEndpoint(),
                "80",
                null,
                new SavedCredentials(
                        client.getConnectionDetails().getCredentials().getClientId(),
                        client.getConnectionDetails().getCredentials().getKey()),
                false,
                false);
        final int previousSize = savedSessionStore.getSessions().size();
        savedSessionStore.getSessions().add(savedSession);
        SavedSessionStore.saveSavedSessionStore(savedSessionStore);
        final  int currentSize=savedSessionStore.getSessions().size();
        assertTrue((previousSize + 1) == currentSize);
    }

    @Test
    public void addSessionTest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final SavedSessionStore savedSessionStore1 = SavedSessionStore.loadSavedSessionStore(resourceBundle, buildInfoService);
                savedSessionStore1.addSession(session);
                final SavedSessionStore savedSessionStore2 = SavedSessionStore.loadSavedSessionStore(resourceBundle, buildInfoService);
                final Optional<SavedSession> session = savedSessionStore2.getSessions().stream().filter(savedSession ->
                        savedSession.getName().equals(testSessionName)).findFirst();
                successFlag = null != session;
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

    @Test
    public void isSessionUpdatedTest() throws IOException {
        final ObservableList<SavedSession> savedSessions = SavedSessionStore.loadSavedSessionStore(resourceBundle, buildInfoService).getSessions();
        session = CreateConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle, buildInfoService);
        assertFalse(SavedSessionStore.containsSessionName(savedSessions, session.getSessionName()));
    }

    @Test
    public void containsSessionNameTest() throws IOException {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore(resourceBundle, buildInfoService);
        assertTrue(savedSessionStore.containsSessionName(savedSessionStore.getSessions(), testSessionName));
    }

    @Test
    public void containsNewSessionNameTest() throws IOException {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore(resourceBundle, buildInfoService);
        final ObservableList<Session> list = new LiveArrayList<>();
        list.add(session);
        assertTrue(savedSessionStore.containsNewSessionName(list, testSessionName));
    }

    @Test
    public void removeSessionTest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore(resourceBundle, buildInfoService);
                savedSessionStore.removeSession(savedSession);
                successFlag = !savedSessionStore.getSessions().contains(savedSession.getName());
                latch.countDown();
            } catch (final IOException e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}