package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTreeTableItem extends TreeItem<FileTreeModel> {

    private final static Logger LOG = LoggerFactory.getLogger(FileTreeTableItem.class);

    private final LocalFileTreeTableProvider provider;
    private final boolean leaf;
    private final FileTreeModel fileTreeModel;

    private boolean accessedChildren = false;

    public FileTreeTableItem(final LocalFileTreeTableProvider provider, final FileTreeModel fileTreeModel) {
        super(fileTreeModel);
        this.fileTreeModel = fileTreeModel;
        this.leaf = isLeaf(fileTreeModel.getPath());
        this.provider = provider;

        this.setGraphic(getGraphicType(fileTreeModel));
    }

    private FontAwesomeIconView getGraphicType(final FileTreeModel fileTreeModel) {
        switch (fileTreeModel.getType()) {
           case MEDIA_DEVICE:
               return Icon.getIcon(FontAwesomeIcon.HDD_ALT);
            case DIRECTORY:
                return Icon.getIcon(FontAwesomeIcon.FOLDER_ALT);
            default:
                return null;
        }
    }

    @Override
    public ObservableList<TreeItem<FileTreeModel>> getChildren() {
        if (!accessedChildren) {
            super.getChildren().setAll(buildChildren(provider, this.fileTreeModel));
            accessedChildren = true;
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        return leaf;
    }

    private static ObservableList<TreeItem<FileTreeModel>> buildChildren(final LocalFileTreeTableProvider provider, final FileTreeModel fileTreeModel) {
        if (fileTreeModel == null) {
            return FXCollections.emptyObservableList();
        }

        final Path path = fileTreeModel.getPath();

        if (path != null && !isLeaf(path)) {
            try {
                final ImmutableList<FileTreeTableItem> children = provider
                        .getListForDir(fileTreeModel)
                        .map(ftm -> new FileTreeTableItem(provider, ftm))
                        .collect(GuavaCollectors.immutableList());

                return FXCollections.observableArrayList(children);
            } catch (final IOException e) {
                LOG.error("Failed to get children for " + path.toString(), e);
            }
        }

        return FXCollections.emptyObservableList();
    }

    private static boolean isLeaf(final Path path) {
        return !Files.isDirectory(path);
    }
}
