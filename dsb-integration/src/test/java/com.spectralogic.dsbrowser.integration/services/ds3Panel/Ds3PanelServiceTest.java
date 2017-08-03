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

package com.spectralogic.dsbrowser.integration.services.ds3Panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.Ds3ClientImpl;
import com.spectralogic.ds3client.commands.spectrads3.GetBucketsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetBucketsSpectraS3Response;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.models.Bucket;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateBucketTask;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TreeTableView;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;


public class Ds3PanelServiceTest {

    private final Workers workers = new Workers();
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static Session session;
    private static final Ds3ClientHelpers HELPERS = Ds3ClientHelpers.wrap(client);
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));

    @BeforeClass
    public static void setUp() {
        new JFXPanel();
            //public Session(final String sessionName, final String endpoint, final String portNo, final String proxyServer, final Ds3Client client,final Boolean defaultSession) {
        session = new Session("DsBrowserTest", cli);
    }

    @Test
    public void checkIfBucketEmpty() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                //Creating empty bucket
                final CreateBucketModel createBucketModel = new CreateBucketModel("fake", UUID.fromString("b8ae2e65-b665-4733-bd48-f7ab760c43f3"));
                final CreateBucketTask createBucketTask = new CreateBucketTask(createBucketModel, client,
                        SessionConstants.DS3_PANEL_SERVICE_TEST_BUCKET_NAME,
                        null, null);
                workers.execute(createBucketTask);
                //Checking is bucket empty
                successFlag = Ds3PanelService.checkIfBucketEmpty(SessionConstants.DS3_PANEL_SERVICE_TEST_BUCKET_NAME, session);
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        Assert.assertTrue(successFlag);
    }


    @Test
    public void setSearchableBucket() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Optional<ImmutableList<Bucket>> searchableBuckets = Ds3PanelService.setSearchableBucket(null, session, Mockito.mock(TreeTableView.class));

                final GetBucketsSpectraS3Request getBucketsSpectraS3Request = new GetBucketsSpectraS3Request();
                final GetBucketsSpectraS3Response response = session.getClient().getBucketsSpectraS3(getBucketsSpectraS3Request);
                final List<Bucket> buckets = response.getBucketListResult().getBuckets();
                searchableBuckets.ifPresent(buckets1 -> successFlag = (!Guard.isNullOrEmpty(buckets1) && buckets1.size() == buckets.size()));
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        Assert.assertTrue(successFlag);
    }
}