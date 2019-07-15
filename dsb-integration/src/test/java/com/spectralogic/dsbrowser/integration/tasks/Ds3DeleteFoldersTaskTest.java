/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
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

import com.google.common.collect.ImmutableMultimap;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.models.ChecksumType;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteFoldersTask;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.AlertService;
import com.spectralogic.dsbrowser.integration.IntegrationHelpers;
import com.spectralogic.dsbrowser.integration.TempStorageIds;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Window;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class Ds3DeleteFoldersTaskTest {

    private final Workers workers = new Workers();
    private Session session;
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static final Ds3ClientHelpers HELPERS = Ds3ClientHelpers.wrap(client);
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();
    private static final String TEST_ENV_NAME = "Ds3DeleteFoldersTaskTest";
    private static final String bucketName = "DeleteFolderTaskTestBucket";
    private static final String folderName = "testFolder/";
    private static TempStorageIds envStorageIds;
    private static UUID envDataPolicyId;
    private final static AlertService ALERT_SERVICE = new AlertService(resourceBundle);
    private final static CreateConnectionTask createConnectionTask = new CreateConnectionTask(ALERT_SERVICE, resourceBundle, buildInfoService);
    private final static Window window = Mockito.mock(Window.class);

    @Before
    public void setUp() throws InterruptedException {
        new JFXPanel();
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
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
            session = createConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false), window);
            try {
                envDataPolicyId = IntegrationHelpers.setupDataPolicy(TEST_ENV_NAME, false, ChecksumType.Type.MD5, client);
                envStorageIds = IntegrationHelpers.setup(TEST_ENV_NAME, envDataPolicyId, client);
                HELPERS.ensureBucketExists(bucketName, envDataPolicyId);
                latch.countDown();
            } catch (final IOException e) {
                e.printStackTrace();
                fail();
            }
        });
        latch.await();
    }

    @After
    public void teardown() throws IOException {
        IntegrationHelpers.teardown(TEST_ENV_NAME, envStorageIds, client);
        client.close();
    }

    @Test
    public void deleteFolder() throws IOException, InterruptedException {
        HELPERS.createFolder(bucketName, folderName);

        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
                final Ds3DeleteFoldersTask deleteFolderTask = new Ds3DeleteFoldersTask(session.getClient(), ImmutableMultimap.of(bucketName, folderName));
                workers.execute(deleteFolderTask);
                deleteFolderTask.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();
                });
                deleteFolderTask.setOnFailed(event -> latch.countDown());
                deleteFolderTask.setOnCancelled(event -> latch.countDown());
        });
        latch.await();
        assertTrue(successFlag);
    }
}