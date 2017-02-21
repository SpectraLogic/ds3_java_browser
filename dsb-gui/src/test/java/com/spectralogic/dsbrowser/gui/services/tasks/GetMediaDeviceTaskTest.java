package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.util.FileTreeTableProvider;
import com.spectralogic.dsbrowser.gui.services.Workers;
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