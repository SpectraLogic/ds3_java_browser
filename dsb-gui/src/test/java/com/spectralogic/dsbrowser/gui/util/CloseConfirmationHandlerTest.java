/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobIdsModel;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.JobSettings;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.*;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3PutJob;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.JFXPanel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;


public class CloseConfirmationHandlerTest {
    private static final JobWorkers jobWorkers = new JobWorkers(10);
    private static final Workers workers = new Workers();
    private static final CreateConnectionTask createConnectionTask = new CreateConnectionTask();
    private static Session session;
    private static CloseConfirmationHandler handler;
    private static File file;
    private boolean successFlag = false;

    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO, null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);
            session = createConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false));
            handler = new CloseConfirmationHandler(null, null, null, null, null, jobWorkers, workers);
            final ClassLoader classLoader = CloseConfirmationHandlerTest.class.getClassLoader();
            final URL url = classLoader.getResource(SessionConstants.LOCAL_FOLDER + SessionConstants.LOCAL_FILE);
            if (url != null) {
                CloseConfirmationHandlerTest.file = new File(url.getFile());
            }
        });
    }

    @Test
    public void setPreferences() throws Exception {
        Platform.runLater(() -> {
            handler.setPreferences(100, 100, 200, 200, false);
            Assert.assertEquals(Double.valueOf(ApplicationPreferences.getInstance().getX()), Double.valueOf(100));
            Assert.assertEquals(Double.valueOf(ApplicationPreferences.getInstance().getY()), Double.valueOf(100));
            Assert.assertEquals(Double.valueOf(ApplicationPreferences.getInstance().getWidth()), Double.valueOf(200));
            Assert.assertEquals(Double.valueOf(ApplicationPreferences.getInstance().getHeight()), Double.valueOf(200));
        });
    }

    @Test
    public void saveSessionStore() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
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
                savedSessionStorePrevious.saveSession(createConnectionTask.createConnection(newSessionModel));
                handler.saveSessionStore(savedSessionStorePrevious);

                //To get list of saved session
                final SavedSessionStore savedSessionStoreNew = SavedSessionStore.loadSavedSessionStore();
                final ObservableList<SavedSession> sessions = savedSessionStoreNew.getSessions();
                final Optional<SavedSession> savedSession = sessions.stream().filter(session -> session.getName().equals(newSessionModel.getSessionName())).findFirst();

                if (savedSession.isPresent() &&newSessionModel.getSessionName().equals(savedSession.get().getName()) &&
                        newSessionModel.getAccessKey().equals(savedSession.get().getCredentials().getAccessId()) &&
                        newSessionModel.getSecretKey().equals(savedSession.get().getCredentials().getSecretKey()) &&
                        newSessionModel.getPortNo().equals(savedSession.get().getPortNo())) {
                    successFlag = true;
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

    @Test
    public void saveJobProperties() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final SavedJobPrioritiesStore savedJobPrioritiesStore = new SavedJobPrioritiesStore(new JobSettings("URGENT", "URGENT", true));
                handler.saveJobPriorities(savedJobPrioritiesStore);
                //To get saved job priorities
                final SavedJobPrioritiesStore savedJobPrioritiesStoreNew = SavedJobPrioritiesStore.loadSavedJobPriorties();
                if (savedJobPrioritiesStoreNew.getJobSettings().getGetJobPriority().equals(Priority.URGENT.toString()) && savedJobPrioritiesStoreNew.getJobSettings().getPutJobPriority().equals(Priority.URGENT.toString())) {
                    successFlag = true;
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

    @Test
    public void saveInterruptionJobs() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
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
                    if (key.equals(session.getEndpoint() + ":" + session.getPortNo())) {
                        successFlag = true;
                    } else {
                        successFlag = false;
                        break;
                    }
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

    @Test
    public void saveSettings() throws Exception {
        try {
            final FilePropertiesSettings filePropertiesSettings = FilePropertiesSettings.createDefault();
            final ProcessSettings processSettings = ProcessSettings.createDefault();
            final ShowCachedJobSettings showCachedJobSettings = ShowCachedJobSettings.createDefault();
            final LogSettings logSettings = LogSettings.DEFAULT;
            new SettingsStore(logSettings, processSettings, filePropertiesSettings, showCachedJobSettings);
            final SettingsStore settingsStoreNew = SettingsStore.loadSettingsStore();
            final ShowCachedJobSettings showCachedJobSettingsNew = settingsStoreNew.getShowCachedJobSettings();
            successFlag = showCachedJobSettingsNew.getShowCachedJob();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        assertTrue(successFlag);
    }

    @Test
    public void cancelAllRunningTasks() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            final List<File> filesList = new ArrayList<>();
            filesList.add(file);
            try {
                final Ds3Client ds3Client = session.getClient();
                final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);
                final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
                final Ds3Common ds3Common = new Ds3Common();
                ds3Common.setDeepStorageBrowserPresenter(deepStorageBrowserPresenter);
                final Ds3PutJob ds3PutJob = new Ds3PutJob(ds3Client, filesList, SessionConstants.ALREADY_EXIST_BUCKET, "", Priority.URGENT.toString(), 5, JobInterruptionStore.loadJobIds(), ds3Common, settingsStore, Mockito.mock(LoggingService.class));
                jobWorkers.execute(ds3PutJob);
                ds3PutJob.setOnSucceeded(event -> {
                    System.out.println("Put job success");
                });
                Thread.sleep(5000);
                final Task task = handler.cancelAllRunningTasks(jobWorkers, workers, JobInterruptionStore.loadJobIds());
                task.setOnSucceeded(event -> {
                    handler.shutdownWorkers();
                    successFlag = true;
                    latch.countDown();
                });
                task.setOnFailed(event -> {
                    handler.shutdownWorkers();
                    latch.countDown();
                });
                task.setOnCancelled(event -> {
                    handler.shutdownWorkers();
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
