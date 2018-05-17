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

package com.spectralogic.dsbrowser.gui.util;


import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Request;
import com.spectralogic.ds3client.models.Contents;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.models.common.CommonPrefixes;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BucketUtil {
    private final static Logger LOG = LoggerFactory.getLogger(BucketUtil.class);

    //Get bucket request used to get Bucket data
    public static GetBucketRequest createRequest(final Ds3TreeTableValue ds3Value,
            final String bucket,
            final Ds3TreeTableItem ds3TreeTableItem,
            final ResourceBundle resourceBundle,
            final int pageLength) {
        final GetBucketRequest request = new GetBucketRequest(bucket).withDelimiter(StringConstants.FORWARD_SLASH).withMaxKeys(pageLength);
        if (!Guard.isStringNullOrEmpty(ds3Value.getMarker())) {
            request.withMarker(ds3Value.getMarker());
        }
        if (ds3Value.getType() == Ds3TreeTableValue.Type.Bucket) {
            return request;
        }
        if (Objects.equals(ds3Value.getFullName(), resourceBundle.getString("addMoreButton"))) {
            final Ds3TreeTableValue parent = ds3TreeTableItem.getParent().getValue();
            if (parent.getType() != BaseTreeModel.Type.Bucket) {
                request.withPrefix(parent.getFullName());
            }
        } else {
            request.withPrefix(ds3Value.getFullName());
        }
        return request;
    }

    //Enables you to get list of filtered files based on equals of key and name
    public static ImmutableList<Ds3TreeTableValue> getFilterFilesList(final ImmutableList<Ds3Object> ds3ObjectListFiles,
            final GetBucketResponse bucketResponse,
            final String bucket,
            final Session session,
            final DateTimeUtils dateTimeUtils) {
        final GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Request requestPlacement
                = new GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Request(bucket, ds3ObjectListFiles);
        try {
            return session.getClient().getPhysicalPlacementForObjectsWithFullDetailsSpectraS3(requestPlacement)
                    .getBulkObjectListResult()
                    .getObjects()
                    .stream()
                    .map(objectPhysicalPlacement -> {
                        final Optional<Contents> contentsOptional = bucketResponse.getListBucketResult()
                                .getObjects()
                                .stream()
                                .filter(j -> j.getKey().equals(objectPhysicalPlacement.getName()))
                                .findFirst();
                        if (contentsOptional.isPresent()) {
                            final Contents content = contentsOptional.get();
                            return new Ds3TreeTableValue(bucket,
                                    objectPhysicalPlacement.getName(),
                                    Ds3TreeTableValue.Type.File,
                                    content.getSize(),
                                    dateTimeUtils.format(content.getLastModified()),
                                    content.getOwner().getDisplayName(),
                                    false);
                        } else {
                            LOG.warn("No PhysicalPlacement found for [{}]", objectPhysicalPlacement.getName());
                            return null;
                        }
                    }).filter(Objects::nonNull).collect(GuavaCollectors.immutableList());
        } catch (final IOException e) {
            LOG.error("Unable to get bucket list", e);
        }

        return null;
    }

    //Enables you to get Directories/Folders list
    public static ImmutableList<Ds3TreeTableValue> getDirectoryValues(final GetBucketResponse bucketResponse, final String bucket) {
        final List<CommonPrefixes> commonPrefixes = bucketResponse.getListBucketResult().getCommonPrefixes();
        return commonPrefixes
                .stream()
                .map(cP -> createDs3TreeTableValue(bucket, cP))
                .collect(GuavaCollectors.immutableList());
    }

    @NotNull
    private static Ds3TreeTableValue createDs3TreeTableValue(final String bucket, final CommonPrefixes cP) {
        final HBox hbox = new HBox();
        hbox.getChildren().add(new Label(StringConstants.FOUR_DASH));
        hbox.setAlignment(Pos.CENTER);
        return new Ds3TreeTableValue(bucket, cP.getPrefix(), Ds3TreeTableValue.Type.Directory, 0, StringConstants.TWO_DASH, StringConstants.TWO_DASH, false);
    }

    //function for distinction on the basis of some property
    public static <T> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor) {
        final Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    static String pluralize(final long count, final ResourceBundle resourceBundle, final String one, final String many) {
        return count == 1 ? count + " " + resourceBundle.getString(one) : count + " " + resourceBundle.getString(many);
    }
}
