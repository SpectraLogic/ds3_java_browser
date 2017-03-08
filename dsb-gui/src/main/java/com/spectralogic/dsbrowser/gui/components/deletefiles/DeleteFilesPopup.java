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

import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTablePresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.Popup;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public final class DeleteFilesPopup {
    public static void show(final Ds3Task deleteTask, final Ds3PanelPresenter ds3PanelPresenter, final Ds3TreeTablePresenter ds3TreeTablePresenter) {
        final DeleteFilesView deleteView = new DeleteFilesView(deleteTask, ds3TreeTablePresenter, ds3PanelPresenter);
        if (ds3PanelPresenter != null) {
            final ObservableList<TreeItem<Ds3TreeTableValue>> selectedPanelItems = ds3PanelPresenter.getDs3TreeTableView().getSelectionModel().getSelectedItems();
            changeLabelText(selectedPanelItems, deleteView);
        } else if (ds3TreeTablePresenter != null) {
            final ObservableList<TreeItem<Ds3TreeTableValue>> selectedMenuItems = ds3TreeTablePresenter.ds3TreeTable.getSelectionModel().getSelectedItems();
            changeLabelText(selectedMenuItems, deleteView);
        }
    }

    public static void changeLabelText(final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems, final DeleteFilesView deleteView) {
        if (selectedItems.get(0).getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
            Popup.show(deleteView.getView(), "Delete File(s)");
        } else if (selectedItems.get(0).getValue().getType().equals(Ds3TreeTableValue.Type.Directory)) {
            Popup.show(deleteView.getView(), "Delete Folder");
        } else {
            Popup.show(deleteView.getView(), "Delete Bucket");
        }

    }
}
