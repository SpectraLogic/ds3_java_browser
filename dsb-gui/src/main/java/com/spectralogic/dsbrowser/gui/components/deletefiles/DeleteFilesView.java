package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;

import java.util.ArrayList;

public class DeleteFilesView extends FXMLView {
    public DeleteFilesView(final Session session, final String bucketName, final ArrayList<Ds3TreeTableValue> files) {
        super(name -> {
            switch (name) {
                case "files":
                    return files;
                case "session":
                    return session;
                case "bucketName":
                    return bucketName;
                default:
                    return null;
            }
        });
    }
}
