package com.spectralogic.dsbrowser.gui.util;


import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public final class Ds3Alert {
    public static void show(final String message, final Alert.AlertType type) {
        final Alert ALERT = new Alert(type);
        ALERT.setTitle("Information Dialog");
        ALERT.setHeaderText(null);
        final Stage stage = (Stage) ALERT.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEP_STORAGE_BROWSER));
        ALERT.setContentText(message);
        ALERT.showAndWait();
    }
}