package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;


public class GetJobPriorityTaskTest {
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
    public void getJobPriority() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                //Loading all interrupted jobs
                final JobInterruptionStore jobInterruptionStore = JobInterruptionStore.loadJobIds();

                //Getting jobId of interrupted job
                final Map<String, Map<String, FilesAndFolderMap>> endPointsMap = jobInterruptionStore.getJobIdsModel()
                        .getEndpoints().stream().filter(endpoint -> endpoint.containsKey(session.getEndpoint() + StringConstants
                                .COLON + session.getPortNo())).findFirst().orElse(null);
                if (!Guard.isMapNullOrEmpty(endPointsMap)) {

                    final Map<String, FilesAndFolderMap> stringFilesAndFolderMapMap = endPointsMap.get(session.getEndpoint() + StringConstants.COLON + session.getPortNo());
                    final String jobId = stringFilesAndFolderMapMap.entrySet().stream()
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(null);
                    if (!Guard.isStringNullOrEmpty(jobId)) {
                        //Getting priority of the jobId
                        final GetJobPriorityTask getJobPriorityTask = new GetJobPriorityTask(session, UUID.fromString(jobId));
                        workers.execute(getJobPriorityTask);

                        //Validating test case
                        getJobPriorityTask.setOnSucceeded(event -> {
                            successFlag = true;
                            latch.countDown();
                        });
                        getJobPriorityTask.setOnFailed(event -> latch.countDown());
                        getJobPriorityTask.setOnCancelled(event -> latch.countDown());
                    } else {
                        successFlag = true;
                        latch.countDown();
                        latch.countDown();
                    }
                } else {
                    successFlag = true;
                    latch.countDown();
                    latch.countDown();
                }
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}