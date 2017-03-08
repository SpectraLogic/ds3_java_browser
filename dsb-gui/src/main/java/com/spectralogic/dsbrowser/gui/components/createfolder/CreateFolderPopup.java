package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.spectralogic.dsbrowser.gui.util.Popup;

public final class CreateFolderPopup {
    public static void show(final CreateFolderModel createFolderModel) {
        final CreateFolderView createFolderView = new CreateFolderView(createFolderModel);
        Popup.show(createFolderView.getView(), "Create Folder");
    }
}
