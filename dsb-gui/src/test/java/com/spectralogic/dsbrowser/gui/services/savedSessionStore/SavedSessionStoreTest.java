package com.spectralogic.dsbrowser.gui.services.savedSessionStore;

import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactfx.collection.LiveArrayList;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class SavedSessionStoreTest {
    private static Session session;
    private static SavedSession savedSession;
    private boolean successFlag = false;

    @BeforeClass
    public static void setUp() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        new JFXPanel();
        Platform.runLater(() -> {
            savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO,
                    null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);
            session = new CreateConnectionTask().createConnection(SessionModelService.setSessionModel(savedSession, false));
            latch.countDown();
        });
        latch.await();
    }

    @Test
    public void loadSavedSessionStoreTest() throws Exception {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore();
        assertNotNull(savedSessionStore);
    }

    @Test
    public void saveSavedSessionStoreTest() throws Exception {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore();
        savedSession = new SavedSession("NewSession1", SessionConstants.SESSION_PATH, SessionConstants.PORT_NO,
                null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);
        final int previousSize = savedSessionStore.getSessions().size();
        savedSessionStore.getSessions().add(savedSession);
        SavedSessionStore.saveSavedSessionStore(savedSessionStore);
        final  int currentSize=savedSessionStore.getSessions().size();
        assertTrue((previousSize + 1) == currentSize);
    }

    @Test
    public void saveSessionTest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final SavedSessionStore savedSessionStore1 = SavedSessionStore.loadSavedSessionStore();
                savedSessionStore1.saveSession(session);
                final SavedSessionStore savedSessionStore2 = SavedSessionStore.loadSavedSessionStore();
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
        assertTrue(successFlag);
    }

    @Test
    public void isSessionUpdatedTest() throws Exception {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore();
        session = new CreateConnectionTask().createConnection(SessionModelService.setSessionModel(savedSession, false));
        assertFalse(savedSessionStore.isSessionUpdated(savedSession, session));
    }

    @Test
    public void containsSessionNameTest() throws Exception {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore();
        assertTrue(savedSessionStore.containsSessionName(savedSessionStore.getSessions(), SessionConstants.SESSION_NAME));
    }

    @Test
    public void containsNewSessionNameTest() throws Exception {
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore();
        final ObservableList<Session> list = new LiveArrayList<>();
        list.add(session);
        assertTrue(savedSessionStore.containsNewSessionName(list, SessionConstants.SESSION_NAME));
    }

    @Test
    public void removeSessionTest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore();
                savedSessionStore.removeSession(savedSession);
                successFlag = !savedSessionStore.getSessions().contains(savedSession.getName());
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}