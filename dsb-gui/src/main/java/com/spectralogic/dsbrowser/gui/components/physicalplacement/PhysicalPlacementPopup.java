package com.spectralogic.dsbrowser.gui.components.physicalplacement;


import com.spectralogic.ds3client.models.PhysicalPlacement;
import com.spectralogic.dsbrowser.gui.util.Popup;

import java.util.ResourceBundle;

public final class PhysicalPlacementPopup {

    public static void show(final PhysicalPlacement physicalPlacement, final ResourceBundle resourceBundle) {
        final PhysicalPlacementView view = new PhysicalPlacementView(physicalPlacement);
        Popup.show(view.getView(), resourceBundle.getString("physicalPlacementLocation"));
    }
}
