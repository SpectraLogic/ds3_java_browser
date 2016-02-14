package com.spectralogic.dsbrowser.gui.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class Popup {
    public static void show(final Parent parent, final String title) {
        final Stage popup = new Stage();
        popup.setMaxWidth(1000);
        final Scene popupScene = new Scene(parent);
        popup.setScene(popupScene);
        popup.setTitle(title);
        popup.showAndWait();

    }
}
