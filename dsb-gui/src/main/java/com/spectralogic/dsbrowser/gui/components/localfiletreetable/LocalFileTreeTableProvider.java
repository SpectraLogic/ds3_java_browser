package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.spectralogic.dsbrowser.util.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class LocalFileTreeTableProvider {
    private final static Logger LOG = LoggerFactory.getLogger(LocalFileTreeTableProvider.class);

    public Stream<FileTreeModel> getRoot() {
        return Arrays.stream(File.listRoots()).map(file -> {
            final FileTreeModel.Type type = getRootType(file);
            final Path path = file.toPath();
            long size = 0;
            try {
                size = Files.size(path);
            } catch (final IOException e) {
                LOG.error("Failed to get the size of " + path.toString(), e);
            }
            return new FileTreeModel(file.toPath(), type, size, -1);
        }).filter(p -> p != null);
    }

    private FileTreeModel.Type getRootType(final File file) {
        if (Platform.isWin()) {
            return FileTreeModel.Type.MEDIA_DEVICE;
        } else if (file.isFile()) {
            return FileTreeModel.Type.FILE;
        } else {
            return FileTreeModel.Type.DIRECTORY;
        }
    }

    public Stream<FileTreeModel> getListForDir(final FileTreeModel fileTreeModel) throws IOException {
        final int newDepth = fileTreeModel.getDepth() + 1;
        return Files.list(fileTreeModel.getPath()).map(filePath -> {
            try {
                final long size = Files.size(filePath);
                final FileTreeModel.Type type = getPathType(filePath);
                return new FileTreeModel(filePath, type, size, newDepth);
            } catch (final IOException e) {
                LOG.error("Failed to get file size for: " + filePath.toString(), e);
                return new FileTreeModel(filePath, FileTreeModel.Type.ERROR, 0, newDepth);
            }
        });
    }

    private static FileTreeModel.Type getPathType(final Path path) {
        if (Files.isDirectory(path)) {
            return FileTreeModel.Type.DIRECTORY;
        } else {
            return FileTreeModel.Type.FILE;
        }
    }
}
