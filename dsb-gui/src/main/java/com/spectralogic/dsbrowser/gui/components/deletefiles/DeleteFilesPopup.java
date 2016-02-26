package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.Popup;

public final class DeleteFilesPopup {
    //public static void show(final Session session, final String bucketName, final ArrayList<Ds3TreeTableValue> files) {
    public static void show(final Ds3Task deleteTask) {
        final DeleteFilesView deleteView = new DeleteFilesView(deleteTask);
        Popup.show(deleteView.getView(), "Delete Files");
    }
}
