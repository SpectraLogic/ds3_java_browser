package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.layout.HBox;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class BucketUtilTest {
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
    public void createRequest() throws Exception {
        final Ds3TreeTableValue ds3TreeTableValue = new Ds3TreeTableValue(SessionConstants.ALREADY_EXIST_BUCKET, SessionConstants.ALREADY_EXIST_BUCKET,
                Ds3TreeTableValue.Type.Bucket, 0L, "", StringConstants.TWO_DASH,
                false, Mockito.mock(HBox.class));
        final GetBucketRequest request1 = BucketUtil.createRequest(ds3TreeTableValue, SessionConstants.ALREADY_EXIST_BUCKET,
                Mockito.mock(Ds3TreeTableItem.class), 100);
        ds3TreeTableValue.setMarker(SessionConstants.FOLDER_INSIDE_EXISTING_BUCKET);
        final GetBucketRequest request2 = BucketUtil.createRequest(ds3TreeTableValue, SessionConstants.ALREADY_EXIST_BUCKET,
                Mockito.mock(Ds3TreeTableItem.class), 100);
        successFlag = (request1 != null && request2 != null) ? true : false;
        assertTrue(successFlag);
    }

    @Test
    public void getFilterFilesList() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Ds3TreeTableValue ds3TreeTableValue = new Ds3TreeTableValue(SessionConstants.ALREADY_EXIST_BUCKET, SessionConstants.ALREADY_EXIST_BUCKET,
                        Ds3TreeTableValue.Type.Bucket, 0L, "", StringConstants.TWO_DASH,
                        false, Mockito.mock(HBox.class));
                final GetBucketRequest request = BucketUtil.createRequest(ds3TreeTableValue, SessionConstants.ALREADY_EXIST_BUCKET,
                        Mockito.mock(Ds3TreeTableItem.class), 100);
                final GetBucketResponse bucketResponse = session.getClient().getBucket(request);
                final List<Ds3Object> ds3ObjectListFiles = bucketResponse.getListBucketResult()
                        .getObjects()
                        .stream()
                        .filter(c -> ((c.getKey() != null) && (!c.getKey().equals(ds3TreeTableValue.getFullName()))))
                        .map(i -> new Ds3Object(i.getKey(), i.getSize()))
                        .collect(Collectors.toList());
                final List<Ds3TreeTableValue> filterFilesList = BucketUtil.getFilterFilesList(ds3ObjectListFiles,
                        bucketResponse, SessionConstants.ALREADY_EXIST_BUCKET, session);
                successFlag = (null != filterFilesList) ? true : false;
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
    public void getDirectoryValues() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        Platform.runLater(() -> {
            try {
                final Ds3TreeTableValue ds3TreeTableValue = new Ds3TreeTableValue(SessionConstants.ALREADY_EXIST_BUCKET, SessionConstants.ALREADY_EXIST_BUCKET,
                        Ds3TreeTableValue.Type.Bucket, 0L, "", StringConstants.TWO_DASH,
                        false, Mockito.mock(HBox.class));
                final GetBucketRequest request = BucketUtil.createRequest(ds3TreeTableValue, SessionConstants.ALREADY_EXIST_BUCKET,
                        Mockito.mock(Ds3TreeTableItem.class), 100);
                final GetBucketResponse bucketResponse = session.getClient().getBucket(request);
                latch.countDown();
                final List<Ds3TreeTableValue> directoryValues = BucketUtil.getDirectoryValues(bucketResponse, SessionConstants.ALREADY_EXIST_BUCKET);
                successFlag = (null != directoryValues) ? true : false;
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

}