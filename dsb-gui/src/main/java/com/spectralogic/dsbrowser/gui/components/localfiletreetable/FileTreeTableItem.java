package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import javax.swing.filechooser.FileSystemView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import com.spectralogic.dsbrowser.util.Icon;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Paint;

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
//
//        if (fileTreeModel.getType() == FileTreeModel.Type.Directory) {
//            this.addEventHandler(TreeItem.branchExpandedEvent(), e -> {
//                if (!error) {
//                    e.getSource().setGraphic(Icon.getIcon(FontAwesomeIcon.FOLDER_OPEN));
//                }
//            });
//
//            this.addEventHandler(TreeItem.branchCollapsedEvent(), e -> {
//                if (!error) {
//                    e.getSource().setGraphic(Icon.getIcon(FontAwesomeIcon.FOLDER));
//                }
//            });
//        }
    }
    
    private ImageView getGraphicType(final FileTreeModel fileTreeModel) {
        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        javax.swing.Icon icon = fileSystemView.getSystemIcon(new File(fileTreeModel.getPath().toString()));
        BufferedImage bufferedImage = new BufferedImage(
                icon.getIconWidth(),
                icon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        icon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);

        ImageView imageView = new ImageView(SwingFXUtils.toFXImage(bufferedImage,null));
        return   imageView;

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
