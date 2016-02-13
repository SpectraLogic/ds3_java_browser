package com.spectralogic.dsbrowser.api;

public class FileTreeModel {
    final private String name;
    final private Type type;
    final private long size;

    public FileTreeModel(final String name, final Type type, final long size) {
        this.name = name;
        this.type = type;
        this.size = size;
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

    public static enum Type {
        FILE, DIRECTORY, MEDIA_DEVICE, FILE_SYSTEM, ERROR
    }
}
