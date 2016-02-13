package com.spectralogic.dsbrowser.local;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.api.FileTreeModel;
import com.spectralogic.dsbrowser.api.FileTreeTableProvider;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalFileTreeTableProvider implements FileTreeTableProvider {
    private final static Logger LOG = LoggerFactory.getLogger(LocalFileTreeTableProvider.class);

    @Override
    public ImmutableList<FileTreeModel> getRoot() {
        final File[] rootFiles = File.listRoots();
        final ImmutableList.Builder<FileTreeModel> builder = ImmutableList.builder();
        for (final File file : rootFiles) {
            final FileTreeModel.Type type;
            if (file.isFile()) {
                type = FileTreeModel.Type.FILE;
            } else {
                type = FileTreeModel.Type.DIRECTORY;
            }
            builder.add(new FileTreeModel(file.getAbsolutePath(), type, file.getTotalSpace()));
        }
        return builder.build();
    }

    @Override
    public ImmutableList<FileTreeModel> getListForDir(final String dirName) throws IOException {
        final Path path = Paths.get(dirName);

        return Files.list(path).map(filePath -> {
            try {
                final long size = Files.size(filePath);
                final FileTreeModel.Type type = getPathType(filePath);
                return new FileTreeModel(filePath.toString(), type, size);
            } catch (final IOException e) {
                LOG.error("Failed to get file size for: " + filePath.toString(), e);
                return new FileTreeModel(filePath.toString(), FileTreeModel.Type.ERROR, 0);
            }

        }).collect(GuavaCollectors.immutableList());
    }

    private static FileTreeModel.Type getPathType(final Path path) {
        if (Files.isDirectory(path)) {
            return FileTreeModel.Type.DIRECTORY;
        } else {
            return FileTreeModel.Type.FILE;
        }
    }
}
