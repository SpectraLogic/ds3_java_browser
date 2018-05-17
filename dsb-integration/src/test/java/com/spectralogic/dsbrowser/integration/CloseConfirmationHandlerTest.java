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

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.utils.ResourceUtils;
import com.spectralogic.dsbrowser.api.services.ShutdownService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
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
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;


public class CloseConfirmationHandlerTest {
    private static final JobWorkers jobWorkers = new JobWorkers();
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static Session session;
    private static CloseConfirmationHandler handler;
    private static File file;
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();
    private static Path path;
    private final static AlertService ALERT_SERVICE = new AlertService(resourceBundle, new Ds3Common());
    private final static CreateConnectionTask createConnectionTask = new CreateConnectionTask(ALERT_SERVICE, resourceBundle, buildInfoService);

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
            session = createConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false));
            handler = new CloseConfirmationHandler(resourceBundle, jobWorkers, Mockito.mock(ShutdownService.class), new Ds3Alert(new Ds3Common()));
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
                final SavedSessionStore savedSessionStorePrevious = SavedSessionStore.loadSavedSessionStore();
                savedSessionStorePrevious.addSession(createConnectionTask.createConnection(newSessionModel));
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

}
