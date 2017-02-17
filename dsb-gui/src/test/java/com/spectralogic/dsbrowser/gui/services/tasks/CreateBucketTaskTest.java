package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;


public class CreateBucketTaskTest {
    private final Workers workers = new Workers();
    private Session session;
    private boolean successFlag = false;

    @Before
    public void setUp() throws Exception {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO, null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);
            session = new CreateConnectionTask().createConnection(SessionModelService.setSessionModel(savedSession, false));
        });
    }

    @Test
    public void createBucket() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final CreateBucketModel createBucketModel = new CreateBucketModel(SessionConstants.DATA_POLICY_NAME,
                        UUID.fromString(SessionConstants.DATA_POLICY_UUID));
                final CreateBucketTask createBucketTask = new CreateBucketTask(createBucketModel, session.getClient(),
                        SessionConstants.CREATE_BUCKET_TASK_TEST_BUCKET_NAME + StringConstants.UNDER_SCORE +
                                +Calendar.getInstance().getTimeInMillis(), null);
                workers.execute(createBucketTask);
                createBucketTask.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();
                });
                createBucketTask.setOnFailed(event -> {
                    latch.countDown();
                });
                createBucketTask.setOnCancelled(event -> {
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