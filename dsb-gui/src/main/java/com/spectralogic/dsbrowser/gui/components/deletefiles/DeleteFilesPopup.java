package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTablePresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.Popup;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public final class DeleteFilesPopup {
    public static void show(final Ds3Task deleteTask, final Ds3PanelPresenter ds3PanelPresenter, final Ds3TreeTablePresenter ds3TreeTablePresenter, final Ds3Common ds3Common) {
        final DeleteFilesView deleteView = new DeleteFilesView(deleteTask, ds3TreeTablePresenter, ds3PanelPresenter, ds3Common);
        if (ds3PanelPresenter != null) {
            ObservableList<TreeItem<Ds3TreeTableValue>> selectedPanelItems = ds3Common.getDs3TreeTableView().getSelectionModel().getSelectedItems();
            if (Guard.isNullOrEmpty(selectedPanelItems)) {
                selectedPanelItems = FXCollections.observableArrayList();
                selectedPanelItems.add(ds3Common.getDs3TreeTableView().getRoot());
            }
            changeLabelText(selectedPanelItems, deleteView);
        } else if (ds3TreeTablePresenter != null) {
            ObservableList<TreeItem<Ds3TreeTableValue>> selectedMenuItems = ds3TreeTablePresenter.ds3TreeTable.getSelectionModel().getSelectedItems();
            if (Guard.isNullOrEmpty(selectedMenuItems)) {
                selectedMenuItems = FXCollections.observableArrayList();
                selectedMenuItems.add(ds3TreeTablePresenter.ds3TreeTable.getRoot());
            }
            changeLabelText(selectedMenuItems, deleteView);
        }
    }

    private static void changeLabelText(final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems,
                                        final DeleteFilesView deleteView) {
        if (selectedItems.get(0).getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
            Popup.show(deleteView.getView(), "Delete File(s)");
        } else if (selectedItems.get(0).getValue().getType().equals(Ds3TreeTableValue.Type.Directory)) {
            Popup.show(deleteView.getView(), "Delete Folder");
        } else {
            Popup.show(deleteView.getView(), "Delete Bucket");
        }

    }
}
