package com.spectralogic.dsbrowser.gui.components.localfiletreetable;


import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;

public class LocalFileTreeTableView extends FXMLView {

    public LocalFileTreeTableView(DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
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
