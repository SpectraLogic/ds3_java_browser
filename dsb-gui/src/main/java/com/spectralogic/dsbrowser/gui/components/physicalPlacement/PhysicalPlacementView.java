package com.spectralogic.dsbrowser.gui.components.physicalPlacement;

import com.airhacks.afterburner.views.FXMLView;

public class PhysicalPlacementView extends FXMLView {

    public PhysicalPlacementView(final Ds3PhysicalPlacement ds3physicalPlacement){
        super(name -> {
            switch (name) {
                case "ds3physicalPlacement": return ds3physicalPlacement;
                default: return null;
            }
        });
        //System.out.println("VVDN LOGS =====> View Class called");
    }
}
