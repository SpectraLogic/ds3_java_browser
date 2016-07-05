package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTreeTableItem extends TreeItem<FileTreeModel> {

    private final static Logger LOG = LoggerFactory.getLogger(FileTreeTableItem.class);

    private final LocalFileTreeTableProvider provider;
    private final boolean leaf;
    private final FileTreeModel fileTreeModel;

    private boolean accessedChildren = false;
    private boolean error = false;

    public FileTreeTableItem(final LocalFileTreeTableProvider provider, final FileTreeModel fileTreeModel) {
        super(fileTreeModel);
        this.fileTreeModel = fileTreeModel;
        this.leaf = isLeaf(fileTreeModel.getPath());
        this.provider = provider;

        this.setGraphic(getGraphicType(fileTreeModel)); // sets the default icon

        if (fileTreeModel.getType() == FileTreeModel.Type.Directory) {
            this.addEventHandler(TreeItem.branchExpandedEvent(), e -> {
                if (!error) {
                    e.getSource().setGraphic(Icon.getIcon(FontAwesomeIcon.FOLDER_OPEN));
                }
            });

            this.addEventHandler(TreeItem.branchCollapsedEvent(), e -> {
                if (!error) {
                    e.getSource().setGraphic(Icon.getIcon(FontAwesomeIcon.FOLDER));
                }
            });
        }
    }

    private FontAwesomeIconView getGraphicType(final FileTreeModel fileTreeModel) {
        switch (fileTreeModel.getType()) {
            case Media_Device:
               return Icon.getIcon(FontAwesomeIcon.HDD_ALT);
            case Directory:
                return Icon.getIcon(FontAwesomeIcon.FOLDER);
            default:
                return null;
        }
    }

    @Override
    public ObservableList<TreeItem<FileTreeModel>> getChildren() {
        if (!accessedChildren) {
            accessedChildren = true;
            final ObservableList<TreeItem<FileTreeModel>> children = super.getChildren();
            buildChildren(children);
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        return leaf;
    }

    private void buildChildren(final ObservableList<TreeItem<FileTreeModel>> children) {
        if (fileTreeModel == null) {
            return;
        }

        final Path path = fileTreeModel.getPath();

        if (path != null && !isLeaf(path)) {
            try {
                final ImmutableList<FileTreeTableItem> fileChildren = provider
                        .getListForDir(fileTreeModel)
                        .map(ftm -> new FileTreeTableItem(provider, ftm))
                        .collect(GuavaCollectors.immutableList());

                children.setAll(fileChildren);
            } catch (final AccessDeniedException ae) {
                LOG.error("Could not access file", ae);
                setError("Invalid permissions");
            }
            catch (final IOException e) {
                LOG.error("Failed to get children for " + path.toString(), e);
                setError("Failed to get children");
            }
        }
    }

    private void setError(final String errorMessage) {
        this.error = true;
        final Node node = Icon.getIcon(FontAwesomeIcon.EXCLAMATION_CIRCLE, Paint.valueOf("RED"));
        final Tooltip errorTip = new Tooltip(errorMessage);
        Tooltip.install(node, errorTip);
        this.setGraphic(node);
    }

    private static boolean isLeaf(final Path path) {
        return !Files.isDirectory(path);
    }
}
