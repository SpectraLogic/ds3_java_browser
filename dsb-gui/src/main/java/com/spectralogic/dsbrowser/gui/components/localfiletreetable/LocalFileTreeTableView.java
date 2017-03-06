package com.spectralogic.dsbrowser.gui.components.localfiletreetable;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class LocalFileTreeTableView extends FXMLView {

    public LocalFileTreeTableView(final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
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
