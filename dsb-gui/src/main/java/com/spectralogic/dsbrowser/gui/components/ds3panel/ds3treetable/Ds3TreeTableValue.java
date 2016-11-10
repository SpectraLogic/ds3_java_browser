package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import javafx.scene.layout.HBox;

import java.io.Serializable;

public class Ds3TreeTableValue implements Serializable {

    private final String bucketName;
    private final String name;
    private final String fullName;
    private final Type type;
    private final long size;
    private final String lastModified;
    private final HBox physicalPlacementHBox;
    private final String owner;
    private final boolean searchOn;
    private  String marker = "";

    public Ds3TreeTableValue(final String bucketName, final String name, final Type type, final long size, final String lastModified, final String owner, final boolean searchOn, final HBox physicalPlacementHBox) {
        this.bucketName = bucketName;
        this.fullName = name;
        this.name = getLastPart(name, type);
        this.type = type;
        this.size = size;
        this.lastModified = lastModified;
        this.physicalPlacementHBox = physicalPlacementHBox;
        this.owner = owner;
        this.searchOn = searchOn;
    }

    //constructor with marker
    public Ds3TreeTableValue(final String bucketName, final String name, final Type type, final long size, final String lastModified, final String owner, final boolean searchOn, final HBox physicalPlacementHBox,String marker) {
        this.bucketName = bucketName;
        this.fullName = name;
        this.name = getLastPart(name, type);
        this.type = type;
        this.size = size;
        this.lastModified = lastModified;
        this.physicalPlacementHBox = physicalPlacementHBox;
        this.owner = owner;
        this.searchOn = searchOn;
        this.marker = marker;
    }

    public boolean isSearchOn() {
        return searchOn;
    }

    public HBox getPhysicalPlacementHBox() {
        return physicalPlacementHBox;
    }

    private static String getLastPart(final String name, final Type type) {
        if (type == Type.Bucket) {
            return name;
        } else if (type == Type.Directory) {
            final String strippedName = name.substring(0, name.length() - 1);
            final int index = strippedName.lastIndexOf('/');
            return strippedName.substring(index + 1);
        }
        final int index = name.lastIndexOf('/');
        return name.substring(index + 1);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public long getSize() {
        return size;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getFullName() {
        return fullName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public String getDirectoryName() {
        switch (type) {
            case Directory:
                return getFullName();
            case Bucket:
                return "";
            default:
                return getParentDir(this.getFullName());
        }
    }

    private String getParentDir(final String fullName) {
        final int index = fullName.lastIndexOf('/');
        if (index < 0) {
            return "";
        }
        return fullName.substring(0, index);
    }

    public String getOwner() {
        return owner;
    }

    public enum Type {
        File, Directory, Bucket , Loader
    }
}
