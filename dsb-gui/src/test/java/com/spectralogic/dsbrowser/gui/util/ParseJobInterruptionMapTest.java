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
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.services.tasks.RecoverInterruptedJobTest;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;


public class ParseJobInterruptionMapTest {
    private final static Logger LOG = LoggerFactory.getLogger(ParseJobInterruptionMapTest.class);

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
                    ParseJobInterruptionMapTest.file = new File(url.getFile());
                }
                final Map<String, Path> filesMap = new HashMap<>();
                filesMap.put(SessionConstants.LOCAL_FILE, file.toPath());
                //Storing a interrupted job into resource file
                final FilesAndFolderMap filesAndFolderMap = new FilesAndFolderMap(filesMap, new HashMap<>(), JobRequestType.PUT.toString(), "2/03/2017 17:26:31", false, "additional", 2567L, "demo");
                final Map<String, FilesAndFolderMap> jobIdMap = new HashMap<>();
                jobIdMap.put(jobId.toString(), filesAndFolderMap);
                final Map<String, Map<String, FilesAndFolderMap>> endPointMap = new HashMap<>();
                endPointMap.put(session.getEndpoint() + StringConstants.COLON + session.getPortNo(), jobIdMap);
                final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpointMapList = new ArrayList<>();
                endpointMapList.add(endPointMap);
                final JobIdsModel jobIdsModel = new JobIdsModel(endpointMapList);
                final JobInterruptionStore jobInterruptionStore1 = new JobInterruptionStore(jobIdsModel);
                jobInterruptionStore1.saveJobInterruptionStore(jobInterruptionStore1);
                jobInterruptionStore = JobInterruptionStore.loadJobIds();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void saveValuesToFiles() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Map<String, Path> filesMap = new HashMap<>();
                filesMap.put("demoFile.txt", file.toPath());
                //Creating jobIdMap
                final FilesAndFolderMap filesAndFolderMap = new FilesAndFolderMap(filesMap, new HashMap<>(), JobRequestType.PUT.toString(), "2/03/2017 17:26:31", false, "additional", 2567L, "demo");
                final Map<String, FilesAndFolderMap> jobIdMap = new HashMap<>();
                jobIdMap.put(jobId.toString(), filesAndFolderMap);
                final Map<String, Map<String, FilesAndFolderMap>> endPointMap = new HashMap<>();
                endPointMap.put(session.getEndpoint() + ":" + session.getPortNo(), jobIdMap);
                //Getting endpoints
                final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpointMapList = new ArrayList<>();
                endpointMapList.add(endPointMap);
                final JobIdsModel jobIdsModel = new JobIdsModel(endpointMapList);
                final JobInterruptionStore jobInterruptionStore1 = new JobInterruptionStore(jobIdsModel);
                jobInterruptionStore1.saveJobInterruptionStore(jobInterruptionStore1);
                final JobInterruptionStore jobInterruptionStore = JobInterruptionStore.loadJobIds();
                //Getting updated endpoints
                final Optional<Map<String, Map<String, FilesAndFolderMap>>> endPointsMapElement = jobInterruptionStore.getJobIdsModel()
                        .getEndpoints().stream().filter(endpoint -> endpoint.containsKey(session.getEndpoint() + StringConstants
                                .COLON + session.getPortNo())).findFirst();
                if(endPointsMapElement.isPresent()) {
                    final Map<String, Map<String, FilesAndFolderMap>> endPointsMap = endPointsMapElement.get();
                    final Map<String, FilesAndFolderMap> stringFilesAndFolderMapMap = endPointsMap.get(session.getEndpoint() + StringConstants.COLON + session.getPortNo());
                    final Optional<String> jobIdKeyElement = stringFilesAndFolderMapMap.entrySet().stream()
                            .map(Map.Entry::getKey).filter(uuidKey -> uuidKey.equals(jobId.toString()))
                            .findFirst();
                    if (jobIdKeyElement.isPresent() && jobIdKeyElement.get().equals(jobId.toString())) {
                        successFlag = true;
                    }
                }
                else {
                    LOG.error("No interrupted job present");
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
    public void getJobIDMap() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(
                        jobInterruptionStore.getJobIdsModel().getEndpoints(), endpoint,
                        new DeepStorageBrowserTaskProgressView<>(), null);
                final Map<String, Map<String, FilesAndFolderMap>> filesAndFolderMap1 = jobInterruptionStore
                        .getJobIdsModel()
                        .getEndpoints().get(0);
                //Get root key of interrupted jobs id map
                final Optional<String> resultMapKayElement = filesAndFolderMap1.entrySet().stream()
                        .map(Map.Entry::getKey)
                        .findFirst();
                boolean result = true;
                //Check if all keys/jobs id are equal
                if (!resultMapKayElement.isPresent() || (jobIDMap.size() != filesAndFolderMap1.get(resultMapKayElement.get()).size())) {
                    result = false;
                } else {
                    for (final String key : jobIDMap.keySet()) {
                        if (!jobIDMap.get(key).equals(filesAndFolderMap1.get(resultMapKayElement.get()).get(key))) {
                            result = false;
                        }
                    }
                }
                successFlag = result;
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
    public void removeJobIdFromFile() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Map<String, Path> filesMap = new HashMap<>();
                filesMap.put("demoFile.txt", file.toPath());
                //Creating jobIdMap
                final FilesAndFolderMap filesAndFolderMap = new FilesAndFolderMap(filesMap, new HashMap<>(), JobRequestType.PUT.toString(), "2/03/2017 17:26:31", false, "additional", 2567L, "demo");
                final Map<String, FilesAndFolderMap> jobIdMap = new HashMap<>();
                jobIdMap.put(jobId.toString(), filesAndFolderMap);
                final Map<String, Map<String, FilesAndFolderMap>> endPointMap = new HashMap<>();
                endPointMap.put(session.getEndpoint() + ":" + session.getPortNo(), jobIdMap);
                final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpointMapList = new ArrayList<>();
                endpointMapList.add(endPointMap);

                final JobIdsModel jobIdsModel = new JobIdsModel(endpointMapList);
                //Saving an interrupted job to file to make method independent
                final JobInterruptionStore jobInterruptionStore1 = new JobInterruptionStore(jobIdsModel);
                jobInterruptionStore1.saveJobInterruptionStore(jobInterruptionStore1);
                jobInterruptionStore = JobInterruptionStore.loadJobIds();
                final Optional<Map<String, Map<String, FilesAndFolderMap>>> endPointsMapElement = jobInterruptionStore.getJobIdsModel()
                        .getEndpoints().stream().filter(endpoint1 -> endpoint1.containsKey(session.getEndpoint() +
                                StringConstants.COLON + session.getPortNo())).findFirst();
                //Getting jobId which to be removed
                if(endPointsMapElement.isPresent()) {
                    final Map<String, Map<String, FilesAndFolderMap>> endPointsMap = endPointsMapElement.get();
                    final Optional<String> resultMapKayElement = endPointsMapElement.get().entrySet().stream()
                            .map(Map.Entry::getKey)
                            .findFirst();
                    if (resultMapKayElement.isPresent()) {
                        final Optional<String> jobIdToBeRemovedElement = endPointsMap.get(resultMapKayElement.get()).entrySet().stream()
                                .map(Map.Entry::getKey)
                                .findFirst();
                        if (jobIdToBeRemovedElement.isPresent()) {
                            //Removing job from file
                            ParseJobInterruptionMap.removeJobIdFromFile(jobInterruptionStore, jobIdToBeRemovedElement.get(), session.getEndpoint() + StringConstants.COLON + session.getPortNo());

                            final boolean isJobPresent = readNewJobId();
                            //Validating test case
                            if (!isJobPresent) {
                                successFlag = true;
                            }
                        }
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

    private boolean readNewJobId() throws Exception {
        jobInterruptionStore = JobInterruptionStore.loadJobIds();

        final Optional<Map<String, Map<String, FilesAndFolderMap>>> endPointsMapNewelement = jobInterruptionStore
                .getJobIdsModel()
                .getEndpoints().stream().filter(endpoint1 -> endpoint1.containsKey(session.getEndpoint() +
                        StringConstants
                                .COLON + session.getPortNo())).findFirst();
        if(endPointsMapNewelement.isPresent()) {
            final Optional<String> resultMapKayNewElement = endPointsMapNewelement.get().entrySet().stream()
                    .map(Map.Entry::getKey)
                    .findFirst();

            if (resultMapKayNewElement.isPresent()) {
                final Optional<String> jobIdToBeRemovedNewElement = endPointsMapNewelement.get().get(resultMapKayNewElement.get()).entrySet().stream()
                        .map(Map.Entry::getKey)
                        .findFirst();
                return jobIdToBeRemovedNewElement.isPresent();
            }
        }
        return true;

    }
}
