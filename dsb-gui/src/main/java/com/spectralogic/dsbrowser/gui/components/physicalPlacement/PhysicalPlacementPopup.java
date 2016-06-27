package com.spectralogic.dsbrowser.gui.components.physicalplacement;


import com.spectralogic.dsbrowser.gui.util.Popup;

public class PhysicalPlacementPopup {

    public static void show(final Ds3PhysicalPlacement physicalPlacement) {
        final PhysicalPlacementView view = new PhysicalPlacementView(physicalPlacement);
        Popup.show(view.getView(), "Physical Placement Location");
    }
}
