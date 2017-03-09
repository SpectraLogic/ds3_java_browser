/*
 * ****************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.util.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.stream.Stream;

public final class LocalFileTreeTableProvider {

    private final static Logger LOG = LoggerFactory.getLogger(LocalFileTreeTableProvider.class);

    private static FileTreeModel.Type getPathType(final Path path) {
        if (Files.isDirectory(path)) {
            return FileTreeModel.Type.Directory;
        } else {
            return FileTreeModel.Type.File;
        }
    }

    public static Stream<FileTreeModel> getRoot(final String rootDir) {
        File[] files = null;

        if (rootDir.equals("My Computer")) {
            files = File.listRoots();
        } else {
            files = new File(rootDir).listFiles();
        }

        if (files == null)
            return null;

        return Arrays.stream(files).map(file -> {
            final FileTreeModel.Type type = getRootType(file);
            final Path path = file.toPath();
            long size = 0;
            String lastModified = "";
            try {
                if ((type == FileTreeModel.Type.Media_Device) || (type == FileTreeModel.Type.Directory)) {
                    size = 0;
                } else {
                    size = Files.size(path);
                }
                final FileTime modifiedTime = Files.getLastModifiedTime(path);
                final SimpleDateFormat sdf = new SimpleDateFormat("M/dd/yyyy HH:mm:ss");
                lastModified = sdf.format(modifiedTime.toMillis());
            } catch (final IOException e) {
                LOG.error("Failed to get the size of " + path.toString(), e);
            }
            if (rootDir.equals("My Computer")) {
                return new FileTreeModel(file.toPath(), type, size, -1, lastModified);
            }
            return new FileTreeModel(file.toPath(), type, size, Paths.get(rootDir).getNameCount(), lastModified);
        }).filter(p -> p != null);
    }

    private static FileTreeModel.Type getRootType(final File file) {
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

    public static Stream<FileTreeModel> getListForDir(final FileTreeModel fileTreeModel) throws IOException {
        final int newDepth = fileTreeModel.getDepth() + 1;
        return Files.list(fileTreeModel.getPath()).map(filePath -> {
            try {
                final FileTreeModel.Type type = getPathType(filePath);
                if (type == FileTreeModel.Type.Directory) {
                    final String size = FileSizeFormat.getFileSizeType(0);
                    final FileTime fileModifiedTime = Files.getLastModifiedTime(filePath);
                    final SimpleDateFormat sdf = new SimpleDateFormat("M/dd/yyyy HH:mm:ss");
                    final String lastModified = sdf.format(fileModifiedTime.toMillis());
                    return new FileTreeModel(filePath, type, 0, newDepth, lastModified);
                } else {
                    final String size = FileSizeFormat.getFileSizeType(Files.size(filePath));
                    final FileTime fileModifiedTime = Files.getLastModifiedTime(filePath);
                    final SimpleDateFormat sdf = new SimpleDateFormat("M/dd/yyyy HH:mm:ss");
                    final String lastModified = sdf.format(fileModifiedTime.toMillis());
                    return new FileTreeModel(filePath, type, Files.size(filePath), newDepth, lastModified);
                }
            } catch (final IOException e) {
                LOG.error("Failed to get file size for: " + filePath.toString(), e);
                return new FileTreeModel(filePath, FileTreeModel.Type.Error, 0, newDepth, "");
            }
        });
    }
}
