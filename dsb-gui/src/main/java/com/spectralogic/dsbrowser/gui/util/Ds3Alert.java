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

package com.spectralogic.dsbrowser.gui.util;


import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Optional;

public final class Ds3Alert {
    public static Optional<ButtonType> showConfirmationAlert(final String title, final String message, final Alert.AlertType type, final String headerText, final String exitButtonText, final String cancelButtonText) {
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
        exitButton.defaultButtonProperty().bind(exitButton.focusedProperty());
        cancelButton.setText(cancelButtonText);
        cancelButton.defaultButtonProperty().bind(cancelButton.focusedProperty());
        return  ALERT.showAndWait();
    }
}
