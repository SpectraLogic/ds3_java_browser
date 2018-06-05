
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

import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ResourceBundle;

/**
 * Lazily initialize Alerts.  Delay the action of showing the Alert.  Store the Alert for re-use.  Do not waste memory
 * if this Alert is never encountered.
 */

@Singleton
public class AlertService {
    private static final String ERROR_TITLE = "errorTitle";
    private static final String ALERT_TITLE = "alertTitle";
    private static final String WARNING_TITLE = "warningTitle";

    private final ResourceBundle resourceBundle;
    private final Ds3Common ds3Common;
    private Alert alert = null;

    @Inject
    public AlertService(final ResourceBundle resourceBundle, final Ds3Common ds3Common) {
        this.resourceBundle = resourceBundle;
        this.ds3Common = ds3Common;
    }


    private void showAlertInternal(final String message, final String title, final Alert.AlertType alertType) {
        if (alert == null) {
            alert = new Alert(alertType);
            alert.initOwner(ds3Common.getWindow());
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setAlertType(alertType);

        //This prevents a null pointer when a dialog  is called right after startup
        final DialogPane dp = alert.getDialogPane();
        if (dp != null) {
            final Scene scene = dp.getScene();
            if (scene != null) {
                final Stage w = (Stage) scene.getWindow();
                if (w != null) {
                    w.getIcons().add(new Image(ImageURLs.DEEP_STORAGE_BROWSER));
                }
            }
        }

        alert.setContentText(message);
        alert.setAlertType(alertType);
        alert.showAndWait();
        alert.close();
    }

    public void error(final String message) {
        showAlertInternal(resourceBundle.getString(message), resourceBundle.getString(ERROR_TITLE), Alert.AlertType.ERROR);
    }

    public void info(final String message) {
        showAlertInternal(resourceBundle.getString(message), resourceBundle.getString(ALERT_TITLE), Alert.AlertType.INFORMATION);
    }

    public void warning(final String message) {
        showAlertInternal(resourceBundle.getString(message), resourceBundle.getString(WARNING_TITLE), Alert.AlertType.WARNING);
    }

}
