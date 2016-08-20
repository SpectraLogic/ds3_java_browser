package com.spectralogic.dsbrowser.gui.components.modifyjobpriority;

import com.airhacks.afterburner.views.FXMLView;

public class ModifyJobPriorityView extends FXMLView {

    public ModifyJobPriorityView(final ModifyJobPriorityModel value) {
        super(name -> {
            switch (name) {
                case "value":
                    return value;
                default:
                    return null;
            }
        });
    }
}
