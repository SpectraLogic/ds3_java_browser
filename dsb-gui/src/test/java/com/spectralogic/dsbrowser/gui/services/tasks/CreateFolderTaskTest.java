package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderModel;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.PathUtil;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CreateFolderTaskTest {

    private final Workers workers = new Workers();
    private Session session;

    @Before
    public void setUp() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession("Test1", "192.168.6.164", "8080",
                    null, new SavedCredentials("c3VsYWJoamFpbg==", "yVBAvWTG"), false);
            session = new NewSessionPresenter().createConnection(savedSession);
        });
    }

    @Test
    public void createFolder() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            final String folderName = "TEMP_FOLDER";
            final CreateFolderModel createFolderModel = new CreateFolderModel(session.getClient(), "TEST1",
                    "TEST1");
            final String location = PathUtil.getFolderLocation(createFolderModel.getLocation(), createFolderModel
                    .getBucketName());
            //Instantiating create folder task
            final CreateFolderTask createFolderTask = new CreateFolderTask(session.getClient(), createFolderModel,
                    folderName, PathUtil.getDs3ObjectList(location, folderName),
                    null);
            workers.execute(createFolderTask);
            //Validating test case
            createFolderTask.setOnSucceeded(event -> {
                //Releasing main thread
                latch.countDown();
                assertTrue(true);
            });
            createFolderTask.setOnFailed(event -> {
                //Releasing main thread
                latch.countDown();
                fail();
            });

        });
        latch.await();
    }
}