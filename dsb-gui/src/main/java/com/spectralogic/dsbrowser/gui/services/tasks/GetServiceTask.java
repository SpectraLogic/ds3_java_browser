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
import com.spectralogic.ds3client.commands.GetServiceRequest;
import com.spectralogic.ds3client.commands.GetServiceResponse;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.ResourceBundle;

public class GetServiceTask extends Ds3Task {

    private final static Logger LOG = LoggerFactory.getLogger(GetServiceTask.class);

    private final ReadOnlyObjectWrapper<ObservableList<TreeItem<Ds3TreeTableValue>>> partialResults;
    private final Session session;
    private final Workers workers;
    private final Ds3Common ds3Common;
    private final ResourceBundle resourceBundle;
    private final LoggingService loggingService;

    public GetServiceTask(final ObservableList<TreeItem<Ds3TreeTableValue>> observableList,
                          final Session session,
                          final Workers workers,
                          final Ds3Common ds3Common,
                          final LoggingService loggingService) {
        this.partialResults = new ReadOnlyObjectWrapper<>(this, "partialResults", observableList);
        this.session = session;
        this.workers = workers;
        this.ds3Common = ds3Common;
        this.resourceBundle = ResourceBundleProperties.getResourceBundle();
        this.loggingService = loggingService;
    }

    @Override
    protected ObservableList<TreeItem<Ds3TreeTableValue>> call() throws Exception {
        final GetServiceResponse response = session.getClient().getService(new GetServiceRequest());
        if (null != response
                && null != response.getListAllMyBucketsResult()
                && !Guard.isNullOrEmpty(response.getListAllMyBucketsResult().getBuckets())) {
            final ImmutableList<Ds3TreeTableValue> buckets = response.getListAllMyBucketsResult()
                    .getBuckets().stream()
                    .map(bucket -> {
                        final HBox hbox = new HBox();
                        hbox.getChildren().add(new Label(StringConstants.FOUR_DASH));
                        hbox.setAlignment(Pos.CENTER);
                        return new Ds3TreeTableValue(bucket.getName(), bucket.getName(), Ds3TreeTableValue.Type.Bucket,
                                0, DateTimeUtils.format(bucket.getCreationDate()), StringConstants.TWO_DASH,
                                false, hbox);
                    }).sorted(Comparator.comparing(b -> b.getName().toLowerCase())).collect(GuavaCollectors.immutableList());

            loggingService.logMessage(resourceBundle.getString("receivedBucketList"), LogType.SUCCESS);
            Platform.runLater(() -> {
                if (null != ds3Common) {
                    if (null != ds3Common.getDeepStorageBrowserPresenter() && null != ds3Common.getDs3PanelPresenter()) {
                        ds3Common.getDs3PanelPresenter().disableSearch(false);
                    }
                    final ImmutableList<Ds3TreeTableItem> treeItems = buckets.stream().map(value ->
                            new Ds3TreeTableItem(value.getName(), session, value, workers, ds3Common, loggingService))
                            .collect(GuavaCollectors.immutableList());
                    if (!Guard.isNullOrEmpty(treeItems)) {
                        partialResults.get().addAll(treeItems);
                        ds3Common.getDs3PanelPresenter().disableSearch(false);
                    } else {
                        ds3Common.getDs3PanelPresenter().disableSearch(true);
                        LOG.info("No buckets found");
                    }
                } else {
                    LOG.info("Ds3Common is null");
                }
            });
        } else {
            LOG.info("No buckets found");
            ds3Common.getDs3PanelPresenter().disableSearch(true);
        }
        return this.partialResults.get();
    }
}
