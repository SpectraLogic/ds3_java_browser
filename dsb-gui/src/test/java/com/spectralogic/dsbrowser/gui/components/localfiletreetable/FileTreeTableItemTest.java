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

package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.spectralogic.browser.gui.testUtil.LoggingServiceFake;
import com.spectralogic.ds3client.utils.ResourceUtils;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.fail;


public class FileTreeTableItemTest {

    final private LoggingService loggingService = new LoggingServiceFake();
    private boolean successFlag =  false;
    private final Workers workers = new Workers(1);
    private final FileTreeTableProvider fileTreeTableProvider = new FileTreeTableProvider(new DateTimeUtils());

    @Test
    public void getGraphicType() throws URISyntaxException, IOException {
        final Path path = ResourceUtils.loadFileResource(SessionConstants.LOCAL_FOLDER + SessionConstants.LOCAL_FILE);
        final FileTreeModel fileTreeModel = new FileTreeModel(path, FileTreeModel.Type.File, Files.size(path), 0, "");
        final FileTreeTableProvider fileTreeTableProvider = Mockito.mock(FileTreeTableProvider.class);
        final FileTreeTableItem fileTreeTableItem = new FileTreeTableItem(fileTreeTableProvider, fileTreeModel, new Workers(), loggingService);
        Assert.assertNotNull(fileTreeTableItem.getGraphicType(fileTreeModel));
    }

    @Test
    public void getLeaf() throws URISyntaxException, IOException {
        final Path path = ResourceUtils.loadFileResource(SessionConstants.LOCAL_FOLDER + SessionConstants.LOCAL_FILE);
        final FileTreeTableProvider fileTreeTableProvider = Mockito.mock(FileTreeTableProvider.class);
        final FileTreeModel fileTreeModel = new FileTreeModel(path, FileTreeModel.Type.File, Files.size(path), 0, "");
        Assert.assertTrue(new FileTreeTableItem(fileTreeTableProvider, fileTreeModel, new Workers(), loggingService).isLeaf());
    }

    @Test
    public void getGraphicFont() throws FileNotFoundException, URISyntaxException {
        final Path path = ResourceUtils.loadFileResource(SessionConstants.LOCAL_FOLDER + SessionConstants.LOCAL_FILE);
        final FileTreeTableProvider fileTreeTableProvider = Mockito.mock(FileTreeTableProvider.class);
        final FileTreeModel fileTreeModel = new FileTreeModel(path, FileTreeModel.Type.Directory, 0, 0, "");
        Assert.assertNotNull(new FileTreeTableItem(fileTreeTableProvider, fileTreeModel, new Workers(), loggingService).getGraphicFont(fileTreeModel));
    }

    @Test
    public void refresh() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        new JFXPanel();
        Platform.runLater(() -> {
            try {
                final Path path = ResourceUtils.loadFileResource(SessionConstants.LOCAL_FOLDER);
                final File testFolder = path.toFile();
                final int numFiles = testFolder.list().length;
                final FileTreeTableProvider fileTreeTableProvider = new FileTreeTableProvider(new DateTimeUtils());
                final FileTreeModel fileTreeModel = new FileTreeModel(path, FileTreeModel.Type.Directory, 0, 0, "");
                final FileTreeTableItem fileTreeTableItem = new FileTreeTableItem(fileTreeTableProvider, fileTreeModel, new Workers(), loggingService);
                fileTreeTableItem.refresh();
                final ObservableList<TreeItem<FileTreeModel>> children = fileTreeTableItem.getChildren();
                if (children.size() == numFiles) {
                    successFlag = true;
                }
                latch.countDown();
            } catch (final URISyntaxException | IOException e) {
                e.printStackTrace();
                latch.countDown();
                fail();
            }
        });
        latch.await();
        Assert.assertTrue(successFlag);
    }

}