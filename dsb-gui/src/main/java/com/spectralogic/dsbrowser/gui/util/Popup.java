package com.spectralogic.dsbrowser.gui.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class Popup {
    public static void show(final Parent parent, final String title) {
        final Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setMaxWidth(1000);
        final Scene popupScene = new Scene(parent);
        popup.setScene(popupScene);
        popup.setTitle(title);
        popup.setAlwaysOnTop(true);
        popup.showAndWait();

    }
}
