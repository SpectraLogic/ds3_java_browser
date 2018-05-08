package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.FileTreeTableProvider;

import javax.inject.Inject;

public class FileTreeTableItemFactory {

    private final LoggingService loggingService;
    private final Workers workers;
    private final FileTreeTableProvider fileTreeTableProvider;

    @Inject
    public FileTreeTableItemFactory(
            final LoggingService loggingService,
            final Workers workers,
            final FileTreeTableProvider fileTreeTableProvider
    ) {
        this.loggingService = loggingService;
        this.workers = workers;
        this.fileTreeTableProvider = fileTreeTableProvider;
    }

    public FileTreeTableItem create(FileTreeModel fileTreeModel) {
        return new FileTreeTableItem(fileTreeTableProvider, fileTreeModel, workers, loggingService);
    }
}
