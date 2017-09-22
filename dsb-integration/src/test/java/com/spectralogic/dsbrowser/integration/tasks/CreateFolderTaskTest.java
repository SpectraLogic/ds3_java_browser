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

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.commands.spectrads3.DeleteFolderRecursivelySpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.models.ChecksumType;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderModel;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateFolderTask;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.integration.IntegrationHelpers;
import com.spectralogic.dsbrowser.integration.TempStorageIds;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CreateFolderTaskTest {

    private final Workers workers = new Workers();
    private static Session session;
    private boolean successFlag = false;
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static final Ds3ClientHelpers HELPERS = Ds3ClientHelpers.wrap(client);
    private static final String TEST_ENV_NAME = "CreateFolderTaskTest";
    private static final String CREATE_FOLDER_TASK_TEST_BUCKET_NAME = "CreateFolderTaskTest_Bucket";
    private static TempStorageIds envStorageIds;
    private static UUID envDataPolicyId;
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();

    @BeforeClass
    public static void setUp() {
        new JFXPanel();
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
            session = new CreateConnectionTask().createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle, buildInfoService);
            try {
                envDataPolicyId = IntegrationHelpers.setupDataPolicy(TEST_ENV_NAME, false, ChecksumType.Type.MD5, client);
                envStorageIds = IntegrationHelpers.setup(TEST_ENV_NAME, envDataPolicyId, client);
                HELPERS.ensureBucketExists(CREATE_FOLDER_TASK_TEST_BUCKET_NAME, envDataPolicyId);
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
    public void createFolder() throws InterruptedException, IOException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String folderName = "testFolder_" + LocalDateTime.now().getSecond();
        Platform.runLater(() -> {
            final CreateFolderModel createFolderModel = new CreateFolderModel(session.getClient(), CREATE_FOLDER_TASK_TEST_BUCKET_NAME, CREATE_FOLDER_TASK_TEST_BUCKET_NAME);

            //Instantiating create folder task
            final CreateFolderTask createFolderTask = new CreateFolderTask(session.getClient(),
                    createFolderModel.getBucketName(),
                    folderName,
                    null,
                    null);
            workers.execute(createFolderTask);

            //Validating test case
            createFolderTask.setOnSucceeded(event -> {
                successFlag = true;
                //Releasing main thread
                latch.countDown();
            });
            createFolderTask.setOnFailed(event -> {
                latch.countDown();
            });
            createFolderTask.setOnCancelled(event -> {
                latch.countDown();
            });
        });
        latch.await();

        client.deleteFolderRecursivelySpectraS3(new DeleteFolderRecursivelySpectraS3Request(CREATE_FOLDER_TASK_TEST_BUCKET_NAME, folderName));
        assertTrue(successFlag);
    }
}
