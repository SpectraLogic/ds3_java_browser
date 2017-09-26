
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

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Lazily initialize Alerts.  Delay the action of showing the Alert.  Store the Alert for re-use.  Do not waste memory
 * if this Alert is never encountered.
 */
public class LazyAlert {

    private final Alert.AlertType alertType;
    private Alert alert = null;

    public LazyAlert(final Alert.AlertType alertType) {
        this.alertType = alertType;
    }

    public LazyAlert() {
        this(Alert.AlertType.INFORMATION);
    }

    private void showAlertInternal(final String message, final String title) {
        if (alert == null) {
            alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);

            final Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(ImageURLs.DEEP_STORAGE_BROWSER));

        }
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showAlert(final String message, final String title) {
        if (Platform.isFxApplicationThread()) {
            showAlertInternal(message, title);
        } else {
            Platform.runLater(() -> showAlertInternal(message, title));
        }
    }

}
