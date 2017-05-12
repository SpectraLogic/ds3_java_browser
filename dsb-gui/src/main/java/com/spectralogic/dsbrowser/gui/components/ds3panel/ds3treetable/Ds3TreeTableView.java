package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class Ds3TreeTableView extends FXMLView {
    public Ds3TreeTableView(final Session session, final Ds3PanelPresenter ds3PanelPresenter) {
        super(name -> {
            switch (name) {
                case StringConstants.CASE_SESSION:
                    return session;
                case StringConstants.CASE_DS3PANELPRESENTER:
                    return ds3PanelPresenter;
                default:
                    return null;
            }
        });
    }
}
