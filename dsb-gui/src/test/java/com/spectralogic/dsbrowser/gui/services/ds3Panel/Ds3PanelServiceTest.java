package com.spectralogic.dsbrowser.gui.services.ds3Panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.spectrads3.GetBucketsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetBucketsSpectraS3Response;
import com.spectralogic.ds3client.models.Bucket;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateBucketTask;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TreeTableView;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;


public class Ds3PanelServiceTest {

    private final Workers workers = new Workers();
    private static Session session;
    private boolean successFlag = false;

    @BeforeClass
    public static void setUp() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO,
                    null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);
            session = new CreateConnectionTask().createConnection(SessionModelService.setSessionModel(savedSession, false));
        });
    }

    @Test
    public void checkIfBucketEmpty() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                //Creating empty bucket
                final CreateBucketModel createBucketModel = new CreateBucketModel("fake", UUID.fromString("b8ae2e65-b665-4733-bd48-f7ab760c43f3"));
                final CreateBucketTask createBucketTask = new CreateBucketTask(createBucketModel, session.getClient(),
                        SessionConstants.DS3_PANEL_SERVICE_TEST_BUCKET_NAME,
                        null);
                workers.execute(createBucketTask);
                //Checking is bucket empty
                successFlag = Ds3PanelService.checkIfBucketEmpty(SessionConstants.DS3_PANEL_SERVICE_TEST_BUCKET_NAME, session);
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
    public void setSearchableBucket() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Optional<ImmutableList<Bucket>> searchableBuckets = Ds3PanelService.setSearchableBucket(null, session, Mockito.mock(TreeTableView.class));

                final GetBucketsSpectraS3Request getBucketsSpectraS3Request = new GetBucketsSpectraS3Request();
                final GetBucketsSpectraS3Response response = session.getClient().getBucketsSpectraS3(getBucketsSpectraS3Request);
                final List<Bucket> buckets = response.getBucketListResult().getBuckets();
                if(searchableBuckets.isPresent()) {
                    successFlag = (!Guard.isNullOrEmpty(searchableBuckets.get()) && searchableBuckets.get().size() == buckets.size());
                }
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