package com.spectralogic.dsbrowser.gui.components.physicalplacement;


import com.spectralogic.ds3client.models.PhysicalPlacement;
import com.spectralogic.dsbrowser.gui.util.Popup;

public final class PhysicalPlacementPopup {

    public static void show(final PhysicalPlacement physicalPlacement) {
        final PhysicalPlacementView view = new PhysicalPlacementView(physicalPlacement);
        Popup.show(view.getView(), "Physical Placement Location");
    }
}
