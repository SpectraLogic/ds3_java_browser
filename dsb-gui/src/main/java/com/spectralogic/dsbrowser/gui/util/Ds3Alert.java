package com.spectralogic.dsbrowser.gui.util;


import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Optional;

public final class Ds3Alert {
    public static void show(final String title, final String message, final Alert.AlertType type) {
        final Alert ALERT = new Alert(type);
        ALERT.setTitle(title);
        ALERT.setHeaderText(null);
        final Stage stage = (Stage) ALERT.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEP_STORAGE_BROWSER));
        ALERT.setContentText(message);
        ALERT.showAndWait();
    }

    public static Optional<ButtonType> showConfirmationAlert(final String title, final String message, final Alert.AlertType type, final String headerText,final String exitButtonText,final String cancelButtonText) {
        final Alert ALERT = new Alert(type);
        ALERT.setTitle(title);
        ALERT.setHeaderText(headerText);
        final Stage stage = (Stage) ALERT.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEP_STORAGE_BROWSER));
        ALERT.setContentText(message);
        final Button exitButton = (Button) ALERT.getDialogPane().lookupButton(
                ButtonType.OK
        );
        final Button cancelButton = (Button) ALERT.getDialogPane().lookupButton(
                ButtonType.CANCEL
        );
        exitButton.setText(exitButtonText);
        cancelButton.setText(cancelButtonText);
        return  ALERT.showAndWait();
    }
}