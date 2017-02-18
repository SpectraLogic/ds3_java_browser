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
import javafx.scene.control.TreeTableView;

public final class DeleteFilesPopup {
    public static void show(final Ds3Task deleteTask, final Ds3Common ds3Common) {
        final DeleteFilesView deleteView = new DeleteFilesView(deleteTask, ds3Common);
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = ds3Common.getDs3TreeTableView();
        if (ds3TreeTableView != null) {
            ObservableList<TreeItem<Ds3TreeTableValue>> selectedPanelItems = ds3Common.getDs3TreeTableView().getSelectionModel().getSelectedItems();
            if (Guard.isNullOrEmpty(selectedPanelItems)) {
                selectedPanelItems = FXCollections.observableArrayList();
                selectedPanelItems.add(ds3Common.getDs3TreeTableView().getRoot());
            }
            changeLabelText(selectedPanelItems, deleteView);
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
