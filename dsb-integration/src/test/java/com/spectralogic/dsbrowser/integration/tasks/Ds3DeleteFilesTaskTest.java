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

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.commands.spectrads3.DeleteBucketSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.models.ChecksumType;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.utils.ResourceUtils;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteFilesTask;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.LazyAlert;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import com.spectralogic.dsbrowser.integration.IntegrationHelpers;
import com.spectralogic.dsbrowser.integration.TempStorageIds;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Ds3DeleteFilesTaskTest {

    private final Workers workers = new Workers();
    private Session session;
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static final Ds3ClientHelpers HELPERS = Ds3ClientHelpers.wrap(client);
    private static final String TEST_ENV_NAME = "DeleteFilesTaskTest";
    private static final String DELETE_FILES_TASK_TEST_BUCKET_NAME = "DeleteFilesTaskTest_Bucket";
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();
    private static TempStorageIds envStorageIds;
    private static UUID envDataPolicyId;
    private final static LazyAlert lazyAlert = new LazyAlert(resourceBundle);
    private final static CreateConnectionTask createConnectionTask = new CreateConnectionTask(lazyAlert, resourceBundle, buildInfoService);

    @Before
    public void setUp() {
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
            session = createConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false));
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
        client.deleteBucketSpectraS3(new DeleteBucketSpectraS3Request(DELETE_FILES_TASK_TEST_BUCKET_NAME).withForce(true));
        IntegrationHelpers.teardown(TEST_ENV_NAME, envStorageIds, client);
        client.close();
    }

    @Test
    public void deleteFiles() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                HELPERS.ensureBucketExists(DELETE_FILES_TASK_TEST_BUCKET_NAME, envDataPolicyId);
                Path path = null;
                try {
                    path = ResourceUtils.loadFileResource("files/");
                } catch (final URISyntaxException | FileNotFoundException e) {
                    e.printStackTrace();
                    latch.countDown();
                    fail();
                }

                Iterable<Ds3Object> objectsList = HELPERS.listObjectsForDirectory(path);
                HELPERS.startWriteJob(DELETE_FILES_TASK_TEST_BUCKET_NAME, objectsList);

                final ImmutableList<String> buckets = ImmutableList.of(DELETE_FILES_TASK_TEST_BUCKET_NAME);

                final Ds3TreeTableValue ds3TreeTableValue = new Ds3TreeTableValue(
                        DELETE_FILES_TASK_TEST_BUCKET_NAME, "files/SampleFiles.txt",
                        Ds3TreeTableValue.Type.File, 0L, StringConstants.EMPTY_STRING,
                        StringConstants.TWO_DASH, false);

                final TreeItem<Ds3TreeTableValue> value = new TreeItem<>();
                value.setValue(ds3TreeTableValue);

                final ImmutableList<TreeItem<Ds3TreeTableValue>> values =
                        new ImmutableList.Builder<TreeItem<Ds3TreeTableValue>>()
                                .add(value)
                                .build();

                final ArrayList<Ds3TreeTableValue> filesToDelete = new ArrayList<>(values
                        .stream()
                        .map(TreeItem::getValue)
                        .collect(Collectors.toList())
                );
                final Map<String, List<Ds3TreeTableValue>> bucketObjectsMap = filesToDelete.stream()
                        .collect(Collectors.groupingBy(Ds3TreeTableValue::getBucketName));

                final Ds3DeleteFilesTask deleteFilesTask = new Ds3DeleteFilesTask(session.getClient(),
                        buckets, bucketObjectsMap);
                workers.execute(deleteFilesTask);
                deleteFilesTask.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();
                    fail();
                });
                deleteFilesTask.setOnFailed(event -> latch.countDown());
                deleteFilesTask.setOnCancelled(event -> latch.countDown());
            } catch (final IOException e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}