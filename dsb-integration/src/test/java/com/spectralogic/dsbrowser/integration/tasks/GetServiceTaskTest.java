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
import com.spectralogic.ds3client.commands.spectrads3.DeleteBucketSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.models.ChecksumType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.services.tasks.GetServiceTask;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.integration.IntegrationHelpers;
import com.spectralogic.dsbrowser.integration.TempStorageIds;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TreeItem;
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

public class GetServiceTaskTest {

    private final Workers workers = new Workers();
    private Session session;
    private boolean successFlag = false;
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static final Ds3ClientHelpers HELPERS = Ds3ClientHelpers.wrap(client);
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();
    private static final String TEST_ENV_NAME = "GetServiceTaskTest";
    private static final String bucketName = "GetServiceTaskTest_bucket";
    private static TempStorageIds envStorageIds;
    private static UUID envDataPolicyId;

    @Before
    public void setUp() throws Exception {
        final ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
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
            } catch (final IOException e) {
                e.printStackTrace();
                fail();
            }
        });
    }

    @After
    public void teardown() throws IOException {
        client.deleteBucketSpectraS3(new DeleteBucketSpectraS3Request(bucketName).withForce(true));
        IntegrationHelpers.teardown(TEST_ENV_NAME, envStorageIds, client);
        client.close();
    }

    @Test
    public void getServiceTask() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                HELPERS.ensureBucketExists(bucketName);

            }catch (final Exception e){
                e.printStackTrace();
                latch.countDown();
            }

            final ObservableList<TreeItem<Ds3TreeTableValue>> observableList = FXCollections.observableArrayList();
            final GetServiceTask getServiceTask = new GetServiceTask(observableList, session, workers,
                    Mockito.mock(Ds3Common.class), Mockito.mock(LoggingService.class));
            workers.execute(getServiceTask);
            getServiceTask.setOnSucceeded(event -> {
                successFlag=true;
                latch.countDown();
            });
            getServiceTask.setOnFailed(event -> {
                latch.countDown();
                fail();
            });
            getServiceTask.setOnCancelled(event -> {
                latch.countDown();
                fail();
            });
        });
        latch.await();
        assertTrue(successFlag);
    }

}
