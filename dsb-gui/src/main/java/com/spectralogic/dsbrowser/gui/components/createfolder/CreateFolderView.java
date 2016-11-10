package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;

public class CreateFolderView extends FXMLView {
    public CreateFolderView(final CreateFolderModel createFolderModel, final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        super(name -> {
            switch (name) {
                case "createFolderModel":
                    return createFolderModel;
                case "deepStorageBrowserPresenter":
                    return deepStorageBrowserPresenter;
                default:
                    return null;
            }
        });
    }
}
