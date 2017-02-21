package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class Ds3PanelView extends FXMLView {
    public Ds3PanelView(final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        super(name -> {
            switch (name) {
                case StringConstants.CASE_DEEPSTORAGEBROWSER:
                    return deepStorageBrowserPresenter;
                default:
                    return null;
            }
        });
    }
}
