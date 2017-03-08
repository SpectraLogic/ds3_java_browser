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

package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTablePresenter;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;

public class DeleteFilesView extends FXMLView {
    public DeleteFilesView(final Ds3Task deleteTask, final Ds3TreeTablePresenter ds3TreeTablePresenter, final Ds3PanelPresenter ds3PanelPresenter) {
        super(name -> {
            switch (name) {
                case "deleteTask":
                    return deleteTask;
                case "ds3TreeTablePresenter":
                    return ds3TreeTablePresenter;
                case "ds3PanelPresenter":
                    return ds3PanelPresenter;
                default:
                    return null;
            }
        });
    }
}
