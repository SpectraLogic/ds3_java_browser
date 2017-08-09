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

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.util.BucketUtil;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.layout.HBox;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class BucketUtilTest {
    private static Session session;
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static final String TEST_ENV_NAME = "BucketUtilTest";
    private static final String BUCKET_UTIL_TEST_BUCKET_NAME = "BucketUtilTest_Bucket";
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
        });
    }

    @Test
    public void createRequest() throws Exception {
        final Ds3TreeTableValue ds3TreeTableValue = new Ds3TreeTableValue(BUCKET_UTIL_TEST_BUCKET_NAME, BUCKET_UTIL_TEST_BUCKET_NAME,
                Ds3TreeTableValue.Type.Bucket, 0L, "", StringConstants.TWO_DASH,
                false, Mockito.mock(HBox.class));
        final GetBucketRequest request1 = BucketUtil.createRequest(ds3TreeTableValue, BUCKET_UTIL_TEST_BUCKET_NAME,
                Mockito.mock(Ds3TreeTableItem.class), 100);
        ds3TreeTableValue.setMarker("testFolder/");
        final GetBucketRequest request2 = BucketUtil.createRequest(ds3TreeTableValue, BUCKET_UTIL_TEST_BUCKET_NAME,
                Mockito.mock(Ds3TreeTableItem.class), 100);
        successFlag = (request1 != null && request2 != null) ? true : false;
        assertTrue(successFlag);
    }

    @Test
    public void getFilterFilesList() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Ds3TreeTableValue ds3TreeTableValue = new Ds3TreeTableValue(BUCKET_UTIL_TEST_BUCKET_NAME, BUCKET_UTIL_TEST_BUCKET_NAME,
                        Ds3TreeTableValue.Type.Bucket, 0L, "", StringConstants.TWO_DASH,
                        false, Mockito.mock(HBox.class));
                final GetBucketRequest request = BucketUtil.createRequest(ds3TreeTableValue, BUCKET_UTIL_TEST_BUCKET_NAME,
                        Mockito.mock(Ds3TreeTableItem.class), 100);
                final GetBucketResponse bucketResponse = session.getClient().getBucket(request);
                final List<Ds3Object> ds3ObjectListFiles = bucketResponse.getListBucketResult()
                        .getObjects()
                        .stream()
                        .filter(c -> ((c.getKey() != null) && (!c.getKey().equals(ds3TreeTableValue.getFullName()))))
                        .map(i -> new Ds3Object(i.getKey(), i.getSize()))
                        .collect(Collectors.toList());
                final List<Ds3TreeTableValue> filterFilesList = BucketUtil.getFilterFilesList(ds3ObjectListFiles,
                        bucketResponse, BUCKET_UTIL_TEST_BUCKET_NAME, session);
                successFlag = (null != filterFilesList) ? true : false;
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
    public void getDirectoryValues() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        Platform.runLater(() -> {
            try {
                final Ds3TreeTableValue ds3TreeTableValue = new Ds3TreeTableValue(BUCKET_UTIL_TEST_BUCKET_NAME, BUCKET_UTIL_TEST_BUCKET_NAME,
                        Ds3TreeTableValue.Type.Bucket, 0L, "", StringConstants.TWO_DASH,
                        false, Mockito.mock(HBox.class));
                final GetBucketRequest request = BucketUtil.createRequest(ds3TreeTableValue, BUCKET_UTIL_TEST_BUCKET_NAME,
                        Mockito.mock(Ds3TreeTableItem.class), 100);
                final GetBucketResponse bucketResponse = session.getClient().getBucket(request);
                latch.countDown();
                final List<Ds3TreeTableValue> directoryValues = BucketUtil.getDirectoryValues(bucketResponse, BUCKET_UTIL_TEST_BUCKET_NAME);
                successFlag = (null != directoryValues) ? true : false;
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

}