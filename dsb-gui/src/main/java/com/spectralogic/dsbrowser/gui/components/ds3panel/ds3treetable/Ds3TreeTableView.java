package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;

public class Ds3TreeTableView extends FXMLView {
    public Ds3TreeTableView(final Session session, final Ds3PanelPresenter ds3PanelPresenter) {
        super(name -> {
            switch (name) {
                case "session":
                    return session;
                case "ds3PanelPresenter":
                    return ds3PanelPresenter;
                default:
                    return null;
            }
        });
    }
}
