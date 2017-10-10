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

package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.dsbrowser.gui.components.ds3panel.FilesCountModel;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.util.BaseTreeModel;
import com.spectralogic.dsbrowser.gui.util.treeItem.TreeItemUtil;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

public class GetNumberOfItemsTask extends Task<FilesCountModel> {

    private final static Logger LOG = LoggerFactory.getLogger(GetNumberOfItemsTask.class);

    private final Ds3Client ds3Client;
    private final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems;

    public GetNumberOfItemsTask(final Ds3Client ds3Client,
                                final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems) {
        this.ds3Client = ds3Client;
        this.selectedItems = selectedItems;
    }

    @Override
    protected FilesCountModel call() throws Exception {

        final List<TreeItem<Ds3TreeTableValue>> discreetPaths = TreeItemUtil.minimumPaths(selectedItems);
        final Set<String> foldersSet = new HashSet<>();

        final FilesCountModel filesCountModel = discreetPaths.stream()
                .filter(item -> item.getValue().getType() == BaseTreeModel.Type.Directory)
                .flatMap(folder -> {
                    final String directoryName = folder.getValue().getDirectoryName();
                    foldersSet.add(directoryName);
                    final String bucket = folder.getValue().getBucketName();
                    try {
                        return StreamSupport.stream(Ds3ClientHelpers.wrap(ds3Client)
                                .listObjects(bucket, directoryName).spliterator(), false);
                    } catch (final IOException ioe) {
                        LOG.error("Failed to get bucket" + bucket, ioe);
                    }
                    return null;
                })
                .map(contents -> {
                    final String contentsKey = contents.getKey();
                    final int index = contentsKey.lastIndexOf("/");

                    if (index == contentsKey.length() - 1) {
                        foldersSet.add(contentsKey);
                        return new FilesCountModel();
                    } else {
                        final String path = contentsKey.substring(0, index + 1);
                        LOG.info("path[{}]", path);
                        foldersSet.add(path);
                        return new FilesCountModel(0, 1, contents.getSize());
                    }
                })
                .reduce(new FilesCountModel(), (x, y) ->
                    new FilesCountModel(
                        x.getNumberOfFolders() + y.getNumberOfFolders(),
                        x.getNumberOfFiles() + y.getNumberOfFiles(),
                        x.getTotalCapacity() + y.getTotalCapacity())
                );

        return new FilesCountModel(ensureZeroOrGreater(foldersSet.size() - 1), filesCountModel.getNumberOfFiles(), filesCountModel.getTotalCapacity());
    }

    private long ensureZeroOrGreater(final long value) {
        if (value < 0) {
            return 0;
        }
        return value;
    }
}

