package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import java.io.Serializable;

public class Ds3TreeTableValue implements Serializable {

    private final String bucketName;
    private final String name;
    private final String fullName;
    private final Type type;
    private final String size;
    private final String lastModified;


    public Ds3TreeTableValue(final String bucketName, final String name, final Type type, final String size, final String lastModified) {
        this.bucketName = bucketName;
        this.fullName = name;
        this.name = getLastPart(name, type);
        this.type = type;
        this.size = size;
        this.lastModified = lastModified;

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

    public String getSize() {
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

    public enum Type {
        File, Directory, Bucket
    }
}
