package com.spectralogic.dsbrowser.gui.components.physicalplacement;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.ds3client.models.PhysicalPlacement;

public class PhysicalPlacementView extends FXMLView {

    public PhysicalPlacementView(final PhysicalPlacement ds3PhysicalPlacement) {
        super(name -> {
            switch (name) {
                case "ds3PhysicalPlacement":
                    return ds3PhysicalPlacement;
                default:
                    return null;
            }
        });

    }
}
