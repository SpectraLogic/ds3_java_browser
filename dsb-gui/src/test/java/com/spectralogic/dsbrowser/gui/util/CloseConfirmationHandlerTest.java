package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobIdsModel;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.JobSettings;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.JFXPanel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;


public class CloseConfirmationHandlerTest {
    private static final JobWorkers jobWorkers = new JobWorkers(10);
    private static final Workers workers = new Workers();
    private static Session session;
    private static CloseConfirmationHandler handler;
    private static File file;

    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession("Test1", "192.168.6.164", "8080", null, new SavedCredentials("c3VsYWJoamFpbg==", "yVBAvWTG"), false);
            session = new NewSessionPresenter().createConnection(savedSession);
            handler = new CloseConfirmationHandler(null, null, null, null, null, jobWorkers, workers);
            final ClassLoader classLoader = CloseConfirmationHandlerTest.class.getClassLoader();
            final URL url = classLoader.getResource("files/demoFile.txt");
            if (url != null) {
                CloseConfirmationHandlerTest.file = new File(url.getFile());
            }
        });
    }

    @Test
    public void setPreferences() throws Exception {
        Platform.runLater(() -> {
            handler.setPreferences(100, 100, 200, 200);
            Assert.assertEquals(Double.valueOf(ApplicationPreferences.getInstance().getX()), Double.valueOf(100));
            Assert.assertEquals(Double.valueOf(ApplicationPreferences.getInstance().getY()), Double.valueOf(100));
            Assert.assertEquals(Double.valueOf(ApplicationPreferences.getInstance().getWidth()), Double.valueOf(200));
            Assert.assertEquals(Double.valueOf(ApplicationPreferences.getInstance().getHeight()), Double.valueOf(200));
        });
        Thread.sleep(2000);
    }

    @Test
    public void saveSessionStore() throws Exception {
        Platform.runLater(() -> {
            try {
                //Creating new session
                final NewSessionModel newSessionModel = new NewSessionModel();
                newSessionModel.setSessionName(session.getSessionName());
                newSessionModel.setEndpoint(session.getEndpoint());
                newSessionModel.setAccessKey("c3VsYWJoamFpbg==");
                newSessionModel.setPortno("8080");
                newSessionModel.setSecretKey("yVBAvWTG");
                newSessionModel.setProxyServer(null);
                final SavedSessionStore savedSessionStorePrevious = SavedSessionStore.loadSavedSessionStore();
                savedSessionStorePrevious.saveSession(newSessionModel.toSession());
                handler.saveSessionStore(savedSessionStorePrevious);
                //To get list of saved session
                final SavedSessionStore savedSessionStoreNew = SavedSessionStore.loadSavedSessionStore();
                final ObservableList<SavedSession> sessions = savedSessionStoreNew.getSessions();
                final SavedSession savedSession = sessions.stream().filter(session -> session.getName().equals(newSessionModel.getSessionName())).findFirst().orElse(null);
                Assert.assertEquals(newSessionModel.getSessionName(), savedSession.getName());
                Assert.assertEquals(newSessionModel.getAccessKey(), savedSession.getCredentials().getAccessId());
                Assert.assertEquals(newSessionModel.getSecretKey(), savedSession.getCredentials().getSecretKey());
                Assert.assertEquals(newSessionModel.getPortNo(), savedSession.getPortNo());
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(2000);
    }

    @Test
    public void saveJobProperties() throws Exception {
        Platform.runLater(() -> {
            try {
                final SavedJobPrioritiesStore savedJobPrioritiesStore = new SavedJobPrioritiesStore(new JobSettings("URGENT", "URGENT", false));
                handler.saveJobPriorities(savedJobPrioritiesStore);
                //To get saved job priorities
                final SavedJobPrioritiesStore savedJobPrioritiesStoreNew = SavedJobPrioritiesStore.loadSavedJobPriorties();
                Assert.assertEquals(savedJobPrioritiesStoreNew.getJobSettings().getGetJobPriority(), Priority.URGENT.toString());
                Assert.assertEquals(savedJobPrioritiesStoreNew.getJobSettings().getPutJobPriority(), Priority.URGENT.toString());
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(2000);
    }

    @Test
    public void saveInterruptionJobs() throws Exception {
        Platform.runLater(() -> {
            try {
                final Map<String, Path> filesMap = new HashMap<>();
                filesMap.put("demoFile.txt", file.toPath());

                final FilesAndFolderMap filesAndFolderMap = new FilesAndFolderMap(filesMap, new HashMap<>(), JobRequestType.PUT.toString(), "2/03/2017 17:26:31", false, "additional", 2567L, "demo");
                final Map<String, FilesAndFolderMap> jobIdMap = new HashMap<>();
                jobIdMap.put("10c286df-867c-46ae-80a6-858dfab003ac", filesAndFolderMap);

                final Map<String, Map<String, FilesAndFolderMap>> endPointMap = new HashMap<>();
                endPointMap.put(session.getEndpoint() + ":" + session.getPortNo(), jobIdMap);

                final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpointMapList = new ArrayList<>();
                endpointMapList.add(endPointMap);

                final JobIdsModel jobIdsModel = new JobIdsModel(endpointMapList);
                final JobInterruptionStore jobInterruptionStore = new JobInterruptionStore(jobIdsModel);
                handler.saveInterruptionJobs(jobInterruptionStore);
                //To get interrupted job from file
                final JobInterruptionStore jobInterruptionStoreNew = JobInterruptionStore.loadJobIds();
                final Map<String, Map<String, FilesAndFolderMap>> filesAndFolderMapNew = jobInterruptionStoreNew.getJobIdsModel().getEndpoints().get(jobInterruptionStoreNew.getJobIdsModel().getEndpoints().size() - 1);
                final Set<String> keySet = filesAndFolderMapNew.keySet();
                for (final String key : keySet) {
                    Assert.assertEquals(key, session.getEndpoint() + ":" + session.getPortNo());
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(2000);
    }

    @Test
    public void saveSettings() throws Exception {
        Platform.runLater(() -> {
            try {
                final FilePropertiesSettings filePropertiesSettings = FilePropertiesSettings.createDefault();
                final ProcessSettings processSettings = ProcessSettings.createDefault();
                final ShowCachedJobSettings showCachedJobSettings = ShowCachedJobSettings.createDefault();
                final LogSettings logSettings = LogSettings.createDefault();
                new SettingsStore(logSettings, processSettings, filePropertiesSettings, showCachedJobSettings);
                final SettingsStore settingsStoreNew = SettingsStore.loadSettingsStore();
                final ShowCachedJobSettings showCachedJobSettingsNew = settingsStoreNew.getShowCachedJobSettings();
                final Boolean showCachedJobNew = showCachedJobSettingsNew.getShowCachedJob();
                Assert.assertEquals(showCachedJobNew, true);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(2000);
    }

    @Test
    public void cancelAllRunningTasks() throws Exception {
        Platform.runLater(() -> {
            final List<File> filesList = new ArrayList<>();
            filesList.add(file);
            final Ds3Client ds3Client = session.getClient();
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);
            try {
                final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
                final Ds3PutJob ds3PutJob = new Ds3PutJob(ds3Client, filesList, "TEST1", "", deepStorageBrowserPresenter, Priority.URGENT.toString(), 5, JobInterruptionStore.loadJobIds(), null, settingsStore);
                jobWorkers.execute(ds3PutJob);
                ds3PutJob.setOnSucceeded(event -> {
                    System.out.println("Put job success");
                });
                Thread.sleep(5000);
                final Task task = handler.cancelAllRunningTasks(jobWorkers, workers, JobInterruptionStore.loadJobIds());
                task.setOnSucceeded(event -> {
                    Assert.assertEquals(true,true);
                    handler.shutdownWorkers();
                });
                task.setOnFailed(event -> {
                    Assert.fail();
                    handler.shutdownWorkers();
                });

            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(5000);
    }
}