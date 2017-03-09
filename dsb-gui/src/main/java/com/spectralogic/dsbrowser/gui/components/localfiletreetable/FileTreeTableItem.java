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

import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Paint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.spectralogic.dsbrowser.gui.components.localfiletreetable.LocalFileTreeTableProvider.getListForDir;


public class FileTreeTableItem extends TreeItem<FileTreeModel> {

    private final static Logger LOG = LoggerFactory.getLogger(FileTreeTableItem.class);

    private final boolean leaf;
    private final FileTreeModel fileTreeModel;
    private boolean accessedChildren = false;
    private final Workers workers;

    public FileTreeTableItem(final FileTreeModel fileTreeModel, final Workers workers) {
        super(fileTreeModel);
        this.fileTreeModel = fileTreeModel;
        this.leaf = isLeaf(fileTreeModel.getPath());
        this.setGraphic(getGraphicType(fileTreeModel)); // sets the default icon
        this.workers = workers;
    }

    private static boolean isLeaf(final Path path) {
        return !Files.isDirectory(path);
    }

    private Node getGraphicType(final FileTreeModel fileTreeModel) {
        final FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        try {
            final javax.swing.Icon icon = fileSystemView.getSystemIcon(new File(fileTreeModel.getPath().toString()));

            final BufferedImage bufferedImage = new BufferedImage(
                    16,
                    16,
                    BufferedImage.TYPE_INT_ARGB
            );
            icon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
            return new ImageView(SwingFXUtils.toFXImage(bufferedImage, null));
        } catch (final Exception e) {
            return getGraphicFont(fileTreeModel);
        }

    }

    private FontAwesomeIconView getGraphicFont(final FileTreeModel fileTreeModel) {
        switch (fileTreeModel.getType()) {
            case File_System:
                return Icon.getIcon(FontAwesomeIcon.FILE);
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
            final Node previousGraphics = super.getGraphic();

            final ImageView processImage = new ImageView("/images/loading.gif");
            processImage.setFitHeight(20);
            processImage.setFitWidth(20);
            super.setGraphic(processImage);
            final Task buildChildren = new Task() {
                @Override
                protected Object call() throws Exception {
                    buildChildren(children);
                    return null;
                }
            };
            workers.execute(buildChildren);
            buildChildren.setOnSucceeded(event -> {
                LOG.info("Success");
                super.setGraphic(previousGraphics);
            });
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        return leaf;
    }

    public void refresh() {
        final ObservableList<TreeItem<FileTreeModel>> list = super.getChildren();
        list.remove(0, list.size());
        buildChildren(list);
    }

    private void buildChildren(final ObservableList<TreeItem<FileTreeModel>> children) {
        if (fileTreeModel == null) {
            return;
        }
        final Path path = fileTreeModel.getPath();
        if (path != null && !isLeaf(path)) try {
            final List<FileTreeTableItem> fileChildren = getListForDir(fileTreeModel)
                    .map(ftm -> new FileTreeTableItem(ftm, workers))
                    .collect(Collectors.toList());
            fileChildren.sort(Comparator.comparing(t -> t.getValue().getType().toString()
            ));

            children.setAll(fileChildren);


        } catch (final AccessDeniedException ae) {
            LOG.error("Could not access file", ae);
            setError("Invalid permissions");
        } catch (final IOException e) {
            LOG.error("Failed to get children for " + path.toString(), e);
            setError("Failed to get children");
        }
    }

    private void setError(final String errorMessage) {
        final Node node = Icon.getIcon(FontAwesomeIcon.EXCLAMATION_CIRCLE, Paint.valueOf("RED"));
        final Tooltip errorTip = new Tooltip(errorMessage);
        Tooltip.install(node, errorTip);
        this.setGraphic(node);
    }
}
