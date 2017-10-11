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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

public final class GetStorageLocations {
    private final static Logger LOG = LoggerFactory.getLogger(GetStorageLocations.class);

    private static final Image ONLINEDISK = new Image(ImageURLs.ONLINE_DISK);
    private static final Image NEARLINEDISK = new Image(ImageURLs.NEARLINE_DISK);
    private static final Image STORAGETAPES = new Image(ImageURLs.STORAGE_TAPES);
    private static final Image EJECTEDTAPES = new Image(ImageURLs.EJECTED_TAPES);
    private static final Image BLACKPEARLCACHE = new Image(ImageURLs.BLACKPEARL_CACHE);
    private static final Image REPLICATION = new Image(ImageURLs.REPLICATION);
    private static final Image CLOUD = new Image(ImageURLs.CLOUD);

    private static final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();

    public static HBox addPlacementIconsAndTooltip(final PhysicalPlacement placement, final boolean inCache) {
        int cloudCount, azureCloud = 0, amazonCloud = 0;

        final HBox placementIconTooltipHbox = new HBox();
        placementIconTooltipHbox.setAlignment(Pos.CENTER);
        placementIconTooltipHbox.setSpacing(3.0);

        if (placement != null && Guard.isNotNullAndNotEmpty(placement.getTapes())) {
            final int storageTapeCount = placement.getTapes().size();
            final long ejectedTapesCount = placement.getTapes().stream().filter(tape -> tape.getEjectDate() != null).count();
            if (ejectedTapesCount < 0) {
                final ImageView ejectedTapeIcon = new ImageView();
                final String toolTipMessage = BucketUtil.pluralize(ejectedTapesCount, resourceBundle, "ejected", "ejecteds");
                ejectedTapeIcon.setImage(EJECTEDTAPES);
                ejectedTapeIcon.setFitHeight(15);
                ejectedTapeIcon.setFitWidth(15);
                Tooltip.install(ejectedTapeIcon, new Tooltip(toolTipMessage));
                placementIconTooltipHbox.getChildren().add(ejectedTapeIcon);
            }
            if ((storageTapeCount - ejectedTapesCount) > 0) {
                final ImageView storageTapeIcon = new ImageView();
                final long tapeCount = storageTapeCount - ejectedTapesCount;
                final String toolTipMessage = BucketUtil.pluralize(tapeCount, resourceBundle, "storage", "storages");
                storageTapeIcon.setImage(STORAGETAPES);
                storageTapeIcon.setFitHeight(15);
                storageTapeIcon.setFitWidth(15);
                Tooltip.install(storageTapeIcon, new Tooltip(toolTipMessage));
                placementIconTooltipHbox.getChildren().add(storageTapeIcon);
            }
        }

        if (placement != null && Guard.isNotNullAndNotEmpty(placement.getPools())) {
            final int onlineDiskCount = placement.getPools().size();
            LOG.info("***onlineDiskCount[{}]", onlineDiskCount);
            final long nearlinePoolsCount = placement.getPools().stream()
                    .filter(pool -> pool.getType().equals(PoolType.NEARLINE))
                    .count();
            LOG.info("***nearlinePoolsCount[{}]", nearlinePoolsCount);
            if (nearlinePoolsCount > 0) {
                final ImageView nearlineDiskIcon = new ImageView();
                final String toolTipMessage = BucketUtil.pluralize(nearlinePoolsCount, resourceBundle, "nearLine", "nearLines");
                nearlineDiskIcon.setImage(NEARLINEDISK);
                nearlineDiskIcon.setFitHeight(15);
                nearlineDiskIcon.setFitWidth(15);
                Tooltip.install(nearlineDiskIcon, new Tooltip(toolTipMessage));
                placementIconTooltipHbox.getChildren().add(nearlineDiskIcon);
            }
            if ((nearlinePoolsCount - onlineDiskCount) > 0) {
                final ImageView onlineDiskIcon = new ImageView();
                final String toolTipMessage = BucketUtil.pluralize(nearlinePoolsCount - onlineDiskCount, resourceBundle, "online", "onlines");
                onlineDiskIcon.setImage(ONLINEDISK);
                onlineDiskIcon.setFitHeight(15);
                onlineDiskIcon.setFitWidth(15);
                Tooltip.install(onlineDiskIcon, new Tooltip(toolTipMessage));
                placementIconTooltipHbox.getChildren().add(onlineDiskIcon);
            }
        }

        if (inCache) {
            final ImageView blackPearlCacheIcon = new ImageView();
            blackPearlCacheIcon.setImage(BLACKPEARLCACHE);
            blackPearlCacheIcon.setFitHeight(15);
            blackPearlCacheIcon.setFitWidth(15);
            Tooltip.install(blackPearlCacheIcon, new Tooltip(resourceBundle.getString("cache")));
            placementIconTooltipHbox.getChildren().add(blackPearlCacheIcon);
        }

        if (placement != null && Guard.isNotNullAndNotEmpty(placement.getDs3Targets())) {
            final long replicationCount = placement.getDs3Targets().stream()
                    .filter(ds3Target -> ds3Target != null
                                         && ds3Target.getReplicatedUserDefaultDataPolicy() != null
                                         && !ds3Target.getReplicatedUserDefaultDataPolicy().isEmpty())
                    .count();
            if (replicationCount > 0) {
                final ImageView replicationIcon = new ImageView();
                final String toolTipMessage = BucketUtil.pluralize(replicationCount, resourceBundle, "replication", "replications");
                replicationIcon.setImage(REPLICATION);
                replicationIcon.setFitHeight(15);
                replicationIcon.setFitWidth(15);
                Tooltip.install(replicationIcon, new Tooltip(toolTipMessage));
                placementIconTooltipHbox.getChildren().add(replicationIcon);
            }
        }

        if (placement != null && Guard.isNotNullAndNotEmpty(placement.getAzureTargets())) {
            azureCloud = placement.getAzureTargets().size();
        }
        if (placement != null && Guard.isNotNullAndNotEmpty(placement.getS3Targets())) {
            amazonCloud = placement.getS3Targets().size();
        }
        cloudCount = azureCloud + amazonCloud;
        if (cloudCount > 0) {
            final ImageView cloudIcon = new ImageView();
            final String toolTipMessage = BucketUtil.pluralize(cloudCount, resourceBundle, "cloud", "clouds");
            cloudIcon.setImage(CLOUD);
            cloudIcon.setFitHeight(15);
            cloudIcon.setFitWidth(15);
            Tooltip.install(cloudIcon, new Tooltip(toolTipMessage));
            placementIconTooltipHbox.getChildren().add(cloudIcon);
        }

        placementIconTooltipHbox.setAlignment(Pos.CENTER);
        if (Guard.isNullOrEmpty(placementIconTooltipHbox.getChildren())) {
            final HBox hbox = new HBox();
            hbox.getChildren().add(new Label(StringConstants.FOUR_DASH));
            hbox.setAlignment(Pos.CENTER);
            placementIconTooltipHbox.getChildren().add(hbox);
        }
        return placementIconTooltipHbox;
    }

}

