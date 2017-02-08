package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.components.about.AboutView;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Test;


public class PopupTest {

    @Test
    public void show() throws Exception {
        new JFXPanel();
        Platform.runLater(() -> {
            Popup.show(new AboutView().getView(), "About");
        });
        Thread.sleep(10000);
    }
}