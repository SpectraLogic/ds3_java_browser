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

package com.spectralogic.dsbrowser.integration;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.utils.ResourceUtils;
import com.spectralogic.dsbrowser.api.services.ShutdownService;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
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
import com.spectralogic.dsbrowser.gui.util.ApplicationPreferences;
import com.spectralogic.dsbrowser.gui.util.CloseConfirmationHandler;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TreeItem;
import javafx.util.Pair;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;


public class CloseConfirmationHandlerTest {
    private static final JobWorkers jobWorkers = new JobWorkers(10);
    private static final Workers workers = new Workers();
    private static final CreateConnectionTask createConnectionTask = new CreateConnectionTask();
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static Session session;
    private static CloseConfirmationHandler handler;
    private static File file;
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();
    private static Path path;
    private static final DateTimeUtils DTU = new DateTimeUtils(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(
                    "CloseConfirmationTest",
                    client.getConnectionDetails().getEndpoint(),
                    "80",
                    null,
                    new SavedCredentials(client.getConnectionDetails().getCredentials().getClientId(), client.getConnectionDetails().getCredentials().getKey()),
                    false,
                    false);
            session = createConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle, buildInfoService);
            handler = new CloseConfirmationHandler(resourceBundle, jobWorkers, Mockito.mock(ShutdownService.class));
            try {
                path = ResourceUtils.loadFileResource("files/");
                if (path != null) {
                    file = path.toFile();
                }
            } catch (final URISyntaxException | FileNotFoundException e) {
                e.printStackTrace();
                fail();
            }
        });
    }

    @Test
    public void setPreferences() {
        Platform.runLater(() -> {
            handler.setPreferences(100, 100, 200, 200, false);
            assertEquals(Double.valueOf(ApplicationPreferences.getInstance().getX()), Double.valueOf(100));
            assertEquals(Double.valueOf(ApplicationPreferences.getInstance().getY()), Double.valueOf(100));
            assertEquals(Double.valueOf(ApplicationPreferences.getInstance().getWidth()), Double.valueOf(200));
            assertEquals(Double.valueOf(ApplicationPreferences.getInstance().getHeight()), Double.valueOf(200));
        });
    }

    @Test
    public void saveSessionStore() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                //Creating new session
                final NewSessionModel newSessionModel = new NewSessionModel();
                newSessionModel.setSessionName(session.getSessionName());
                newSessionModel.setEndpoint(session.getEndpoint());
                newSessionModel.setPortno("80");
                newSessionModel.setAccessKey(client.getConnectionDetails().getCredentials().getClientId());
                newSessionModel.setSecretKey(client.getConnectionDetails().getCredentials().getKey());
                newSessionModel.setProxyServer(null);
                final SavedSessionStore savedSessionStorePrevious = SavedSessionStore.loadSavedSessionStore(resourceBundle, buildInfoService);
                savedSessionStorePrevious.addSession(createConnectionTask.createConnection(newSessionModel, resourceBundle, buildInfoService));
                handler.saveSessionStore(savedSessionStorePrevious);

                //To get list of saved session
                final SavedSessionStore savedSessionStoreNew = SavedSessionStore.loadSavedSessionStore(resourceBundle, buildInfoService);
                final ObservableList<SavedSession> sessions = savedSessionStoreNew.getSessions();
                final Optional<SavedSession> savedSession = sessions.stream().filter(session -> session.getName().equals(newSessionModel.getSessionName())).findFirst();

                if (savedSession.isPresent() &&newSessionModel.getSessionName().equals(savedSession.get().getName()) &&
                        newSessionModel.getAccessKey().equals(savedSession.get().getCredentials().getAccessId()) &&
                        newSessionModel.getSecretKey().equals(savedSession.get().getCredentials().getSecretKey()) &&
                        newSessionModel.getPortNo().equals(savedSession.get().getPortNo())) {
                    successFlag = true;
                }
                latch.countDown();
            } catch (final IOException e) {
                e.printStackTrace();
                latch.countDown();
                fail();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

    @Test
    public void saveJobProperties() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final SavedJobPrioritiesStore savedJobPrioritiesStore = new SavedJobPrioritiesStore(new JobSettings("URGENT", "URGENT", true));
                handler.saveJobPriorities(savedJobPrioritiesStore);
                //To get saved job priorities
                final SavedJobPrioritiesStore savedJobPrioritiesStoreNew = SavedJobPrioritiesStore.loadSavedJobPriorities();
                if (savedJobPrioritiesStoreNew.getJobSettings().getGetJobPriority().equals(Priority.URGENT.toString()) && savedJobPrioritiesStoreNew.getJobSettings().getPutJobPriority().equals(Priority.URGENT.toString())) {
                    successFlag = true;
                }
                latch.countDown();
            } catch (final IOException e) {
                e.printStackTrace();
                latch.countDown();
                fail();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

    @Test
    public void saveInterruptionJobs() throws InterruptedException {
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
            } catch (final IOException e) {
                e.printStackTrace();
                latch.countDown();
                fail();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

    @Test
    public void saveSettings() {
        final FilePropertiesSettings filePropertiesSettings = FilePropertiesSettings.DEFAULT;
        final ProcessSettings processSettings = ProcessSettings.createDefault();
        final ShowCachedJobSettings showCachedJobSettings = ShowCachedJobSettings.createDefault();
        final LogSettings logSettings = LogSettings.DEFAULT;

        new SettingsStore(logSettings, processSettings, filePropertiesSettings, showCachedJobSettings);
        SettingsStore settingsStoreNew = null;
        try {
            settingsStoreNew = SettingsStore.loadSettingsStore();
        } catch (final IOException e) {
            e.printStackTrace();
            fail();
        }

        final ShowCachedJobSettings showCachedJobSettingsNew = settingsStoreNew.getShowCachedJobSettings();
        assertTrue(showCachedJobSettingsNew.getShowCachedJob());
    }

    @Test
    public void cancelAllRunningTasks() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            final TreeItem<Ds3TreeTableValue> destination = new TreeItem<>();
            final ImmutableList<Pair<String,Path>> pair = ImmutableList.of(new Pair<>("files/", path));
            try {
                final Ds3Client ds3Client = session.getClient();
                final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);
                final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
                final Ds3Common ds3Common = new Ds3Common();
                ds3Common.setDeepStorageBrowserPresenter(deepStorageBrowserPresenter);
                final Ds3PutJob ds3PutJob = new Ds3PutJob(ds3Client, pair, "cancelAllTasksBucket", "",
                        JobInterruptionStore.loadJobIds(), Priority.URGENT.toString(), 5, resourceBundle,
                        settingsStore, Mockito.mock(LoggingService.class), deepStorageBrowserPresenter, DTU, destination);
                ds3PutJob.setOnSucceeded(event -> {
                    System.out.println("Put job success");
                });
                jobWorkers.execute(ds3PutJob);
                Thread.sleep(5000);
                final Task task = handler.cancelAllRunningTasks(jobWorkers, workers, JobInterruptionStore.loadJobIds(), Mockito.mock(LoggingService.class));
                task.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();
                });
                task.setOnFailed(event -> {
                    latch.countDown();
                });
                task.setOnCancelled(event -> {
                    latch.countDown();
                });
            } catch (final InterruptedException | IOException e) {
                e.printStackTrace();
                latch.countDown();
                fail();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}
