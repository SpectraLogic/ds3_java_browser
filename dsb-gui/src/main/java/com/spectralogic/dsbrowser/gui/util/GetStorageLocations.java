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

import com.spectralogic.ds3client.models.*;
import com.spectralogic.ds3client.utils.Guard;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.ResourceBundle;

public final class GetStorageLocations {
    private static final Image ONLINEDISK = new Image(ImageURLs.ONLINE_DISK);
    private static final Image NEARLINEDISK = new Image(ImageURLs.NEARLINE_DISK);
    private static final Image STORAGETAPES = new Image(ImageURLs.STORAGE_TAPES);
    private static final Image EJECTEDTAPES = new Image(ImageURLs.EJECTED_TAPES);
    private static final Image BLACKPEARLCACHE = new Image(ImageURLs.BLACKPEARL_CACHE);
    private static final Image REPLICATION = new Image(ImageURLs.REPLICATION);
    private static final Image CLOUD = new Image(ImageURLs.CLOUD);

    private static final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();

    public static HBox addPlacementIconsAndTooltip(final PhysicalPlacement placement, final boolean inCache) {

        final HBox placementIconTooltipHbox = new HBox();

        addTapeIconsAndTooltip(placement, placementIconTooltipHbox);

        addPoolIconsAndTooltip(placement, placementIconTooltipHbox);

        addCacheIconAndTooltip(inCache, placementIconTooltipHbox);

        addReplicationIconAndTooltip(placement, placementIconTooltipHbox);

        addCloudIconAndTooltip(placement, placementIconTooltipHbox);

        placementIconTooltipHbox.setAlignment(Pos.CENTER);
        placementIconTooltipHbox.setSpacing(3.0);
        if (Guard.isNullOrEmpty(placementIconTooltipHbox.getChildren())) {
            final HBox hbox = new HBox();
            hbox.getChildren().add(new Label(StringConstants.FOUR_DASH));
            hbox.setAlignment(Pos.CENTER);
            placementIconTooltipHbox.getChildren().add(hbox);
        }
        return placementIconTooltipHbox;
    }

    private static void addCloudIconAndTooltip(final PhysicalPlacement placement, final HBox placementIconTooltipHbox) {
        int azureCloud = 0, amazonCloud = 0;
        if (placement != null && Guard.isNotNullAndNotEmpty(placement.getAzureTargets())) {
            azureCloud = placement.getAzureTargets().size();
        }
        if (placement != null && Guard.isNotNullAndNotEmpty(placement.getS3Targets())) {
            amazonCloud = placement.getS3Targets().size();
        }

        final int cloudCount = azureCloud + amazonCloud;
        if (cloudCount > 0) {
            final String toolTipMessage = BucketUtil.pluralize(cloudCount, resourceBundle, "cloud", "clouds");
            placementIconTooltipHbox.getChildren().add(createIcon(CLOUD, toolTipMessage));
        }
    }

    private static void addReplicationIconAndTooltip(final PhysicalPlacement placement, final HBox placementIconTooltipHbox) {
        if (placement != null && Guard.isNotNullAndNotEmpty(placement.getDs3Targets())) {
            final long replicationCount = placement.getDs3Targets().stream()
                    .filter(ds3Target -> ds3Target != null
                                         && ds3Target.getReplicatedUserDefaultDataPolicy() != null
                                         && !ds3Target.getReplicatedUserDefaultDataPolicy().isEmpty())
                    .count();
            if (replicationCount > 0) {
                final String toolTipMessage = BucketUtil.pluralize(replicationCount, resourceBundle, "replication", "replications");
                placementIconTooltipHbox.getChildren().add(createIcon(REPLICATION, toolTipMessage));
            }
        }
    }

    private static void addCacheIconAndTooltip(final boolean inCache, final HBox placementIconTooltipHbox) {
        if (inCache) {
            placementIconTooltipHbox.getChildren().add(createIcon(BLACKPEARLCACHE, resourceBundle.getString("cache")));
        }
    }

    private static void addPoolIconsAndTooltip(final PhysicalPlacement placement, final HBox placementIconTooltipHbox) {
        if (placement != null && Guard.isNotNullAndNotEmpty(placement.getPools())) {
            final int poolsCount = placement.getPools().size();
            final long nearlinePoolsCount = placement.getPools().stream()
                    .filter(pool -> pool.getType().equals(PoolType.NEARLINE))
                    .count();
            if (nearlinePoolsCount > 0) {
                final String toolTipMessage = BucketUtil.pluralize(nearlinePoolsCount, resourceBundle, "nearLine", "nearLines");
                placementIconTooltipHbox.getChildren().add(createIcon(NEARLINEDISK, toolTipMessage));
            }
            if (poolsCount - nearlinePoolsCount > 0) {
                final String toolTipMessage = BucketUtil.pluralize(poolsCount - nearlinePoolsCount, resourceBundle, "online", "onlines");
                placementIconTooltipHbox.getChildren().add(createIcon(ONLINEDISK, toolTipMessage));
            }
        }
    }

    private static void addTapeIconsAndTooltip(final PhysicalPlacement placement, final HBox placementIconTooltipHbox) {
        if (placement != null && Guard.isNotNullAndNotEmpty(placement.getTapes())) {
            final int storageTapeCount = placement.getTapes().size();
            final long ejectedTapesCount = placement.getTapes().stream().filter(tape -> tape.getEjectDate() != null).count();
            if (ejectedTapesCount < 0) {
                final String toolTipMessage = BucketUtil.pluralize(ejectedTapesCount, resourceBundle, "ejected", "ejecteds");
                placementIconTooltipHbox.getChildren().add(createIcon(EJECTEDTAPES, toolTipMessage));
            }
            if (storageTapeCount - ejectedTapesCount > 0) {
                final long tapeCount = storageTapeCount - ejectedTapesCount;
                final String toolTipMessage = BucketUtil.pluralize(tapeCount, resourceBundle, "storage", "storages");
                placementIconTooltipHbox.getChildren().add(createIcon(STORAGETAPES, toolTipMessage));
            }
        }
    }

    private static ImageView createIcon(final Image image, final String tooltipMessage) {
        final ImageView icon = new ImageView();
        icon.setImage(image);
        icon.setFitHeight(15);
        icon.setFitWidth(15);
        Tooltip.install(icon, new Tooltip(tooltipMessage));
        return icon;
    }

}

