package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.HBox;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GetBucketTaskTest {
    private final Workers workers = new Workers();
    private static Session session;
    private boolean successFlag = false;

    @BeforeClass
    public static void setUp() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO,
                    null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);
            session = new NewSessionPresenter().createConnection(savedSession);
        });
    }

    @Test
    public void getBucket() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try{
                final Ds3TreeTableValue ds3TreeTableValue = new Ds3TreeTableValue(SessionConstants.ALREADY_EXIST_BUCKET, SessionConstants.ALREADY_EXIST_BUCKET,
                        Ds3TreeTableValue.Type.Bucket, 0L, "", StringConstants.TWO_DASH,
                        false, Mockito.mock(HBox.class));
                final GetBucketTask getBucketTask = new GetBucketTask(FXCollections.observableArrayList(), SessionConstants.ALREADY_EXIST_BUCKET, session, ds3TreeTableValue,
                        false, workers, Mockito.mock(Ds3TreeTableItem.class),
                        Mockito.mock(TreeTableView.class), Mockito.mock(Ds3Common.class));
                workers.execute(getBucketTask);
                getBucketTask.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();
                });
                getBucketTask.setOnFailed(event -> {
                    latch.countDown();
                });
                getBucketTask.setOnCancelled(event -> {
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