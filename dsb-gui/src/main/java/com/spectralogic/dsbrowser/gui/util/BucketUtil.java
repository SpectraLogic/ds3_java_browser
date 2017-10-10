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

package com.spectralogic.dsbrowser.gui.util;


import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Response;
import com.spectralogic.ds3client.models.Contents;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.spectralogic.dsbrowser.gui.util.GetStorageLocations.addPlacementIconsandTooltip;

public final class BucketUtil {
    private final static Logger LOG = LoggerFactory.getLogger(BucketUtil.class);

    //Get bucket request used to get Bucket data
    public static GetBucketRequest createRequest(final Ds3TreeTableValue ds3Value, final String bucket,
                                                 final Ds3TreeTableItem ds3TreeTableItem, final int pageLength) {
        final GetBucketRequest request;
        //if marker is set blank for a item that means offset is 0 else set the marker
        if (ds3Value.getMarker().equals(StringConstants.EMPTY_STRING)) {
            request = new GetBucketRequest(bucket).withDelimiter(StringConstants.FORWARD_SLASH).withMaxKeys(pageLength);
        } else {
            request = new GetBucketRequest(bucket).withDelimiter(StringConstants.FORWARD_SLASH).withMaxKeys(pageLength)
                    .withMarker(ds3Value.getMarker());
        }
        if (ds3Value.getType() != Ds3TreeTableValue.Type.Bucket) {
            if (ds3Value.getType() == Ds3TreeTableValue.Type.Loader) {
                if (ds3TreeTableItem.getParent().getValue().getType() != Ds3TreeTableValue.Type.Bucket) {
                    final Ds3TreeTableValue ds3ParentValue = ds3TreeTableItem.getParent().getValue();
                    request.withPrefix(ds3ParentValue.getFullName());
                }
            } else {
                request.withPrefix(ds3Value.getFullName());
            }
        }
        return request;
    }

    //Enables you to get list of filtered files based on equals of key and name
    public static List<Ds3TreeTableValue> getFilterFilesList(final List<Ds3Object> ds3ObjectListFiles,
                                                             final GetBucketResponse bucketResponse, final String bucket,
                                                             final Session session, final DateTimeUtils dateTimeUtils) {
        final List<Ds3TreeTableValue> filteredFiles = new ArrayList<>();
        try {
            final GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Request requestPlacement
                    = new GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Request(bucket, ds3ObjectListFiles);
            final GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Response responsePlacement
                    = session.getClient().getPhysicalPlacementForObjectsWithFullDetailsSpectraS3(requestPlacement);
            final List<Ds3TreeTableValue> filteredFilesList = responsePlacement
                    .getBulkObjectListResult()
                    .getObjects()
                    .stream()
                    .map(i -> {
                        final Contents content = bucketResponse.getListBucketResult()
                                .getObjects()
                                .stream()
                                .filter(j -> j.getKey().equals(i.getName()))
                                .findFirst()
                                .get();
                        final HBox iconsAndTooltip = addPlacementIconsandTooltip(i.getPhysicalPlacement(), i.getInCache());
                        return new Ds3TreeTableValue(bucket, i.getName(), Ds3TreeTableValue.Type.File,
                                content.getSize(), dateTimeUtils.format(content.getLastModified()),
                                content.getOwner().getDisplayName(), false, iconsAndTooltip);
                    }).collect(Collectors.toList());
            filteredFiles.addAll(filteredFilesList);
        } catch (final Exception e) {
            LOG.error("Unable to get bucket list", e);
        }
        return filteredFiles;
    }

    //Enables you to get Directories/Folders list
    public static List<Ds3TreeTableValue> getDirectoryValues(final GetBucketResponse bucketResponse, final String bucket) {
        return bucketResponse.getListBucketResult().getCommonPrefixes().stream().map(i ->
        {
            final String folderName = i.getPrefix();
            final HBox hbox = new HBox();
            hbox.getChildren().add(new Label(StringConstants.FOUR_DASH));
            hbox.setAlignment(Pos.CENTER);
            return new Ds3TreeTableValue(bucket, folderName, Ds3TreeTableValue.Type.Directory, 0,
                    StringConstants.TWO_DASH, StringConstants.TWO_DASH, false, hbox);

        }).collect(Collectors.toList());
    }

    //function for distinction on the basis of some property
    public static <T> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor) {
        final Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

}
