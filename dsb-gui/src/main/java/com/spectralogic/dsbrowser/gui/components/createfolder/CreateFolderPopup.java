package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.Popup;

public class CreateFolderPopup {
    public static void show(CreateFolderModel createFolderModel) {
        final CreateFolderView createFolderView = new CreateFolderView(createFolderModel);
        Popup.show(createFolderView.getView(), "Create Folder");
    }
}
