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

package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.Popup;
import com.spectralogic.dsbrowser.gui.util.ResourceBundleProperties;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

public final class DeleteFilesPopup {
    private final static Logger LOG = LoggerFactory.getLogger(DeleteFilesPopup.class);

    private static final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();

    public static void show(final Ds3Task deleteTask, final Ds3Common ds3Common) {
        final DeleteItemView deleteView = new DeleteItemView(deleteTask);
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = ds3Common.getDs3TreeTableView();
        if (ds3TreeTableView != null) {

            ObservableList<TreeItem<Ds3TreeTableValue>> selectedPanelItems = ds3Common.getDs3TreeTableView().getSelectionModel().getSelectedItems();
            if (Guard.isNullOrEmpty(selectedPanelItems)) {
                selectedPanelItems = FXCollections.observableArrayList();
                selectedPanelItems.add(ds3Common.getDs3TreeTableView().getRoot());
            }
            changeLabelText(selectedPanelItems, deleteView);
        } else if (ds3Common.getDs3TreeTableView() != null) {
            ObservableList<TreeItem<Ds3TreeTableValue>> selectedMenuItems = ds3Common.getDs3TreeTableView().getSelectionModel().getSelectedItems();
            if (Guard.isNullOrEmpty(selectedMenuItems)) {
                selectedMenuItems = FXCollections.observableArrayList();
                selectedMenuItems.add(ds3Common.getDs3TreeTableView().getRoot());
            }
            changeLabelText(selectedMenuItems, deleteView);
        }
    }

    private static void changeLabelText(final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems,
                                        final DeleteItemView deleteView) {
        if (selectedItems.get(0).getValue().getType() == (Ds3TreeTableValue.Type.File)) {
            Popup.show(deleteView.getView(), resourceBundle.getString("deleteFiles"));
        } else if (selectedItems.get(0).getValue().getType() == (Ds3TreeTableValue.Type.Directory)) {
            Popup.show(deleteView.getView(), resourceBundle.getString("deleteFolder"));
        } else {
            Popup.show(deleteView.getView(), resourceBundle.getString("deleteBucket"));
        }

    }
}
