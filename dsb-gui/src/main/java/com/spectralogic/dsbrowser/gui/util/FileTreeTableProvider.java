/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.util.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

@Singleton
public class FileTreeTableProvider {

    private final static Logger LOG = LoggerFactory.getLogger(FileTreeTableProvider.class);

    private final DateTimeUtils dateTimeUtils;

    @Inject
    public FileTreeTableProvider(final DateTimeUtils dateTimeUtils) {
        this.dateTimeUtils = dateTimeUtils;
    }

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
        return getDirectChildren(files, rootDir);
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
                final String lastModified = dateTimeUtils.formatDate(fileModifiedTime.toMillis());
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

    private Stream<FileTreeModel> getDirectChildren(final File[] files, final String rootDir) {
        return Arrays.stream(files).map(file -> {
            final FileTreeModel.Type type = getRootType(file);
            final Path path = file.toPath();
            long size = 0;
            String lastModified = StringConstants.EMPTY_STRING;
            try {
                if ((type != FileTreeModel.Type.Media_Device) && (type != FileTreeModel.Type.Directory)) {
                    size = Files.size(path);
                }
                final FileTime modifiedTime = Files.getLastModifiedTime(path);
                lastModified = dateTimeUtils.formatDate(modifiedTime.toMillis());
            } catch (final IOException e) {
                LOG.error("Failed to get the size of " + path.toString(), e);
            }
            if (rootDir.equals(StringConstants.ROOT_LOCATION)) {
                return new FileTreeModel(file.toPath(), type, size, -1, lastModified);
            }
            return new FileTreeModel(file.toPath(), type, size, Paths.get(rootDir).getNameCount(), lastModified);
        }).filter(Objects::nonNull);
    }
}
