package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.spectralogic.dsbrowser.gui.util.Popup;

import java.util.ResourceBundle;

public final class CreateFolderPopup {
    public static void show(final CreateFolderModel createFolderModel, final ResourceBundle resourceBundle) {
        final CreateFolderView createFolderView = new CreateFolderView(createFolderModel);
        Popup.show(createFolderView.getView(), resourceBundle.getString("createFolderButton"));
    }
}
