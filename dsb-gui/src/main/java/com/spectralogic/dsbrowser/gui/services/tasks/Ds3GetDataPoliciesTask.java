
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

package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetDataPoliciesSpectraS3Request;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketWithDataPoliciesModel;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.ResourceBundle;

public class Ds3GetDataPoliciesTask extends Ds3Task {
    private final static Logger LOG = LoggerFactory.getLogger(GetBucketTask.class);
    private final Session session;
    private final Workers workers;
    private final ResourceBundle resourceBundle;
    private final LoggingService loggingService;

    public Ds3GetDataPoliciesTask(final Session session,
                                  final Workers workers,
                                  final ResourceBundle resourceBundle,
                                  final LoggingService loggingService) {
        this.resourceBundle = resourceBundle;
        this.session = session;
        this.workers = workers;
        this.loggingService = loggingService;
    }

    @Override
    protected Optional<CreateBucketWithDataPoliciesModel> call() throws Exception {
        try {
            final Ds3Client client = session.getClient();
            LOG.debug("Getting DataPolicies from [{}]", session.getEndpoint());
            final ImmutableList<CreateBucketModel> buckets = client.getDataPoliciesSpectraS3(new GetDataPoliciesSpectraS3Request()).getDataPolicyListResult().
                    getDataPolicies().stream().map(bucket -> new CreateBucketModel(bucket.getName(), bucket.getId())).collect(GuavaCollectors.immutableList());
            final ImmutableList<CreateBucketWithDataPoliciesModel> dataPoliciesList = buckets.stream().map(policies ->
                    new CreateBucketWithDataPoliciesModel(buckets, session, workers)).collect(GuavaCollectors.immutableList());

            LOG.debug("Found DataPolicies on [{}]", session.getEndpoint());
            loggingService.logMessage(resourceBundle.getString("dataPolicyRetrieved"), LogType.SUCCESS);

            final Optional<CreateBucketWithDataPoliciesModel> first = dataPoliciesList.stream().findFirst();
            return Optional.ofNullable(first.get());
        } catch (final Exception e) {
            LOG.error("Failed to get DataPolicies from " + session.getEndpoint(), e);
            loggingService.logMessage(resourceBundle.getString("dataPolicyNotFoundErr") + StringConstants.SPACE + session.getEndpoint() + StringConstants.SPACE + e, LogType.ERROR);
            throw e;
        }
    }
}
