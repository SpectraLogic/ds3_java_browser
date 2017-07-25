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

import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3GetJob_Test;
import com.spectralogic.dsbrowser.gui.util.FileTreeTableProvider;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TreeItem;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

public class FileTreeTableItemTest {

    private boolean successFlag =  false;

    @Test
    public void getGraphicType() throws Exception {
        final ClassLoader classLoader = Ds3GetJob_Test.class.getClassLoader();
        final URL url = classLoader.getResource(SessionConstants.LOCAL_FOLDER + SessionConstants.LOCAL_FILE);
        Path path = null;
        if (url != null) {
            path = new File(url.getFile()).toPath();
        }
        final FileTreeModel fileTreeModel = new FileTreeModel(path, FileTreeModel.Type.File, Files.size(path), 0, "");
        final FileTreeTableProvider fileTreeTableProvider = Mockito.mock(FileTreeTableProvider.class);
        final FileTreeTableItem fileTreeTableItem = new FileTreeTableItem(fileTreeTableProvider, fileTreeModel, new Workers());
        Assert.assertNotNull(fileTreeTableItem.getGraphicType(fileTreeModel));
    }

    @Test
    public void getLeaf() throws Exception {
        final ClassLoader classLoader = Ds3GetJob_Test.class.getClassLoader();
        final URL url = classLoader.getResource(SessionConstants.LOCAL_FOLDER + SessionConstants.LOCAL_FILE);
        Path path = null;
        if (url != null) {
            path = new File(url.getFile()).toPath();
        }
        final FileTreeTableProvider fileTreeTableProvider = Mockito.mock(FileTreeTableProvider.class);
        final FileTreeModel fileTreeModel = new FileTreeModel(path, FileTreeModel.Type.File, Files.size(path), 0, "");
        Assert.assertTrue(new FileTreeTableItem(fileTreeTableProvider, fileTreeModel, new Workers()).isLeaf());
    }

    @Test
    public void getGraphicFont() throws Exception {
        final ClassLoader classLoader = Ds3GetJob_Test.class.getClassLoader();
        final URL url = classLoader.getResource(SessionConstants.LOCAL_FOLDER);
        Path path = null;
        if (url != null) {
            path = new File(url.getFile()).toPath();
        }
        final FileTreeTableProvider fileTreeTableProvider = Mockito.mock(FileTreeTableProvider.class);
        final FileTreeModel fileTreeModel = new FileTreeModel(path, FileTreeModel.Type.Directory, 0, 0, "");
        Assert.assertNotNull(new FileTreeTableItem(fileTreeTableProvider, fileTreeModel, new Workers()).getGraphicFont(fileTreeModel));
    }

    @Test
    public void refresh() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        new JFXPanel();
        Platform.runLater(() -> {
            try {
                final ClassLoader classLoader = Ds3GetJob_Test.class.getClassLoader();
                final URL url = classLoader.getResource(SessionConstants.LOCAL_FOLDER);
                Path path = null;
                if (url != null) {
                    path = new File(url.getFile()).toPath();
                }
                final FileTreeTableProvider fileTreeTableProvider = new FileTreeTableProvider();
                final FileTreeModel fileTreeModel = new FileTreeModel(path, FileTreeModel.Type.Directory, 0, 0, "");
                final FileTreeTableItem fileTreeTableItem = new FileTreeTableItem(fileTreeTableProvider, fileTreeModel, new Workers());
                fileTreeTableItem.refresh();
                final ObservableList<TreeItem<FileTreeModel>> children = fileTreeTableItem.getChildren();
                if (children.size() == path.toFile().list().length) {
                    successFlag = true;
                }
                latch.countDown();
            }
            catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        Assert.assertTrue(successFlag);
    }

}