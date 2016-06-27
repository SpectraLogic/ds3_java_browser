package com.spectralogic.dsbrowser.gui.components.physicalPlacement;


import com.spectralogic.dsbrowser.gui.util.Popup;

public class PhysicalPlacementPopup {

    public static void show(final Ds3PhysicalPlacement physicalPlacement) {
        final PhysicalPlacementView view = new PhysicalPlacementView(physicalPlacement);
        //System.out.println("VVDN LOGS ====> POP Class called");
        Popup.show(view.getView(), "Physical Placement Location");
    }
}
