/*
 * ******************************************************************************
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
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.spectralogic.dsbrowser.gui.util.BaseTreeModel;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.nio.file.Path;

public class FileTreeModel extends BaseTreeModel {
    final private Path path;
    final private long size;
    final private int depth;
    private final String lastModified;

    public FileTreeModel(final Path path, final Type type, final long size, final int depth, final String lastModified) {
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

    public long getSize() {
        return size;
    }

    public int getDepth() {
        return depth;
    }

    public String getLastModified() {
        return lastModified;
    }

    private String getNamePart(final Path path, final int depth) {
        if (depth < 0) {
            //get volume name from volume letter
            final FileSystemView fileSystemView = FileSystemView.getFileSystemView();
            if (!fileSystemView.getSystemDisplayName(new File(path.toString())).isEmpty()) {
                return fileSystemView.getSystemDisplayName(new File(path.toString()));
            }
            else {
                return path.toString();
            }
        } else {
            return path.getName(depth).toString();
        }
    }

}
