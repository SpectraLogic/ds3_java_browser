package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

public class Ds3TreeTableValue {

    private final String name;
    private final String fullName;
    private final Type type;
    private final long size;
    private final String lastModified;

    public Ds3TreeTableValue(final String name, final Type type, final long size, final String lastModified) {
        this.fullName = name;
        this.name = getLastPart(name, type);
        this.type = type;
        this.size = size;
        this.lastModified = lastModified;
    }

    private static String getLastPart(final String name, final Type type) {
        if (type == Type.BUCKET) {
            return name;
        } else if (type == Type.DIRECTORY) {
            return name.substring(0, name.length() - 1);
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


    public enum Type {
        FILE, DIRECTORY, BUCKET
    }
}
