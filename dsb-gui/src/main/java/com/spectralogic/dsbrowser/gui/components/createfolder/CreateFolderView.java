package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.airhacks.afterburner.views.FXMLView;

public class CreateFolderView extends FXMLView {
    public CreateFolderView(final CreateFolderModel createFolderModel) {
        super(name -> {
            switch (name) {
                case "createFolderModel":
                    return createFolderModel;
                default:
                    return null;
            }
        });
    }
}
