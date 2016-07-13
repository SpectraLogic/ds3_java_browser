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
                size = FileSizeFormat.getFileSizeType(Files.size(path));
                FileTime modifiedTime = Files.getLastModifiedTime(path);
                final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                lastModified = sdf.format(modifiedTime.toMillis());
            } catch (final IOException e) {
                LOG.error("Failed to get the size of " + path.toString(), e);
            }
            return new FileTreeModel(file.toPath(), type, size, -1, lastModified);
        }).filter(p -> p != null);
    }

    private FileTreeModel.Type getRootType(final File file) {
        if (Platform.isWin()) {
            return FileTreeModel.Type.Media_Device;
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
                final String size = FileSizeFormat.getFileSizeType(Files.size(filePath));
                final FileTreeModel.Type type = getPathType(filePath);
                final FileTime fileModifiedTime = Files.getLastModifiedTime(filePath);
                final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                final String lastModified = sdf.format(fileModifiedTime.toMillis());
                return new FileTreeModel(filePath, type, size, newDepth, lastModified);
            } catch (final IOException e) {
                LOG.error("Failed to get file size for: " + filePath.toString(), e);
                return new FileTreeModel(filePath, FileTreeModel.Type.Error, "", newDepth, "");
            }
        });
    }

    private static FileTreeModel.Type getPathType(final Path path) {
        if (Files.isDirectory(path)) {
            return FileTreeModel.Type.Directory;
        } else {
            return FileTreeModel.Type.File;
        }
    }

}
