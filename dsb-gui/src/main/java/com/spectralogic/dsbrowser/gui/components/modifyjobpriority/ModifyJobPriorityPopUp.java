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

package com.spectralogic.dsbrowser.gui.components.modifyjobpriority;

import com.spectralogic.dsbrowser.gui.util.Popup;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ResourceBundle;

@Singleton
public final class ModifyJobPriorityPopUp {

    private final ResourceBundle resourceBundle;
    private final Popup popup;

    @Inject
    public ModifyJobPriorityPopUp(final ResourceBundle resourceBundle, Popup popup) {
        this.resourceBundle = resourceBundle;
        this.popup = popup;
    }

    public void show(final ModifyJobPriorityModel value) {
        final ModifyJobPriorityView view = new ModifyJobPriorityView(value);
        popup.show(view.getView(), resourceBundle.getString("changeJobPriority"));
    }

}
