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

package com.spectralogic.dsbrowser.integration.tasks;

import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.util.FileTreeTableProvider;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.TreeItem;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

public class GetMediaDeviceTaskTest {

    private boolean successFlag = false;

    @Test
    public void call() throws Exception {
        new JFXPanel();
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final FileTreeTableProvider provider = new FileTreeTableProvider();
                final Stream<FileTreeModel> rootItems = provider.getRoot("My Computer");
                final TreeItem treeItem = Mockito.mock(TreeItem.class);
                Mockito.when(treeItem.getChildren()).thenReturn(FXCollections.observableArrayList());
                final Workers workers = new Workers();

                final GetMediaDeviceTask task = new GetMediaDeviceTask(rootItems, treeItem, provider, workers);
                workers.execute(task);
                task.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();
                });
                task.setOnFailed(event -> {
                    Assert.fail();
                    latch.countDown();

                });
                task.setOnCancelled(event -> {
                    Assert.fail();
                    latch.countDown();
                });
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