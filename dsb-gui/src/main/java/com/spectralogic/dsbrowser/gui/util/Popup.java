package com.spectralogic.dsbrowser.gui.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ResourceBundle;

public final class Popup {
    public static void show(final Parent parent, final String title) {
        final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();
        final Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setMaxWidth(1000);
        final Scene popupScene = new Scene(parent);
        popup.getIcons().add(new Image(resourceBundle.getString("dsbIconPath")));
        popup.setScene(popupScene);
        popup.setTitle(title);
        popup.setAlwaysOnTop(false);
        popup.setResizable(false);
        popup.showAndWait();
    }
}
