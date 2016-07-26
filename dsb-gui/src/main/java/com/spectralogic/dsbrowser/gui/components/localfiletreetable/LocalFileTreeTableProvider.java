package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.util.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.stream.Stream;

public class LocalFileTreeTableProvider {
    private final static Logger LOG = LoggerFactory.getLogger(LocalFileTreeTableProvider.class);

    private static FileTreeModel.Type getPathType(final Path path) {
        if (Files.isDirectory(path)) {
            return FileTreeModel.Type.Directory;
        } else {
            return FileTreeModel.Type.File;
        }
    }

    public Stream<FileTreeModel> getRoot(final String rootDir) {
        File[] files = null;

        if (rootDir == System.getProperty("user.home")) {
            files = new File(System.getProperty("user.home")).listFiles();

        } else {
            files = File.listRoots();
        }
        return Arrays.stream(files).map(file -> {
            final FileTreeModel.Type type = getRootType(file);
            final Path path = file.toPath();
            String size = "";
            String lastModified = "";
            try {
                if ((type == FileTreeModel.Type.Media_Device) || (type == FileTreeModel.Type.Directory)) {
                    size = FileSizeFormat.getFileSizeType(0);
                } else {
                    size = FileSizeFormat.getFileSizeType(Files.size(path));
                }
                final FileTime modifiedTime = Files.getLastModifiedTime(path);
                final SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy HH:mm:ss");
                lastModified = sdf.format(modifiedTime.toMillis());
            } catch (final IOException e) {
                LOG.error("Failed to get the size of " + path.toString(), e);
            }
            if (rootDir == System.getProperty("user.home")) {
                return new FileTreeModel(file.toPath(), type, size, 2, lastModified);
            }
            return new FileTreeModel(file.toPath(), type, size, -1, lastModified);
        }).filter(p -> p != null);
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
        final int newDepth = fileTreeModel.getDepth() + 1;
        return Files.list(fileTreeModel.getPath()).map(filePath -> {
            try {
                final FileTreeModel.Type type = getPathType(filePath);
                if (type == FileTreeModel.Type.Directory) {
                    final String size = FileSizeFormat.getFileSizeType(0);
                    final FileTime fileModifiedTime = Files.getLastModifiedTime(filePath);
                    final SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy HH:mm:ss");
                    final String lastModified = sdf.format(fileModifiedTime.toMillis());
                    return new FileTreeModel(filePath, type, size, newDepth, lastModified);
                } else {
                    final String size = FileSizeFormat.getFileSizeType(Files.size(filePath));
                    final FileTime fileModifiedTime = Files.getLastModifiedTime(filePath);
                    final SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy HH:mm:ss");
                    final String lastModified = sdf.format(fileModifiedTime.toMillis());
                    return new FileTreeModel(filePath, type, size, newDepth, lastModified);
                }
            } catch (final IOException e) {
                LOG.error("Failed to get file size for: " + filePath.toString(), e);
                return new FileTreeModel(filePath, FileTreeModel.Type.Error, "", newDepth, "");
            }
        });
    }

}
