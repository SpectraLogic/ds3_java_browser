package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeTableItem;
import com.spectralogic.dsbrowser.gui.util.FileTreeTableProvider;
import com.spectralogic.dsbrowser.gui.services.Workers;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;

import java.util.stream.Stream;

public class GetMediaDeviceTask extends Task{
    private final Stream<FileTreeModel> rootItems;
    private final TreeItem<FileTreeModel> rootTreeItem;
    private final FileTreeTableProvider provider;
    private final Workers workers;

    public GetMediaDeviceTask(final Stream<FileTreeModel> rootItems, final TreeItem<FileTreeModel> rootTreeItem, final FileTreeTableProvider provider, final Workers workers) {
        this.rootItems = rootItems;
        this.rootTreeItem = rootTreeItem;
        this.provider = provider;
        this.workers = workers;
    }

    @Override
    protected Object call() throws Exception {
        rootItems.forEach(ftm ->
                {
                    final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, ftm, workers);
                    rootTreeItem.getChildren().add(newRootTreeItem);
                }
        );
        return null;
    }
}
