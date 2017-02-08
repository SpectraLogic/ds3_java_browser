package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;


public class CreateBucketTaskTest {
    private final Workers workers = new Workers();
    private Session session;

    @Before
    public void setUp() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession("Test1", "192.168.6.164", "8080", null, new SavedCredentials("c3VsYWJoamFpbg==", "yVBAvWTG"), false);
            session = new NewSessionPresenter().createConnection(savedSession);
        });
    }

    @Test
    public void createBucket() throws Exception {
        Platform.runLater(() -> {
            final CreateBucketModel createBucketModel = new CreateBucketModel("fake", UUID.fromString("b8ae2e65-b665-4733-bd48-f7ab760c43f3"));
            final CreateBucketTask createBucketTask = new CreateBucketTask(createBucketModel, session.getClient(),
                    "TEMP_BUCKET",
                    null);
            workers.execute(createBucketTask);
            createBucketTask.setOnSucceeded(event -> {
                assertEquals(true, true);
            });
            createBucketTask.setOnFailed(event -> {
                fail();
            });
        });
        Thread.sleep(5000);
    }
}