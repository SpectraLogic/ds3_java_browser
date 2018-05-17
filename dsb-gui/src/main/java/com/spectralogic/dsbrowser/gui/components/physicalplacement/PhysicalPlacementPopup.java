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
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.inject.Inject;
import java.util.ResourceBundle;

public class PhysicalPlacementPopup {

    private final ResourceBundle resourceBundle;
    private final Ds3Common ds3Common;

    @Inject
    public PhysicalPlacementPopup(final ResourceBundle resourceBundle, final  Ds3Common ds3Common) {
        this.resourceBundle = resourceBundle;
        this.ds3Common = ds3Common;
    }

    public void show(final PhysicalPlacement physicalPlacement) {
        final PhysicalPlacementView view = new PhysicalPlacementView(physicalPlacement);

        final Stage popup = new Stage();
        popup.initOwner(ds3Common.getWindow());
        popup.initModality(Modality.APPLICATION_MODAL);
        final Scene popupScene = new Scene(view.getView());
        popup.getIcons().add(new Image(StringConstants.DSB_ICON_PATH));
        popup.setScene(popupScene);
        popup.setTitle(resourceBundle.getString("physicalPlacementLocation"));
        popup.setAlwaysOnTop(false);
        popup.setResizable(true);
        popup.showAndWait();
    }

}
