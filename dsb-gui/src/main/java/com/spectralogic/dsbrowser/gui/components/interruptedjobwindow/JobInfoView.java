package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class JobInfoView extends FXMLView {

    public JobInfoView(final EndpointInfo endpointInfo) {
        super(name -> {
            switch (name) {
                case StringConstants.CASE_ENDPOINT:
                    return endpointInfo;
                default:
                    return null;
            }
        });
    }
}
