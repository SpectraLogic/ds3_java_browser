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
import java.util.concurrent.atomic.AtomicInteger;

public final class GetStorageLocations {

    private static final Image ONLINEDISK = new Image(ImageURLs.ONLINE_DISK);
    private static final Image NEARLINEDISK = new Image(ImageURLs.NEARLINE_DISK);
    private static final Image STORAGETAPES = new Image(ImageURLs.STORAGE_TAPES);
    private static final Image EJECTEDTAPES = new Image(ImageURLs.EJECTED_TAPES);
    private static final Image BLACKPEARLCACHE = new Image(ImageURLs.BLACKPEARL_CACHE);
    private static final Image REPLICATION = new Image(ImageURLs.REPLICATION);
    private static final Image CLOUD = new Image(ImageURLs.CLOUD);

    private static final AtomicInteger ejectedTapesCount = new AtomicInteger(0);
    private static final AtomicInteger nearLineDiskCount = new AtomicInteger(0);
    private static final AtomicInteger replicationCount = new AtomicInteger(0);
    private static int cloudCount, azureCloud = 0, amazonCloud = 0;
    private static final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();

    public static HBox addPlacementIconsandTooltip(final PhysicalPlacement placement, final boolean inCache) {

        final HBox placementIconTooltipHbox = new HBox();
        placementIconTooltipHbox.setAlignment(Pos.CENTER);
        placementIconTooltipHbox.setSpacing(3.0);

        if (placement != null && Guard.isNotNullAndNotEmpty(placement.getTapes())) {
            final int storageTapeCount = placement.getTapes().size();
            placement.getTapes().forEach(i -> {
                if (i.getEjectDate() != null) {
                    ejectedTapesCount.incrementAndGet();
                }
            });
            if (ejectedTapesCount.intValue() != 0) {
                final ImageView ejectedTapeIcon = new ImageView();
                final String toolTipMessage = pluralize(ejectedTapesCount.intValue(), resourceBundle, "ejected", "ejecteds");
                ejectedTapeIcon.setImage(EJECTEDTAPES);
                ejectedTapeIcon.setFitHeight(15);
                ejectedTapeIcon.setFitWidth(15);
                Tooltip.install(ejectedTapeIcon, new Tooltip(toolTipMessage));
                placementIconTooltipHbox.getChildren().add(ejectedTapeIcon);
            }
            if ((storageTapeCount - ejectedTapesCount.intValue()) != 0) {
                final ImageView storageTapeIcon = new ImageView();
                final int tapeCount = storageTapeCount - ejectedTapesCount.intValue();
                final String toolTipMessage = pluralize(tapeCount, resourceBundle, "storage", "storages");
                storageTapeIcon.setImage(STORAGETAPES);
                storageTapeIcon.setFitHeight(15);
                storageTapeIcon.setFitWidth(15);
                Tooltip.install(storageTapeIcon, new Tooltip(toolTipMessage));
                placementIconTooltipHbox.getChildren().add(storageTapeIcon);
            }
        }

        if (placement != null && Guard.isNotNullAndNotEmpty(placement.getPools())) {
            final int onlineDiskCount = placement.getPools().size();
            placement.getPools().forEach(i -> {
                if (i.getType().equals(PoolType.NEARLINE)) {
                    nearLineDiskCount.incrementAndGet();
                }
            });
            if (nearLineDiskCount.intValue() != 0) {
                final ImageView nearlineDiskIcon = new ImageView();
                final String toolTipMessage = pluralize(nearLineDiskCount.intValue(), resourceBundle, "nearLine", "nearLines");
                nearlineDiskIcon.setImage(NEARLINEDISK);
                nearlineDiskIcon.setFitHeight(15);
                nearlineDiskIcon.setFitWidth(15);
                Tooltip.install(nearlineDiskIcon, new Tooltip(toolTipMessage));
                placementIconTooltipHbox.getChildren().add(nearlineDiskIcon);
            }
            if ((nearLineDiskCount.intValue() - onlineDiskCount) != 0) {
                final ImageView onlineDiskIcon = new ImageView();
                final String toolTipMessage = pluralize(nearLineDiskCount.intValue() - onlineDiskCount, resourceBundle, "online", "onlines");
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
            placement.getDs3Targets().stream()
                    .filter(ds3Target ->
                                ds3Target != null
                                && ds3Target.getReplicatedUserDefaultDataPolicy() != null
                                && !ds3Target.getReplicatedUserDefaultDataPolicy().isEmpty())
                    .forEach(targetWithDefaultDP -> replicationCount.incrementAndGet() );
            if (replicationCount.intValue() != 0) {
                final ImageView replicationIcon = new ImageView();
                final String toolTipMessage = pluralize(replicationCount.intValue(), resourceBundle, "replication", "replications");
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
        if (cloudCount != 0) {
            final ImageView cloudIcon = new ImageView();
            final String toolTipMessage = pluralize(cloudCount, resourceBundle, "cloud", "clouds");
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

    private static String pluralize(final int count, final  ResourceBundle resourceBundle, final String one, final String many) {
        if(count == 1) {
            return count + " " + resourceBundle.getString(one);
        } else {
            return count + " " + resourceBundle.getString(many);
        }
    }
}

