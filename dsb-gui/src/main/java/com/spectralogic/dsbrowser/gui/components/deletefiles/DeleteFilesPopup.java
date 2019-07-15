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

package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.Popup;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ResourceBundle;

@Singleton
public class DeleteFilesPopup {
    private final static Logger LOG = LoggerFactory.getLogger(DeleteFilesPopup.class);

    private final Ds3Common ds3Common;
    private final ResourceBundle resourceBundle;
    private final Popup popup;

    @Inject
    public DeleteFilesPopup(
            final Ds3Common ds3Common,
            final ResourceBundle resourceBundle,
            final Popup popup
    ) {
        this.ds3Common = ds3Common;
        this.resourceBundle = resourceBundle;
        this.popup = popup;
    }


    public void show(final Ds3Task deleteTask, final Window window) {
        final DeleteItemView deleteView = new DeleteItemView(deleteTask);
        if (ds3Common.getDs3TreeTableView() != null) {
            final ObservableList<TreeItem<Ds3TreeTableValue>> selectedPanelItems = ds3Common.getDs3TreeTableView().getSelectionModel().getSelectedItems();
            changeLabelText(selectedPanelItems, deleteView, window);
        }
    }

    private void changeLabelText(final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems,
            final DeleteItemView deleteView, final Window window) {
        if (selectedItems.get(0).getValue().getType() == Ds3TreeTableValue.Type.File) {
            popup.show(deleteView.getView(), resourceBundle.getString("deleteFiles"), window);
        } else if (selectedItems.get(0).getValue().getType() == Ds3TreeTableValue.Type.Directory) {
            popup.show(deleteView.getView(), resourceBundle.getString("deleteFolder"), window);
        } else {
            popup.show(deleteView.getView(), resourceBundle.getString("deleteBucket"), window);
        }

    }
}
