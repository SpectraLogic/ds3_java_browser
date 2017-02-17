package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.DeepStorageBrowserTaskProgressView;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * THERE MUST BE AN INTERRUPTED JOB IN LOCAL FILE SYSTEM TO SUCCESSFULLY RUN THIS TEST CASE
 */
public class CancelSelectedInterruptedJobTest {

    private static final Workers workers = new Workers();
    private static Session session;
    private boolean successFlag = false;


    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            try {
                final SavedSession savedSession = new SavedSession("Test1", "192.168.6.164", "8080", null, new SavedCredentials("c3VsYWJoamFpbg==", "yVBAvWTG"), false);
                session = new NewSessionPresenter().createConnection(savedSession);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void call() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Ds3Client ds3Client = session.getClient();
                final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);
                Mockito.when(deepStorageBrowserPresenter.getCircle()).thenReturn(Mockito.mock(Circle.class));
                Mockito.when(deepStorageBrowserPresenter.getLblCount()).thenReturn(Mockito.mock(Label.class));
                Mockito.when(deepStorageBrowserPresenter.getJobButton()).thenReturn(Mockito.mock(Button.class));
                final Ds3Common ds3Common = Mockito.mock(Ds3Common.class);
                Mockito.when(ds3Common.getCurrentSession()).thenReturn(session);
                final JobInterruptionStore jobInterruptionStore = JobInterruptionStore.loadJobIds();
                final Map<String, Map<String, FilesAndFolderMap>> endPointsMap = jobInterruptionStore.getJobIdsModel()
                        .getEndpoints().stream().filter(endpoint -> endpoint.containsKey(session.getEndpoint() + StringConstants
                                .COLON + session.getPortNo())).findFirst().orElse(null);
                final Map<String, FilesAndFolderMap> stringFilesAndFolderMapMap = endPointsMap.get(session.getEndpoint() + StringConstants.COLON + session.getPortNo());
                final String jobIdKey = stringFilesAndFolderMapMap.entrySet().stream()
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);
                final EndpointInfo endPointInfo = new EndpointInfo(session.getEndpoint() + StringConstants.COLON + session.getPortNo(),
                        ds3Client,
                        stringFilesAndFolderMapMap,
                        deepStorageBrowserPresenter,
                        ds3Common
                );
                final DeepStorageBrowserTaskProgressView<Ds3JobTask> taskProgressView = new DeepStorageBrowserTaskProgressView<>();
                Mockito.when(deepStorageBrowserPresenter.getJobProgressView()).thenReturn(taskProgressView);
                final CancelSelectedInterruptedJob cancelSelectedInterruptedJob = new CancelSelectedInterruptedJob(jobIdKey, endPointInfo, jobInterruptionStore);
                workers.execute(cancelSelectedInterruptedJob);
                cancelSelectedInterruptedJob.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();

                });
                cancelSelectedInterruptedJob.setOnFailed(event -> latch.countDown());
                cancelSelectedInterruptedJob.setOnCancelled(event -> latch.countDown());
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        Assert.assertTrue(successFlag);
    }


}