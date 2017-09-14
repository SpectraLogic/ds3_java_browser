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

import com.spectralogic.ds3client.commands.spectrads3.GetObjectsWithFullDetailsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetObjectsWithFullDetailsSpectraS3Response;
import com.spectralogic.ds3client.models.Bucket;
import com.spectralogic.ds3client.models.BulkObject;
import com.spectralogic.ds3client.models.DetailedS3Object;
import com.spectralogic.ds3client.models.S3ObjectType;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SearchJobTask extends Ds3Task<List<Ds3TreeTableItem>> {
    private final static Logger LOG = LoggerFactory.getLogger(SearchJobTask.class);
    private final List<Bucket> searchableBuckets;
    private final String searchText;
    private final Session session;
    private final Workers workers;
    private final Ds3Common ds3Common;
    private final LoggingService loggingService;

    public SearchJobTask(final List<Bucket> searchableBuckets,
                         final String searchText,
                         final Session session,
                         final Workers workers,
                         final Ds3Common ds3Common,
                         final LoggingService loggingService) {
        this.searchableBuckets = searchableBuckets;
        this.searchText = searchText.trim();
        this.session = session;
        this.workers = workers;
        this.ds3Common = ds3Common;
        this.loggingService = loggingService;
    }

    @Override
    protected List<Ds3TreeTableItem> call() throws Exception {
        try {
            final List<Ds3TreeTableItem> list = new ArrayList<>();
            searchableBuckets.forEach(bucket -> {
                if (bucket.getName().contains(searchText)) {
                    loggingService.logMessage(StringBuilderUtil.bucketFoundMessage("'" + searchText + "'", bucket.getName()).toString(), LogType.SUCCESS);
                    final Ds3TreeTableValue value = new Ds3TreeTableValue(bucket.getName(), bucket.getName(), Ds3TreeTableValue.Type.Bucket,
                            0, StringConstants.TWO_DASH, StringConstants.TWO_DASH, false, null);
                    list.add(new Ds3TreeTableItem(value.getName(), session, value, workers, ds3Common, loggingService));
                } else {
                    final List<DetailedS3Object> detailedDs3Objects = getDetailedDs3Objects(bucket.getName());
                    if (Guard.isNotNullAndNotEmpty(detailedDs3Objects)) {
                        final List<Ds3TreeTableItem> treeTableItems = buildTreeItems(detailedDs3Objects, bucket.getName());
                        if (Guard.isNotNullAndNotEmpty(treeTableItems)) {
                            list.addAll(treeTableItems);
                            loggingService.logMessage(StringBuilderUtil.searchInBucketMessage(bucket.getName(), list.size()).toString(),
                                    LogType.SUCCESS);
                        }
                    }
                }
            });
            return list;
        } catch (final Exception e) {
            LOG.error("Search failed", e);
            loggingService.logMessage(StringBuilderUtil.searchFailedMessage().append(e).toString(), LogType.ERROR);
            return null;
        }
    }

    /**
     * To build the treeTableItem from the object list. This method considers the files only.
     *
     * @param detailedS3Objects object list with detailed information
     * @param bucketName        bucket's name
     * @return list of treeTableItem
     */
    private List<Ds3TreeTableItem> buildTreeItems(final List<DetailedS3Object> detailedS3Objects,
                                                  final String bucketName) {
        final List<Ds3TreeTableItem> list = new ArrayList<>();
        detailedS3Objects.forEach(itemObject -> {
                    if (!itemObject.getType().equals(S3ObjectType.FOLDER)) {
                        HBox physicalPlacementHBox = null;
                        //TO get the physical placement of the objects
                        if (itemObject.getBlobs() != null && !Guard.isNullOrEmpty(itemObject.getBlobs().getObjects())) {
                            final List<BulkObject> objects = itemObject.getBlobs().getObjects();
                            physicalPlacementHBox = getConfiguredHBox(objects);
                        }
                        final Ds3TreeTableValue treeTableValue = new Ds3TreeTableValue(bucketName, itemObject.getName(),
                                Ds3TreeTableValue.Type.File, itemObject.getSize(),
                                DateTimeUtils.format(itemObject.getCreationDate()), itemObject.getOwner(), true, physicalPlacementHBox);
                        list.add(new Ds3TreeTableItem(treeTableValue.getFullName(), session,
                                treeTableValue, workers, ds3Common, loggingService));
                    }
                }
        );
        return list;
    }

    /**
     * To get the detailed object list from a bucket.
     *
     * @param bucketName bucketName
     * @return list of Detailed objects
     */
    private List<DetailedS3Object> getDetailedDs3Objects(final String bucketName) {
        try {
            final GetObjectsWithFullDetailsSpectraS3Request request = new GetObjectsWithFullDetailsSpectraS3Request()
                    .withBucketId(bucketName).withName(StringConstants.PERCENT + searchText + StringConstants.PERCENT)
                    .withIncludePhysicalPlacement(true);
            final GetObjectsWithFullDetailsSpectraS3Response responseFullDetails = session.getClient().getObjectsWithFullDetailsSpectraS3(request);
            return responseFullDetails.getDetailedS3ObjectListResult().getDetailedS3Objects();
        } catch (final Exception e) {
            LOG.error("Not able to fetch detailed object list", e);
            return null;
        }
    }

    /**
     * To add Physical placement icons and tooltip
     *
     * @param objects object
     * @return HBox
     */
    private HBox getConfiguredHBox(final List<BulkObject> objects) {
        final Optional<BulkObject> first = objects.stream().findFirst();
        if (first.isPresent()) {
            final BulkObject bulkObject = first.get();
            return GetStorageLocations.addPlacementIconsandTooltip(bulkObject.getPhysicalPlacement(), bulkObject.getInCache());
        }
        return null;
    }

}
