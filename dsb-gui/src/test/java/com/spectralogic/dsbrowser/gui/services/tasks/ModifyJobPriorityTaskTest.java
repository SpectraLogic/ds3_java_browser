package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.modifyjobpriority.ModifyJobPriorityModel;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertTrue;

public class ModifyJobPriorityTaskTest {
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
    public void modifyPriority() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        Platform.runLater(() -> {
            try {
                //Loading all interrupted jobs
                final JobInterruptionStore jobInterruptionStore = JobInterruptionStore.loadJobIds();

                //Getting jobId of interrupted job
                final Optional<Map<String, Map<String, FilesAndFolderMap>>> endPointsMap = jobInterruptionStore.getJobIdsModel()
                        .getEndpoints().stream().filter(endpoint -> endpoint.containsKey(session.getEndpoint()
                                + StringConstants.COLON + session.getPortNo())).findFirst();
                if (endPointsMap.isPresent()&& !Guard.isMapNullOrEmpty(endPointsMap.get())) {

                    final Map<String, FilesAndFolderMap> stringFilesAndFolderMapMap =
                            endPointsMap.get().get(session.getEndpoint() + StringConstants.COLON + session.getPortNo());
                    final Optional<String> jobId = stringFilesAndFolderMapMap.entrySet().stream()
                            .map(Map.Entry::getKey)
                            .findFirst();

                    if (jobId.isPresent() && !Guard.isStringNullOrEmpty(jobId.get())) {
                        //Getting priority of the jobId
                        final GetJobPriorityTask getJobPriorityTask = new GetJobPriorityTask(session, UUID.fromString(jobId.get()));
                        workers.execute(getJobPriorityTask);
                        latch.countDown();
                        final ModifyJobPriorityModel value = (ModifyJobPriorityModel) getJobPriorityTask.get();

                        //Changing priority of job
                        final ModifyJobPriorityTask modifyJobPriorityTask = new ModifyJobPriorityTask(value, Priority.LOW);
                        workers.execute(modifyJobPriorityTask);

                        //Validating test case
                        modifyJobPriorityTask.setOnSucceeded(event -> {
                            successFlag = true;
                            latch.countDown();
                        });
                        modifyJobPriorityTask.setOnFailed(event -> latch.countDown());
                        modifyJobPriorityTask.setOnCancelled(event -> latch.countDown());
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
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}