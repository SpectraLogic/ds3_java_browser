package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.components.about.AboutView;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class PopupTest {

    @Test
    public void show() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        new JFXPanel();
        Platform.runLater(() -> {
            Popup.show(new AboutView().getView(), "About");
        });
        latch.await(10, TimeUnit.SECONDS);
    }
}