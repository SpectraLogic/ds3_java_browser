package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;

import com.airhacks.afterburner.views.FXMLView;

public class JobInfoView extends FXMLView {

    public JobInfoView(final EndpointInfo endpointInfo) {
        super(name -> {
            switch (name) {
                case "endpointInfo":
                    return endpointInfo;
                default:
                    return null;
            }
        });
    }
}
