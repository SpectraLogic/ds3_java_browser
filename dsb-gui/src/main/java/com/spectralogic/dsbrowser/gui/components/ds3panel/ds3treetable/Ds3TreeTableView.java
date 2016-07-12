package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;

public class Ds3TreeTableView extends FXMLView {
    public Ds3TreeTableView(final Session session, DeepStorageBrowserPresenter deepStorageBrowserPresenter, Ds3PanelPresenter ds3PanelPresenter) {
        super(name -> {
            if (name.equals("session")) {
                return session;
            } else if(name.equals("deepStorageBrowserPresenter"))
                return deepStorageBrowserPresenter;
            else if(name.equals("ds3PanelPresenter"))
                return ds3PanelPresenter;
            return null;
        });
    }
}
