package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class CreateFolderView extends FXMLView {
    public CreateFolderView(final CreateFolderModel createFolderModel) {
        super(name -> {
            switch (name) {
                case StringConstants.CASE_CREATEFOLDER:
                    return createFolderModel;
                default:
                    return null;
            }
        });
    }
}
