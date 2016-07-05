package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import java.nio.file.Path;

public class FileTreeModel {
    final private Path path;
    final private String name;
    final private Type type;
    final private String size;
    final private int depth;
    private final String lastModified;

    public FileTreeModel(final Path path, final Type type, final String size, final int depth, final String lastModified) {
        this.path = path;
        this.type = type;
        this.size = size;
        this.depth = depth;
        this.lastModified = lastModified;
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

    public String getSize() {
        return size;
    }

    public int getDepth() {
        return depth;
    }

    public String getLastModified() {
		return lastModified;
	}

	public String getNamePart(final Path path, final int depth) {
        if (depth < 0) {
            return path.toString();
        } else {
            return path.getName(depth).toString();
        }
    }

    public static enum Type {
        File, Directory, Media_Device, File_System, Error
    }
}
