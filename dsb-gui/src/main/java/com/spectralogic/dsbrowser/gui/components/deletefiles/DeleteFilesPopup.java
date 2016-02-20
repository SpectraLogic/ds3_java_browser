package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.Popup;

import java.util.ArrayList;

public final class DeleteFilesPopup {
    public static void show(final Session session, final String bucketName, final ArrayList<Ds3TreeTableValue> files) {
        final DeleteFilesView deleteView = new DeleteFilesView(session, bucketName, files);
        Popup.show(deleteView.getView(), "Delete Files");
    }
}
