package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.util.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.stream.Stream;

public class FileTreeTableProvider {

    private final static Logger LOG = LoggerFactory.getLogger(FileTreeTableProvider.class);

    public Stream<FileTreeModel> getRoot(final String rootDir) {
        final File[] files;
        if (rootDir.equals(StringConstants.ROOT_LOCATION)) {
            files = File.listRoots();

        } else {
            files = new File(rootDir).listFiles();
        }
        if (files == null) {
            return null;
        }
        return getDirectChildren(files , rootDir);
    }

    private FileTreeModel.Type getRootType(final File file) {
        if (Platform.isWin()) {
            if (file.isDirectory()) {
                return FileTreeModel.Type.Directory;
            } else if (file.isFile()) {
                return FileTreeModel.Type.File;
            } else {
                return FileTreeModel.Type.Media_Device;
            }
        } else if (file.isFile()) {
            return FileTreeModel.Type.File;
        } else {
            return FileTreeModel.Type.Directory;
        }
    }

    public Stream<FileTreeModel> getListForDir(final FileTreeModel fileTreeModel) throws IOException {
        LOG.info("Get Childern of a Directory {}", fileTreeModel.getPath());
        final int newDepth = fileTreeModel.getDepth() + 1;
        return Files.list(fileTreeModel.getPath()).map(filePath -> {
            try {
                final FileTreeModel.Type type = getRootType(filePath.toFile());
                final FileTime fileModifiedTime = Files.getLastModifiedTime(filePath);
                final String lastModified = DateFormat.formatDate(fileModifiedTime.toMillis());
                long size = 0;
                if (type != FileTreeModel.Type.Directory) {
                    size = Files.size(filePath);
                }
                return new FileTreeModel(filePath, type, size, newDepth, lastModified);
            } catch (final IOException e) {
                LOG.error("Failed to get file size for: " + filePath.toString(), e);
                return new FileTreeModel(filePath, FileTreeModel.Type.Error, 0, newDepth, "");
            }
        });
    }

    private Stream<FileTreeModel> getDirectChildren(final File[] files , final String rootDir) {
         return Arrays.stream(files).map(file -> {
            final FileTreeModel.Type type = getRootType(file);
            final Path path = file.toPath();
            long size = 0;
            String lastModified = StringConstants.EMPTY_STRING;
            try {
                if ((type == FileTreeModel.Type.Media_Device) || (type == FileTreeModel.Type.Directory)) {
                    size = 0;
                } else {
                    size = Files.size(path);
                }
                final FileTime modifiedTime = Files.getLastModifiedTime(path);
                lastModified = DateFormat.formatDate(modifiedTime.toMillis());
            } catch (final IOException e) {
                LOG.error("Failed to get the size of " + path.toString(), e);
            }
            if (rootDir.equals(StringConstants.ROOT_LOCATION)) {
                return new FileTreeModel(file.toPath(), type, size, -1, lastModified);
            }
            return new FileTreeModel(file.toPath(), type, size, Paths.get(rootDir).getNameCount(), lastModified);
        }).filter(p -> p != null);
    }
}