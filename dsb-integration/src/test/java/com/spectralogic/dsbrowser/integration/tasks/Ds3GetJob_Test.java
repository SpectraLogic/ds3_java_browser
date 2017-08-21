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

package com.spectralogic.dsbrowser.integration.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.models.ChecksumType;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.utils.ResourceUtils;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3GetJob;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.DeepStorageBrowserTaskProgressView;
import com.spectralogic.dsbrowser.integration.IntegrationHelpers;
import com.spectralogic.dsbrowser.integration.TempStorageIds;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Ds3GetJob_Test {

    private static final JobWorkers jobWorkers = new JobWorkers(10);
    private static Session session;
    private static Ds3GetJob ds3GetJob;
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static final Ds3ClientHelpers HELPERS = Ds3ClientHelpers.wrap(client);
    private static final String TEST_ENV_NAME = "Ds3GetJob_Test";
    private static final String DS3GETJOB_TEST_BUCKET_NAME = "Ds3GetJob_Test_Bucket";
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();
    private static TempStorageIds envStorageIds;
    private static UUID envDataPolicyId;

    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(
                    TEST_ENV_NAME,
                    client.getConnectionDetails().getEndpoint(),
                    "80",
                    null,
                    new SavedCredentials(client.getConnectionDetails().getCredentials().getClientId(), client.getConnectionDetails().getCredentials().getKey()),
                    false,
                    false);
            session = CreateConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle, buildInfoService);
            // pre assuming that file is the direct child of bucket
            final Ds3TreeTableValueCustom ds3TreeTableValueCustom = new Ds3TreeTableValueCustom(DS3GETJOB_TEST_BUCKET_NAME,
                    "SampleFiles.txt",
                    Ds3TreeTableValue.Type.File,
                    3718,
                    "2/07/2017 10:28:17",
                    "spectra",
                    false);
            final List<Ds3TreeTableValueCustom> listTreeTable = new ArrayList<>();
            listTreeTable.add(ds3TreeTableValueCustom);
            Path path = null;
            try {
                path = ResourceUtils.loadFileResource("files/");
            } catch (final URISyntaxException | FileNotFoundException e) {
                e.printStackTrace();
                fail();
            }
            final Ds3Client ds3Client = session.getClient();
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);
            final Ds3Common ds3Common = Mockito.mock(Ds3Common.class);
            Mockito.when(ds3Common.getDeepStorageBrowserPresenter()).thenReturn(deepStorageBrowserPresenter);
            Mockito.when(ds3Common.getCurrentSession()).thenReturn(session);
            final DeepStorageBrowserTaskProgressView<Ds3JobTask> taskProgressView = new DeepStorageBrowserTaskProgressView<>();
            Mockito.when(deepStorageBrowserPresenter.getJobProgressView()).thenReturn(taskProgressView);

            try {
                ds3GetJob = new Ds3GetJob(listTreeTable, path, ds3Client, Priority.URGENT.toString(), 5, JobInterruptionStore.loadJobIds(), deepStorageBrowserPresenter, resourceBundle, null);
                taskProgressView.getTasks().add(ds3GetJob);
                envDataPolicyId = IntegrationHelpers.setupDataPolicy(TEST_ENV_NAME, false, ChecksumType.Type.MD5, client);
                envStorageIds = IntegrationHelpers.setup(TEST_ENV_NAME, envDataPolicyId, client);
                HELPERS.ensureBucketExists(DS3GETJOB_TEST_BUCKET_NAME, envDataPolicyId);
            } catch (final IOException e) {
                e.printStackTrace();
                fail();
            }
        });
    }

    @AfterClass
    public static void teardown() throws IOException {
        IntegrationHelpers.teardown(TEST_ENV_NAME, envStorageIds, client);
        client.close();
    }

    @Test
    public void executeJob() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            jobWorkers.execute(ds3GetJob);
            ds3GetJob.setOnSucceeded(event -> {
                if (!ds3GetJob.isJobFailed()) {
                    successFlag = true;
                }
                latch.countDown();

            });
            ds3GetJob.setOnFailed(event -> latch.countDown());
            ds3GetJob.setOnCancelled(event -> latch.countDown());
        });
        latch.await(20, TimeUnit.SECONDS);
        assertTrue(successFlag);
    }

    @Test
    public void createFileMap() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            final Ds3TreeTableValueCustom ds3TreeTableValueCustom = new Ds3TreeTableValueCustom(DS3GETJOB_TEST_BUCKET_NAME,
                    "SampleFiles.txt",
                    Ds3TreeTableValue.Type.File,
                    3718,
                    "2/07/2017 10:28:17",
                    "spectra",
                    false);
            final ImmutableList listItems = ImmutableList.builder().add(ds3TreeTableValueCustom).build();
            final ImmutableMap fileMap = ds3GetJob.getFileMap(listItems);
            if (fileMap.size() == 1) {
                successFlag = true;
            }
            countDownLatch.countDown();
        });
        countDownLatch.await();
        assertTrue(successFlag);
    }

    @Test
    public void createFolderMap() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            final Ds3TreeTableValueCustom ds3TreeTableValueCustom = new Ds3TreeTableValueCustom(DS3GETJOB_TEST_BUCKET_NAME,
                    "testFolder/", Ds3TreeTableValue.Type.Directory,
                    3718, "2/07/2017 10:28:17", "spectra", false);
            final ImmutableList listItems = ImmutableList.builder().add(ds3TreeTableValueCustom).build();
            final ImmutableMap fileMap = ds3GetJob.getFolderMap(listItems);
            if (fileMap.size() == 1) {
                successFlag = true;
            }
            countDownLatch.countDown();
        });
        countDownLatch.await();
        assertTrue(successFlag);
    }

    @Test
    public void getDs3Object() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            final Ds3TreeTableValueCustom ds3TreeTableValueCustom = new Ds3TreeTableValueCustom(DS3GETJOB_TEST_BUCKET_NAME,
                    "testFolder/", Ds3TreeTableValue.Type.Directory,
                    3718, "2/07/2017 10:28:17", "spectra", false);
            final ImmutableList ds3ObjectList = ds3GetJob.getDS3Objects(DS3GETJOB_TEST_BUCKET_NAME, ds3TreeTableValueCustom).toList();
            if (ds3ObjectList.size() == 1) {
                successFlag = true;
            }
            countDownLatch.countDown();
        });
        countDownLatch.await();
        assertTrue(successFlag);
    }


}
