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

package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.inject.assistedinject.Assisted;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeTableItem;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.FileTreeTableProvider;
import com.spectralogic.dsbrowser.gui.services.Workers;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;

import javax.inject.Inject;
import java.util.Optional;
import java.util.stream.Stream;

public class GetMediaDeviceTask extends Task{
    private final Stream<FileTreeModel> rootItems;
    private final TreeItem<FileTreeModel> rootTreeItem;
    private final FileTreeTableProvider provider;
    private final Workers workers;
    private final LoggingService loggingService;

    @Inject
    public GetMediaDeviceTask(
            @Assisted final Stream<FileTreeModel> rootItems,
            @Assisted final TreeItem<FileTreeModel> rootTreeItem,
            final FileTreeTableProvider provider,
            final Workers workers,
            final LoggingService loggingService) {
        this.rootItems = rootItems;
        this.rootTreeItem = rootTreeItem;
        this.provider = provider;
        this.workers = workers;
        this.loggingService = loggingService;
    }

    @Override
    protected Optional<Object> call() throws Exception {
        rootItems.forEach(ftm ->
                {
                    final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, ftm, workers, loggingService);
                    rootTreeItem.getChildren().add(newRootTreeItem);
                }
        );
        return Optional.empty();
    }

    public interface GetMediaDeviceTaskFactory {
        public GetMediaDeviceTask create(
                final Stream<FileTreeModel> rootItems,
                final TreeItem<FileTreeModel> rootTreeItem
        );
    }
}
