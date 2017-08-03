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

import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactfx.collection.LiveArrayList;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SavedSessionStoreTest {
    private static Session session;
    private static SavedSession savedSession;
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));

    @BeforeClass
    public static void setUp() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        new JFXPanel();
        Platform.runLater(() -> {
            savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO,
                    null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);
            session = new CreateConnectionTask().createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle);
            latch.countDown();
        });
        latch.await();
    }

    @Test
    public void loadSavedSessionStoreTest() throws Exception {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore(resourceBundle);
        Assert.assertNotNull(savedSessionStore);
    }

    @Test
    public void saveSavedSessionStoreTest() throws Exception {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore(resourceBundle);
        savedSession = new SavedSession("NewSession1", SessionConstants.SESSION_PATH, SessionConstants.PORT_NO,
                null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);
        final int previousSize = savedSessionStore.getSessions().size();
        savedSessionStore.getSessions().add(savedSession);
        SavedSessionStore.saveSavedSessionStore(savedSessionStore);
        final  int currentSize=savedSessionStore.getSessions().size();
        Assert.assertTrue((previousSize + 1) == currentSize);
    }

    @Test
    public void addSessionTest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final SavedSessionStore savedSessionStore1 = SavedSessionStore.loadSavedSessionStore(resourceBundle);
                savedSessionStore1.addSession(session);
                final SavedSessionStore savedSessionStore2 = SavedSessionStore.loadSavedSessionStore(resourceBundle);
                final Optional<SavedSession> session = savedSessionStore2.getSessions().stream().filter(savedSession ->
                        savedSession.getName().equals(SessionConstants.SESSION_NAME)).findFirst();
                successFlag = null != session;
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        Assert.assertTrue(successFlag);
    }

    @Test
    public void isSessionUpdatedTest() throws Exception {
        final ObservableList<SavedSession> savedSessions = SavedSessionStore.loadSavedSessionStore(resourceBundle).getSessions();
        session = new CreateConnectionTask().createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle);
        Assert.assertFalse(SavedSessionStore.loadSavedSessionStore(resourceBundle).containsSessionName(savedSessions, session.getSessionName()));
    }

    @Test
    public void containsSessionNameTest() throws Exception {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore(resourceBundle);
        Assert.assertTrue(savedSessionStore.containsSessionName(savedSessionStore.getSessions(), SessionConstants.SESSION_NAME));
    }

    @Test
    public void containsNewSessionNameTest() throws Exception {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore(resourceBundle);
        final ObservableList<Session> list = new LiveArrayList<>();
        list.add(session);
        Assert.assertTrue(savedSessionStore.containsNewSessionName(list, SessionConstants.SESSION_NAME));
    }

    @Test
    public void removeSessionTest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore(resourceBundle);
                savedSessionStore.removeSession(savedSession);
                successFlag = !savedSessionStore.getSessions().contains(savedSession.getName());
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        Assert.assertTrue(successFlag);
    }
}