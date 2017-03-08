/*
 * ****************************************************************************
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
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class LazyAlert {

    private final String title;
    private final Alert.AlertType alertType;
    private Alert alert = null;

    public LazyAlert(final String title, final Alert.AlertType alertType) {
        this.title = title;
        this.alertType = alertType;
    }

    public LazyAlert(final String title) {
        this(title, Alert.AlertType.INFORMATION);
    }

    private void showAlertInternal(final String message) {
        if (alert == null) {
            alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);

            final Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(ImageURLs.DEEPSTORAGEBROWSER));

        }
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showAlert(final String message) {
        if (Platform.isFxApplicationThread()) {
            showAlertInternal(message);
        } else {
            Platform.runLater(() -> showAlertInternal(message));
        }
    }
}
