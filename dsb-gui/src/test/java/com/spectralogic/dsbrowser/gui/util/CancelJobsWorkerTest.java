package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobIdsModel;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.CancelAllRunningJobsTask;
import com.spectralogic.dsbrowser.gui.services.tasks.CancelAllTaskBySession;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3PutJob;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class CancelJobsWorkerTest {
    private static final JobWorkers jobWorkers = new JobWorkers(10);
    private static final Workers workers = new Workers();
    private static Session session;
    private static String endpoint;
    private static File file;
    private static final UUID jobId = UUID.randomUUID();
    private static JobInterruptionStore jobInterruptionStore;
    private boolean successFlag = false;

    @BeforeClass
    public static void setUp() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        new JFXPanel();
        Platform.runLater(() -> {
            try {
                //Initiating session
                final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO, null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);
                session = new CreateConnectionTask().createConnection(SessionModelService.setSessionModel(savedSession, false));
                //Initializing endpoint
                endpoint = session.getEndpoint() + StringConstants.COLON + session.getPortNo();
                //Loading resource file
                final ClassLoader classLoader = ParseJobInterruptionMapTest.class.getClassLoader();
                final URL url = classLoader.getResource(SessionConstants.LOCAL_FOLDER + SessionConstants.LOCAL_FILE);
                if (url != null) {
                    CancelJobsWorkerTest.file = new File(url.getFile());
                }
                final Map<String, Path> filesMap = new HashMap<>();
                filesMap.put(SessionConstants.LOCAL_FILE, file.toPath());
                //Storing a interrupted job into resource file
                final FilesAndFolderMap filesAndFolderMap = new FilesAndFolderMap(filesMap, new HashMap<>(), JobRequestType.PUT.toString(), "2/03/2017 17:26:31", false, "additional", 2567L, "demo");
                final Map<String, FilesAndFolderMap> jobIdMap = new HashMap<>();
                jobIdMap.put(jobId.toString(), filesAndFolderMap);
                final Map<String, Map<String, FilesAndFolderMap>> endPointMap = new HashMap<>();
                endPointMap.put(session.getEndpoint() + ":" + session.getPortNo(), jobIdMap);
                final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpointMapList = new ArrayList<>();
                endpointMapList.add(endPointMap);
                final JobIdsModel jobIdsModel = new JobIdsModel(endpointMapList);
                final JobInterruptionStore jobInterruptionStore1 = new JobInterruptionStore(jobIdsModel);
                jobInterruptionStore1.saveJobInterruptionStore(jobInterruptionStore1);
                jobInterruptionStore = JobInterruptionStore.loadJobIds();
            } catch (final Exception io) {
                io.printStackTrace();
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void cancelTasks() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final List<File> filesList = new ArrayList<>();
                filesList.add(file);
                final Ds3Client ds3Client = session.getClient();
                final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);

                //Initiating a put job which to be cancelled
                final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
                final Ds3PutJob ds3PutJob = new Ds3PutJob(ds3Client, filesList, SessionConstants.ALREADY_EXIST_BUCKET, "",
                        deepStorageBrowserPresenter, Priority.URGENT.toString(), 5,
                        JobInterruptionStore.loadJobIds(), null, settingsStore);
                //Starting put job task
                jobWorkers.execute(ds3PutJob);
                ds3PutJob.setOnSucceeded(event -> {
                    System.out.println("Put job success");
                });
                ds3PutJob.setOnFailed(event -> {
                    System.out.println("Put job failed");
                });
                Thread.sleep(5000);
                //Cancelling put job task
                final CancelAllRunningJobsTask cancelAllRunningJobsTask = CancelJobsWorker.cancelTasks(jobWorkers, JobInterruptionStore.loadJobIds(), workers);
                cancelAllRunningJobsTask.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();
                });
                cancelAllRunningJobsTask.setOnFailed(event -> {
                    latch.countDown();
                });
                cancelAllRunningJobsTask.setOnCancelled(event -> {
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

    @Test
    public void cancelAllRunningJobsBySession() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final List<File> filesList = new ArrayList<>();
                filesList.add(file);
                final Ds3Client ds3Client = session.getClient();
                final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);

                //Initiating put job which to be cancelled
                final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
                final Ds3PutJob ds3PutJob = new Ds3PutJob(ds3Client, filesList, SessionConstants.ALREADY_EXIST_BUCKET, "",
                        deepStorageBrowserPresenter, Priority.URGENT.toString(), 5,
                        JobInterruptionStore.loadJobIds(), null, settingsStore);
                //Starting put job task
                jobWorkers.execute(ds3PutJob);
                ds3PutJob.setOnSucceeded(event -> {
                    System.out.println("Put job success");
                });

                ds3PutJob.setOnFailed(event -> {
                    System.out.println("Put job fail");
                });
                Thread.sleep(5000);
                //Cancelling task by session
                final CancelAllTaskBySession cancelAllRunningJobsBySession = CancelJobsWorker.cancelAllRunningJobsBySession(jobWorkers,
                        jobInterruptionStore, null, workers, session);
                cancelAllRunningJobsBySession.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();
                });
                cancelAllRunningJobsBySession.setOnFailed(event -> {
                    latch.countDown();
                });
                cancelAllRunningJobsBySession.setOnCancelled(event -> {
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
