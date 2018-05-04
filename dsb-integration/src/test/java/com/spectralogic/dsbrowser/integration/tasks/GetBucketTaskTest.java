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

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.services.tasks.GetBucketTask;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import com.spectralogic.dsbrowser.integration.LoggingServiceFake;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.HBox;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

public class GetBucketTaskTest {
    private static final DateTimeUtils DTU = new DateTimeUtils(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    private final Workers workers = new Workers();
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static Session session;
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private static final BuildInfoServiceImpl buildInfoService = new BuildInfoServiceImpl();
    private final static String bucketName = "TestGetBucketTask";

    @BeforeClass
    public static void setUp() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(bucketName,
                    client.getConnectionDetails().getEndpoint(),
                    "80",
                    null,
                    new SavedCredentials(
                            client.getConnectionDetails().getCredentials().getClientId(),
                            client.getConnectionDetails().getCredentials().getKey()),
                    false,
                    false);
            session = CreateConnectionTask.createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle, buildInfoService);
        });
    }

    @Test
    public void getBucket() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try{
                final Ds3TreeTableValue ds3TreeTableValue = new Ds3TreeTableValue(bucketName,
                        bucketName,
                        Ds3TreeTableValue.Type.Bucket,
                        0L,
                        StringConstants.EMPTY_STRING,
                        StringConstants.TWO_DASH,
                        false,
                        Mockito.mock(HBox.class));
                final GetBucketTask getBucketTask = new GetBucketTask(FXCollections.observableArrayList(),
                                                                      bucketName,
                                                                      session,
                                                                      ds3TreeTableValue,
                                                                      workers,
                                                                      DTU,
                                                                      Mockito.mock(Ds3TreeTableItem.class),
                                                                      Mockito.mock(TreeTableView.class),
                                                                      Mockito.mock(Ds3Common.class),
                                                                      new LoggingServiceFake());
                workers.execute(getBucketTask);
                getBucketTask.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();
                });
                getBucketTask.setOnFailed(event -> {
                    latch.countDown();
                });
                getBucketTask.setOnCancelled(event -> {
                    latch.countDown();
                });
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}
