package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;

public class Ds3TreeTableView extends FXMLView {
    public Ds3TreeTableView(final Session session) {
        super(name -> {
            if (name.equals("session")) {
                return session;
            }
            return null;
        });
    }
}
