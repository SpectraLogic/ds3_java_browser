package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;

public class Ds3PanelView extends FXMLView {
    public Ds3PanelView(final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        super(name -> {
            switch (name) {
                case "deepStorageBrowserPresenter":
                    return deepStorageBrowserPresenter;
                default:
                    return null;
            }
        });
    }
}
