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

package com.spectralogic.dsbrowser.gui.components.physicalplacement;

import com.spectralogic.ds3client.models.PhysicalPlacement;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ResourceBundle;

public final class PhysicalPlacementPopup {

    public static void show(final PhysicalPlacement physicalPlacement, final ResourceBundle resourceBundle) {
        final PhysicalPlacementView view = new PhysicalPlacementView(physicalPlacement);

        final Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        final Scene popupScene = new Scene(view.getView());
        popup.getIcons().add(new Image(resourceBundle.getString("dsbIconPath")));
        popup.setScene(popupScene);
        popup.setTitle(resourceBundle.getString("physicalPlacementLocation"));
        popup.setAlwaysOnTop(false);
        popup.setResizable(true);
        popup.showAndWait();
    }

}
