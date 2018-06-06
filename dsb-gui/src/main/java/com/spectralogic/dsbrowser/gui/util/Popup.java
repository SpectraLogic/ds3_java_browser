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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.inject.Inject;


public class Popup {

    private final Ds3Common ds3Common;

    @Inject
    public Popup(final Ds3Common ds3Common) {
        this.ds3Common =  ds3Common;
    }

    public void show(final Parent parent, final String title, final Boolean resizeable) {
        final Stage popup = new Stage();
        popup.initOwner(ds3Common.getWindow());

        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setMaxWidth(1000);
        final Scene popupScene = new Scene(parent);
        popup.getIcons().add(new Image(StringConstants.DSB_ICON_PATH));
        popup.setScene(popupScene);
        popup.setTitle(title);
        popup.setAlwaysOnTop(false);
        popup.setResizable(resizeable);
        popup.showAndWait();
    }

    public void show(final Parent parent, final String title) {
        show(parent, title, false);
    }
}
