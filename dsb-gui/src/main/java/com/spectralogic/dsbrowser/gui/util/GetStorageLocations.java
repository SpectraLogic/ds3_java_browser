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
    private static int cloudCount, azureCloud, amazoneCloud = 0;
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
                ejectedTapeIcon.setImage(EJECTEDTAPES);
                ejectedTapeIcon.setFitHeight(15);
                ejectedTapeIcon.setFitWidth(15);
                Tooltip.install(ejectedTapeIcon, new Tooltip(Integer.toString(ejectedTapesCount.intValue()) + resourceBundle.getString("ejected")));
                placementIconTooltipHbox.getChildren().add(ejectedTapeIcon);
            }
            if ((storageTapeCount - ejectedTapesCount.intValue()) != 0) {
                final ImageView storageTapeIcon = new ImageView();
                storageTapeIcon.setImage(STORAGETAPES);
                storageTapeIcon.setFitHeight(15);
                storageTapeIcon.setFitWidth(15);
                Tooltip.install(storageTapeIcon, new Tooltip(Integer.toString(storageTapeCount - ejectedTapesCount.intValue()) + resourceBundle.getString("storage")));
                placementIconTooltipHbox.getChildren().add(storageTapeIcon);
            }
        }

        if (Guard.isNotNullAndNotEmpty(placement.getPools())) {
            final int onlineDiskCount = placement.getPools().size();
            placement.getPools().forEach(i -> {
                if (i.getType().equals(PoolType.NEARLINE)) {
                    nearLineDiskCount.incrementAndGet();
                }
            });
            if (nearLineDiskCount.intValue() != 0) {
                final ImageView nearlineDiskIcon = new ImageView();
                nearlineDiskIcon.setImage(NEARLINEDISK);
                nearlineDiskIcon.setFitHeight(15);
                nearlineDiskIcon.setFitWidth(15);
                Tooltip.install(nearlineDiskIcon, new Tooltip(Integer.toString(nearLineDiskCount.intValue()) + resourceBundle.getString("nearLine")));
                placementIconTooltipHbox.getChildren().add(nearlineDiskIcon);
            }
            if ((nearLineDiskCount.intValue() - onlineDiskCount) != 0) {
                final ImageView onlineDiskIcon = new ImageView();
                onlineDiskIcon.setImage(ONLINEDISK);
                onlineDiskIcon.setFitHeight(15);
                onlineDiskIcon.setFitWidth(15);
                Tooltip.install(onlineDiskIcon, new Tooltip(Integer.toString(nearLineDiskCount.intValue() - onlineDiskCount) + resourceBundle.getString("online")));
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

        if (Guard.isNotNullAndNotEmpty(placement.getDs3Targets())) {
            placement.getDs3Targets().forEach(i -> {
                if (!i.getReplicatedUserDefaultDataPolicy().isEmpty()) {
                    replicationCount.incrementAndGet();
                }
            });
            if (replicationCount.intValue() != 0) {
                final ImageView replicationIcon = new ImageView();
                replicationIcon.setImage(REPLICATION);
                replicationIcon.setFitHeight(15);
                replicationIcon.setFitWidth(15);
                Tooltip.install(replicationIcon, new Tooltip(Integer.toString(replicationCount.intValue()) + resourceBundle.getString("replication")));
                placementIconTooltipHbox.getChildren().add(replicationIcon);
            }
        }

        if (Guard.isNotNullAndNotEmpty(placement.getAzureTargets())) {
            azureCloud = placement.getAzureTargets().size();
        }
        if (Guard.isNotNullAndNotEmpty(placement.getS3Targets())) {
            amazoneCloud = placement.getS3Targets().size();
        }
        cloudCount = azureCloud + amazoneCloud;
        if (cloudCount != 0) {
            final ImageView cloudIcon = new ImageView();
            cloudIcon.setImage(CLOUD);
            cloudIcon.setFitHeight(15);
            cloudIcon.setFitWidth(15);
            Tooltip.install(cloudIcon, new Tooltip(Integer.toString(cloudCount) + resourceBundle.getString("cloud")));
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

