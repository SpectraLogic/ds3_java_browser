package com.spectralogic.dsbrowser.gui.util;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.models.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

public class GetStorageLocations {

    private static final Image ONLINEDISK = new Image(ImageURLs.ONLINEDISK);
    private static final Image NEARLINEDISK = new Image(ImageURLs.NEARLINEDISK);
    private static final Image STORAGETAPES = new Image(ImageURLs.STORAGETAPES);
    private static final Image EJECTEDTAPES = new Image(ImageURLs.EJECTEDTAPES);
    private static final Image BLACKPEARLCACHE = new Image(ImageURLs.BLACKPEARLCACHE);
    private static final Image REPLICATION = new Image(ImageURLs.REPLICATION);

    public static HBox addPlacementIconsandTooltip(final BulkObject objects) {

        final HBox placementIconTooltipHbox = new HBox();
        placementIconTooltipHbox.setAlignment(Pos.CENTER);
        placementIconTooltipHbox.setSpacing(3.0);

        if (objects != null && objects.getPhysicalPlacement().getTapes() != null) {
            final ImmutableList<Tape> ejectedTapes = objects.getPhysicalPlacement().getTapes().stream().filter(i -> i.getEjectDate() != null).collect(GuavaCollectors.immutableList());
            final int storageTapeCount = objects.getPhysicalPlacement().getTapes().size();

            if (ejectedTapes.size() != 0) {
                final ImageView ejectedTapeIcon = new ImageView();
                ejectedTapeIcon.setImage(EJECTEDTAPES);
                ejectedTapeIcon.setFitHeight(15);
                ejectedTapeIcon.setFitWidth(15);
                Tooltip.install(ejectedTapeIcon, new Tooltip(Integer.toString(ejectedTapes.size()) + " copy ejected"));
                placementIconTooltipHbox.getChildren().add(ejectedTapeIcon);
            }

            if ((storageTapeCount - ejectedTapes.size()) != 0) {
                final ImageView storageTapeIcon = new ImageView();
                storageTapeIcon.setImage(STORAGETAPES);
                storageTapeIcon.setFitHeight(15);
                storageTapeIcon.setFitWidth(15);
                Tooltip.install(storageTapeIcon, new Tooltip(Integer.toString(storageTapeCount - ejectedTapes.size()) + " copy on Storage Tape"));
                placementIconTooltipHbox.getChildren().add(storageTapeIcon);
            }
        }

        if (objects.getPhysicalPlacement().getPools() != null) {
            final ImmutableList<Pool> nearLineDisk = objects.getPhysicalPlacement().getPools().stream().filter(i -> i.getType().equals(PoolType.NEARLINE)).collect(GuavaCollectors.immutableList());
            final int onlineDiskCount = objects.getPhysicalPlacement().getPools().size();

            if (nearLineDisk.size() != 0) {
                final ImageView nearlineDiskIcon = new ImageView();
                nearlineDiskIcon.setImage(NEARLINEDISK);
                nearlineDiskIcon.setFitHeight(15);
                nearlineDiskIcon.setFitWidth(15);
                Tooltip.install(nearlineDiskIcon, new Tooltip(Integer.toString(nearLineDisk.size()) + " copy on ArcticBlue"));
                placementIconTooltipHbox.getChildren().add(nearlineDiskIcon);
            }
            if ((onlineDiskCount - nearLineDisk.size()) != 0) {
                final ImageView onlineDiskIcon = new ImageView();
                onlineDiskIcon.setImage(ONLINEDISK);
                onlineDiskIcon.setFitHeight(15);
                onlineDiskIcon.setFitWidth(15);
                Tooltip.install(onlineDiskIcon, new Tooltip(Integer.toString(onlineDiskCount - nearLineDisk.size()) + " copy on Online Disk"));
                placementIconTooltipHbox.getChildren().add(onlineDiskIcon);
            }
        }

        if (objects.getInCache()) {
            final ImageView blackPearlCacheIcon = new ImageView();
            blackPearlCacheIcon.setImage(BLACKPEARLCACHE);
            blackPearlCacheIcon.setFitHeight(15);
            blackPearlCacheIcon.setFitWidth(15);
            Tooltip.install(blackPearlCacheIcon, new Tooltip("In BlackPearl cache"));
            placementIconTooltipHbox.getChildren().add(blackPearlCacheIcon);
        }

        placementIconTooltipHbox.setAlignment(Pos.CENTER);

        if (placementIconTooltipHbox.getChildren().size() == 0) {
            final HBox hbox = new HBox();
            hbox.getChildren().add(new Label("----"));
            hbox.setAlignment(Pos.CENTER);
            placementIconTooltipHbox.getChildren().add(hbox);
        }

        if (objects.getPhysicalPlacement().getDs3Targets() != null) {
            final List<Ds3Target> ds3Targets = objects.getPhysicalPlacement().getDs3Targets();

            if (ds3Targets.size() != 0) {
                final ImageView replicationIcon = new ImageView();
                replicationIcon.setImage(REPLICATION);
                replicationIcon.setFitHeight(15);
                replicationIcon.setFitWidth(15);
                Tooltip.install(replicationIcon, new Tooltip(Integer.toString(ds3Targets.size()) + "BP Replicated copy"));
                placementIconTooltipHbox.getChildren().add(replicationIcon);
            }
        }
        return placementIconTooltipHbox;
    }

    public static HBox addPlacementIconsandTooltip(final PhysicalPlacement placement , final boolean inCache) {

        final HBox placementIconTooltipHbox = new HBox();
        placementIconTooltipHbox.setAlignment(Pos.CENTER);
        placementIconTooltipHbox.setSpacing(3.0);

        if (placement != null && placement.getTapes() != null) {
            final ImmutableList<Tape> ejectedTapes = placement.getTapes().stream().filter(i -> i.getEjectDate() != null).collect(GuavaCollectors.immutableList());
            final int storageTapeCount = placement.getTapes().size();

            if (ejectedTapes.size() != 0) {
                final ImageView ejectedTapeIcon = new ImageView();
                ejectedTapeIcon.setImage(EJECTEDTAPES);
                ejectedTapeIcon.setFitHeight(15);
                ejectedTapeIcon.setFitWidth(15);
                Tooltip.install(ejectedTapeIcon, new Tooltip(Integer.toString(ejectedTapes.size()) + " copy ejected"));
                placementIconTooltipHbox.getChildren().add(ejectedTapeIcon);
            }

            if ((storageTapeCount - ejectedTapes.size()) != 0) {
                final ImageView storageTapeIcon = new ImageView();
                storageTapeIcon.setImage(STORAGETAPES);
                storageTapeIcon.setFitHeight(15);
                storageTapeIcon.setFitWidth(15);
                Tooltip.install(storageTapeIcon, new Tooltip(Integer.toString(storageTapeCount - ejectedTapes.size()) + " copy on Storage Tape"));
                placementIconTooltipHbox.getChildren().add(storageTapeIcon);
            }
        }

        if (placement.getPools() != null) {
            final ImmutableList<Pool> nearLineDisk = placement.getPools().stream().filter(i -> i.getType().equals(PoolType.NEARLINE)).collect(GuavaCollectors.immutableList());
            final int onlineDiskCount = placement.getPools().size();

            if (nearLineDisk.size() != 0) {
                final ImageView nearlineDiskIcon = new ImageView();
                nearlineDiskIcon.setImage(NEARLINEDISK);
                nearlineDiskIcon.setFitHeight(15);
                nearlineDiskIcon.setFitWidth(15);
                Tooltip.install(nearlineDiskIcon, new Tooltip(Integer.toString(nearLineDisk.size()) + " copy on Nearline Disk"));
                placementIconTooltipHbox.getChildren().add(nearlineDiskIcon);
            }
            if ((onlineDiskCount - nearLineDisk.size()) != 0) {
                final ImageView onlineDiskIcon = new ImageView();
                onlineDiskIcon.setImage(ONLINEDISK);
                onlineDiskIcon.setFitHeight(15);
                onlineDiskIcon.setFitWidth(15);
                Tooltip.install(onlineDiskIcon, new Tooltip(Integer.toString(onlineDiskCount - nearLineDisk.size()) + " copy on Online Disk"));
                placementIconTooltipHbox.getChildren().add(onlineDiskIcon);
            }
        }

        if (inCache) {
            final ImageView blackPearlCacheIcon = new ImageView();
            blackPearlCacheIcon.setImage(BLACKPEARLCACHE);
            blackPearlCacheIcon.setFitHeight(15);
            blackPearlCacheIcon.setFitWidth(15);
            Tooltip.install(blackPearlCacheIcon, new Tooltip("In cache"));
            placementIconTooltipHbox.getChildren().add(blackPearlCacheIcon);
        }

        placementIconTooltipHbox.setAlignment(Pos.CENTER);

        if (placementIconTooltipHbox.getChildren().size() == 0) {
            final HBox hbox = new HBox();
            hbox.getChildren().add(new Label("----"));
            hbox.setAlignment(Pos.CENTER);
            placementIconTooltipHbox.getChildren().add(hbox);
        }

        if (placement.getDs3Targets() != null) {
            final List<Ds3Target> ds3Targets = placement.getDs3Targets();

            if (ds3Targets.size() != 0) {
                final ImageView replicationIcon = new ImageView();
                replicationIcon.setImage(REPLICATION);
                replicationIcon.setFitHeight(15);
                replicationIcon.setFitWidth(15);
                Tooltip.install(replicationIcon, new Tooltip(Integer.toString(ds3Targets.size()) + " Replicated copy"));
                placementIconTooltipHbox.getChildren().add(replicationIcon);
            }
        }
        return placementIconTooltipHbox;
    }

}

