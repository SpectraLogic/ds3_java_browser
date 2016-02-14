package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import java.nio.file.Path;

public class FileTreeModel {
    final private Path path;
    final private String name;
    final private Type type;
    final private long size;
    final private int depth;

    public FileTreeModel(final Path path, final Type type, final long size, final int depth) {
        this.path = path;
        this.type = type;
        this.size = size;
        this.depth = depth;
        this.name = getNamePart(path, depth);
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public Type getType() {
        return type;
    }

    public long getSize() {
        return size;
    }

    public int getDepth() {
        return depth;
    }

    public String getNamePart(final Path path, final int depth) {
        if (depth < 0) {
            return path.toString();
        } else {
            return path.getName(depth).toString();
        }
    }

    public static enum Type {
        FILE, DIRECTORY, MEDIA_DEVICE, FILE_SYSTEM, ERROR
    }
}
