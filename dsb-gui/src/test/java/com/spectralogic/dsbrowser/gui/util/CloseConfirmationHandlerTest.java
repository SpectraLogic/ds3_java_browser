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

import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobIdsModel;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));

    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO, null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false, false);
            session = createConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle);
            handler = new CloseConfirmationHandler(resourceBundle, jobWorkers,(() -> {}));
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

}
