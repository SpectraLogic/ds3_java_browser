package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertTrue;


public class Ds3DeleteBucketTaskTest {
    private final Workers workers = new Workers();
    private Session session;
    private boolean successFlag = false;

    @Before
    public void setUp() throws Exception {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME,
                    SessionConstants.SESSION_PATH, SessionConstants.PORT_NO, null,
                    new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY),
                    false);
            session = new CreateConnectionTask().createConnection(
                    SessionModelService.setSessionModel(savedSession, false));
        });
    }

    @Test
    public void deleteBucket() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Ds3DeleteBucketTask deleteBucketTask = new Ds3DeleteBucketTask(session.getClient(),
                        SessionConstants.DS3_PANEL_SERVICE_TEST_BUCKET_NAME);
                workers.execute(deleteBucketTask);
                deleteBucketTask.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();
                });
                deleteBucketTask.setOnFailed(event -> {
                    latch.countDown();
                });
                deleteBucketTask.setOnCancelled(event -> {
                    latch.countDown();
                });
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}