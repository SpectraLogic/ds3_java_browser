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

package com.spectralogic.dsbrowser.integration.services.ds3Panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.browser.gui.testUtil.LoggingServiceFake;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.commands.spectrads3.GetBucketsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetBucketsSpectraS3Response;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.models.Bucket;
import com.spectralogic.ds3client.models.ChecksumType;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.PhysicalPlacementPopup;
import com.spectralogic.dsbrowser.gui.components.version.VersionPopup;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.Ds3PanelService;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateBucketTask;
import com.spectralogic.dsbrowser.gui.services.tasks.PhysicalPlacementTask;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.integration.IntegrationHelpers;
import com.spectralogic.dsbrowser.integration.TempStorageIds;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TreeTableView;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;


public class Ds3PanelServiceTest {

    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
    private final LoggingService loggingService = new LoggingServiceFake();
    private final DateTimeUtils dateTimeUtils = new DateTimeUtils();
    private final Workers workers = new Workers();
    private final Ds3Common ds3Common = new Ds3Common();
    private static final Ds3Client client = Ds3ClientBuilder.fromEnv().withHttps(false).build();
    private static final Ds3ClientHelpers HELPERS = Ds3ClientHelpers.wrap(client);
    private static Session session;
    private boolean successFlag = false;
    private static final String TEST_ENV_NAME = "Ds3PanelServiceTest";
    private static final String DS3_PANEL_SERVICE_TEST_BUCKET_NAME = "Ds3PanelServiceTest_Bucket";
    private static TempStorageIds envStorageIds;
    private static UUID envDataPolicyId;
    private final RefreshCompleteViewWorker refreshCompleteViewWorker = new RefreshCompleteViewWorker(null, workers, dateTimeUtils, loggingService);
    private final AlertService alertService = new AlertService(resourceBundle);
    private final PhysicalPlacementTask.PhysicalPlacementTaskFactory physicalPlacementTaskFactory = new PhysicalPlacementTask.PhysicalPlacementTaskFactory() {
        @Override
        public PhysicalPlacementTask create(final Ds3TreeTableValue value) {
            return null;
        }
    };
    private final Ds3PanelService ds3PanelService = new Ds3PanelService(resourceBundle,
            dateTimeUtils,
            ds3Common,
            workers,
            loggingService,
            refreshCompleteViewWorker,
            new VersionPopup(resourceBundle, ds3Common, alertService),
            new Popup(),
            new PhysicalPlacementPopup(resourceBundle, ds3Common),
            physicalPlacementTaskFactory);

    @BeforeClass
    public static void setUp() {
        new JFXPanel();
        session = new Session("Ds3PanelServiceTest",
                client.getConnectionDetails().getEndpoint(),
                "80",
                null,
                client,
                false,
                false);
        try {
            envDataPolicyId = IntegrationHelpers.setupDataPolicy(TEST_ENV_NAME, false, ChecksumType.Type.MD5, client);
            envStorageIds = IntegrationHelpers.setup(TEST_ENV_NAME, envDataPolicyId, client);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void teardown() throws IOException {
        IntegrationHelpers.teardown(TEST_ENV_NAME, envStorageIds, client);
        client.close();
    }

    @Test
    public void checkIfBucketEmpty() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                //Creating empty bucket
                final String bucketName = "emptyTestBucket";
                HELPERS.ensureBucketExists(bucketName, envDataPolicyId);

                final CreateBucketModel createBucketModel = new CreateBucketModel("test_dp", envDataPolicyId);
                final CreateBucketTask createBucketTask = new CreateBucketTask(createBucketModel, client,
                        bucketName, null, null);
                workers.execute(createBucketTask);

                //Checking is bucket empty
                successFlag = ds3PanelService.checkIfBucketEmpty(bucketName, session);
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
    public void setSearchableBucket() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                HELPERS.ensureBucketExists(DS3_PANEL_SERVICE_TEST_BUCKET_NAME, envDataPolicyId);

                final Optional<ImmutableList<Bucket>> searchableBuckets = ds3PanelService.setSearchableBucket(null, session, Mockito.mock(TreeTableView.class));

                final GetBucketsSpectraS3Request getBucketsSpectraS3Request = new GetBucketsSpectraS3Request();
                final GetBucketsSpectraS3Response response = session.getClient().getBucketsSpectraS3(getBucketsSpectraS3Request);
                final List<Bucket> buckets = response.getBucketListResult().getBuckets();
                searchableBuckets.ifPresent(buckets1 -> successFlag = !Guard.isNullOrEmpty(buckets1) && buckets1.size() == buckets.size());
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}
