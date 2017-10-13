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
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.commands.spectrads3.DeleteBucketSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.models.ChecksumType;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.utils.ResourceUtils;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.integration.IntegrationHelpers;
import com.spectralogic.dsbrowser.integration.TempStorageIds;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.shape.Circle;
import javafx.util.Pair;
import org.controlsfx.control.TaskProgressView;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Ds3PutJobTest {
    private static final DateTimeUtils DTU = new DateTimeUtils(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    private static final JobWorkers jobWorkers = new JobWorkers(10);
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();
    private static Ds3Client client;
    private static Ds3ClientHelpers HELPERS;
    private static final String TEST_ENV_NAME = "Ds3PutJobTest";
    private static final String BUCKET_NAME = "Ds3PutJobTest_Bucket";
    private static TempStorageIds envStorageIds;
    private static UUID envDataPolicyId;
    private static Session session;
    private static Ds3PutJob ds3PutJob;
    private boolean successFlag = false;

    @BeforeClass
    public static void setConnection() throws FileNotFoundException, URISyntaxException {
        new JFXPanel();
        final Path path = ResourceUtils.loadFileResource("files/SampleFiles.txt");
        Platform.runLater(() -> {
            client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
            HELPERS = Ds3ClientHelpers.wrap(client);

            final SavedSession savedSession = new SavedSession(
                    "Ds3PutJobTest",
                    client.getConnectionDetails().getEndpoint(),
                    "80",
                    null,
                    new SavedCredentials(
                            client.getConnectionDetails().getCredentials().getClientId(),
                            client.getConnectionDetails().getCredentials().getKey()),
                    false,
                    false);
            session = CreateConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle, buildInfoService);
            final ImmutableList<Pair<String, Path>> pair;
            pair = ImmutableList.of(new Pair<>("files/SampleFiles.txt", path));
            final Ds3Client ds3Client = session.getClient();
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);
            Mockito.when(deepStorageBrowserPresenter.getNumInterruptedJobsCircle()).thenReturn(Mockito.mock(Circle.class));
            Mockito.when(deepStorageBrowserPresenter.getNumInterruptedJobsLabel()).thenReturn(Mockito.mock(Label.class));
            Mockito.when(deepStorageBrowserPresenter.getRecoverInterruptedJobsButton()).thenReturn(Mockito.mock(Button.class));
            final Ds3Common ds3Common = Mockito.mock(Ds3Common.class);
            Mockito.when(ds3Common.getCurrentSession()).thenReturn(session);
            Mockito.when(ds3Common.getDeepStorageBrowserPresenter()).thenReturn(deepStorageBrowserPresenter);
            final TaskProgressView<Ds3JobTask> taskProgressView = new TaskProgressView<>();
            Mockito.when(deepStorageBrowserPresenter.getJobProgressView()).thenReturn(taskProgressView);
            final TreeItem<Ds3TreeTableValue> destination = new TreeItem<>();

            try {
                final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
                settingsStore.getShowCachedJobSettings().setShowCachedJob(false);
                ds3PutJob = new Ds3PutJob(ds3Client, pair, BUCKET_NAME, "", JobInterruptionStore.loadJobIds(), Priority.URGENT.toString(),
                        5, resourceBundle, settingsStore, Mockito.mock(LoggingService.class), deepStorageBrowserPresenter, DTU, destination);
                envDataPolicyId = IntegrationHelpers.setupDataPolicy(TEST_ENV_NAME, false, ChecksumType.Type.MD5, client);
                envStorageIds = IntegrationHelpers.setup(TEST_ENV_NAME, envDataPolicyId, client);
                HELPERS.ensureBucketExists(BUCKET_NAME, envDataPolicyId);
            } catch (final IOException io) {
                io.printStackTrace();
                fail();
            }

            taskProgressView.getTasks().add(ds3PutJob);
        });
    }

    @AfterClass
    public static void teardown() throws IOException {
        client.deleteBucketSpectraS3(new DeleteBucketSpectraS3Request(BUCKET_NAME).withForce(true));
        IntegrationHelpers.teardown(TEST_ENV_NAME, envStorageIds, client);
        client.close();
    }

    @Test
    public void executeJob() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            jobWorkers.execute(ds3PutJob);
            ds3PutJob.setOnSucceeded(event -> {
                if (!Ds3JobTask.isJobFailed()) {
                    successFlag = true;
                }
                latch.countDown();
            });
            ds3PutJob.setOnFailed(event -> latch.countDown());
            ds3PutJob.setOnCancelled(event -> latch.countDown());
        });
        latch.await();
        assertTrue(successFlag);
    }

}
