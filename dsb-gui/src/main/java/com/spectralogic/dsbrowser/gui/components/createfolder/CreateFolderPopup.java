package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.util.Popup;

import java.util.ResourceBundle;

public final class CreateFolderPopup {
    public static void show(final CreateFolderModel createFolderModel, final DeepStorageBrowserPresenter deepStorageBrowserPresenter, final ResourceBundle resourceBundle) {
        final CreateFolderView createFolderView = new CreateFolderView(createFolderModel, deepStorageBrowserPresenter);
        Popup.show(createFolderView.getView(), resourceBundle.getString("createFolderButton"));
    }
}
