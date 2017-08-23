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
import com.google.common.collect.ImmutableMap;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.utils.ResourceUtils;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
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
import com.spectralogic.dsbrowser.gui.util.CancelJobsWorker;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.application.Platform;
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
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CancelJobsWorkerTest {
    private static final JobWorkers jobWorkers = new JobWorkers(10);
    private static final Workers workers = new Workers();
    private static Session session;
    private static File file;
    private static final UUID jobId = UUID.randomUUID();
    private static JobInterruptionStore jobInterruptionStore;
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private final static String testFolder = "files/";
    private final static String testFile = "SampleFiles.txt";
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();
    private static final String TEST_ENV_NAME = "CancelJobsWorkerTest";
    private final static String bucketName = "CancelJobsWorkerTestBucket";

    @BeforeClass
    public static void setUp() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        new JFXPanel();
        Platform.runLater(() -> {
            try {
                //Initiating session
                final SavedSession savedSession = new SavedSession(
                        TEST_ENV_NAME,
                        client.getConnectionDetails().getEndpoint(),
                        "80",
                        null,
                        new SavedCredentials(
                                client.getConnectionDetails().getCredentials().getClientId(),
                                client.getConnectionDetails().getCredentials().getKey()),
                        false,
                        false);
                session = CreateConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle, buildInfoService);

                //Loading resource file
                final Path path;
                try {
                    path = ResourceUtils.loadFileResource(testFolder + testFile);
                    CancelJobsWorkerTest.file = path.toFile();
                } catch (final URISyntaxException | FileNotFoundException e) {
                    e.printStackTrace();
                    fail();
                }

                final ImmutableMap<String, Path> filesMap = new ImmutableMap.Builder<String, Path>().put(testFile, file.toPath()).build();

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
                JobInterruptionStore.saveJobInterruptionStore(jobInterruptionStore1);
                jobInterruptionStore = JobInterruptionStore.loadJobIds();
            } catch (final IOException io) {
                io.printStackTrace();
                latch.countDown();
                fail();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void cancelTasks() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final ImmutableList<File> filesList = ImmutableList.of(file);
                final Ds3Client ds3Client = session.getClient();
                final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);
                final Ds3Common ds3Common = new Ds3Common();
                ds3Common.setDeepStorageBrowserPresenter(deepStorageBrowserPresenter);

                //Initiating a put job which to be cancelled
                final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
                final Ds3PutJob ds3PutJob = new Ds3PutJob(ds3Client, filesList, bucketName, StringConstants.EMPTY_STRING,
                         Priority.URGENT.toString(), 5,
                        JobInterruptionStore.loadJobIds(), deepStorageBrowserPresenter, session, settingsStore, Mockito.mock(LoggingService.class), resourceBundle);

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
                final CancelAllRunningJobsTask cancelAllRunningJobsTask = CancelJobsWorker.cancelTasks(jobWorkers, JobInterruptionStore.loadJobIds(), workers, Mockito.mock(LoggingService.class));
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
            } catch (final InterruptedException | IOException e) {
                e.printStackTrace();
                latch.countDown();
                fail();
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
                final Ds3Common ds3Common = new Ds3Common();
                ds3Common.setDeepStorageBrowserPresenter(deepStorageBrowserPresenter);

                //Initiating put job which to be cancelled
                final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
                final Ds3PutJob ds3PutJob = new Ds3PutJob(ds3Client, filesList, bucketName, "",
                        Priority.URGENT.toString(), 5,
                        JobInterruptionStore.loadJobIds(), deepStorageBrowserPresenter, session, settingsStore, Mockito.mock(LoggingService.class), resourceBundle);

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
                        jobInterruptionStore, workers, session, Mockito.mock(LoggingService.class));
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
            } catch (final InterruptedException | IOException e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}
