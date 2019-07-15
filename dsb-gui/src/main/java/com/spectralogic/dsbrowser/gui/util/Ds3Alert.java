/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
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


import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class Ds3Alert {
    private final Ds3Common ds3Common;

    @Inject
    public Ds3Alert(final Ds3Common ds3Common) {
        this.ds3Common = ds3Common;
    }

    public Optional<ButtonType> showConfirmationAlert(final String title, final String message, final Alert.AlertType type, final String headerText, final String exitButtonText, final String cancelButtonText) {
        final Alert alert = new Alert(type);
        alert.initOwner(ds3Common.getWindow());
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        final Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEP_STORAGE_BROWSER));
        alert.setContentText(message);
        final Button exitButton = (Button) alert.getDialogPane().lookupButton(
                ButtonType.OK
        );
        final Button cancelButton = (Button) alert.getDialogPane().lookupButton(
                ButtonType.CANCEL
        );
        exitButton.setText(exitButtonText);
        exitButton.defaultButtonProperty().bind(exitButton.focusedProperty());
        cancelButton.setText(cancelButtonText);
        cancelButton.defaultButtonProperty().bind(cancelButton.focusedProperty());
        return alert.showAndWait();
    }
}
