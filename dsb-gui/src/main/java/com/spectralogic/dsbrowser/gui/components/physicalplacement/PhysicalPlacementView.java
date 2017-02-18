package com.spectralogic.dsbrowser.gui.components.physicalplacement;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.ds3client.models.PhysicalPlacement;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class PhysicalPlacementView extends FXMLView {

    public PhysicalPlacementView(final PhysicalPlacement ds3PhysicalPlacement) {
        super(name -> {
            switch (name) {
                case StringConstants.CASE_DS3PHYSICALPLACEMENT:
                    return ds3PhysicalPlacement;
                default:
                    return null;
            }
        });

    }
}
