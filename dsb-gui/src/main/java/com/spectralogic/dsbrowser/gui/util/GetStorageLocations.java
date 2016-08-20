package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.models.BulkObject;
import com.spectralogic.ds3client.models.PoolType;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

public class GetStorageLocations {

    private static final Image ONLINE_DISK = new Image(GetStorageLocations.class.getResource("/images/online_disk.png").toString());
    private static final Image NEARLINE_DISK = new Image(GetStorageLocations.class.getResource("/images/nearline-disk.png").toString());
    private static final Image STORAGE_TAPES = new Image(GetStorageLocations.class.getResource("/images/storage-tapes.png").toString());
    private static final Image EJECTED_TAPES = new Image(GetStorageLocations.class.getResource("/images/ejected-tapes.png").toString());
    private static final Image BLACK_PEARL_CACHE = new Image(GetStorageLocations.class.getResource("/images/cache.png").toString());
    private static final Image REPLICATION = new Image(GetStorageLocations.class.getResource("/images/replication.png").toString());

    public static HBox addPlacementIconsandTooltip(BulkObject objects) {

        final int onlineDiskCount;
        final List<Integer> nearLineDiskCount = new ArrayList<>();
        final List<Integer> ejectedTapeCount = new ArrayList<>();

        final HBox placementIconTooltipHbox = new HBox();
        placementIconTooltipHbox.setAlignment(Pos.CENTER);
        placementIconTooltipHbox.setSpacing(3.0);

        if (objects.getPhysicalPlacement().getTapes() != null) {
            objects.getPhysicalPlacement().getTapes().forEach(tape -> {
                        if (tape.getEjectDate() != null) {
                            ejectedTapeCount.add(1);
                        }
                    }
            );
            if (ejectedTapeCount.size() != 0) {

                final ImageView ejectedTapeIcon = new ImageView();
                ejectedTapeIcon.setImage(EJECTED_TAPES);
                ejectedTapeIcon.setFitHeight(15);
                ejectedTapeIcon.setFitWidth(15);
                Tooltip.install(ejectedTapeIcon, new Tooltip(Integer.toString(ejectedTapeCount.size()) + " copy ejected"));
                placementIconTooltipHbox.getChildren().add(ejectedTapeIcon);
            } else {
                final ImageView storageTapeIcon = new ImageView();
                storageTapeIcon.setImage(STORAGE_TAPES);
                storageTapeIcon.setFitHeight(15);
                storageTapeIcon.setFitWidth(15);
                final int storageTapeCount = objects.getPhysicalPlacement().getTapes().size();
                Tooltip.install(storageTapeIcon, new Tooltip(Integer.toString(storageTapeCount - ejectedTapeCount.size()) + " copy on Storage Tape"));
                placementIconTooltipHbox.getChildren().add(storageTapeIcon);
            }
        }

        if (objects.getPhysicalPlacement().getPools() != null) {
            objects.getPhysicalPlacement().getPools().forEach(pool -> {
                if (pool.getType() == PoolType.NEARLINE) {
                    nearLineDiskCount.add(1);
                }
            });
            if (nearLineDiskCount.size() != 0) {
                final ImageView nearlineDiskIcon = new ImageView();
                nearlineDiskIcon.setImage(NEARLINE_DISK);
                nearlineDiskIcon.setFitHeight(15);
                nearlineDiskIcon.setFitWidth(15);
                Tooltip.install(nearlineDiskIcon, new Tooltip(Integer.toString(nearLineDiskCount.size()) + " copy on Nearline Disk"));
                placementIconTooltipHbox.getChildren().add(nearlineDiskIcon);
            } else {
                final ImageView onlineDiskIcon = new ImageView();
                onlineDiskIcon.setImage(ONLINE_DISK);
                onlineDiskIcon.setFitHeight(15);
                onlineDiskIcon.setFitWidth(15);
                onlineDiskCount = objects.getPhysicalPlacement().getPools().size();
                Tooltip.install(onlineDiskIcon, new Tooltip(Integer.toString(onlineDiskCount - nearLineDiskCount.size()) + " copy on Online Disk"));
                placementIconTooltipHbox.getChildren().add(onlineDiskIcon);
            }
        }

        if (objects.getInCache()) {
            final ImageView blackPearlCacheIcon = new ImageView();
            blackPearlCacheIcon.setImage(BLACK_PEARL_CACHE);
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

        return placementIconTooltipHbox;
    }

}

