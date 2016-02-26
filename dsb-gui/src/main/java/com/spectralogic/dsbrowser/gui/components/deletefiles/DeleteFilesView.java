package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;

public class DeleteFilesView extends FXMLView {
    public DeleteFilesView(final Ds3Task deleteTask) {
        super(name -> {
            switch (name) {
                case "deleteTask":
                    return deleteTask;
                default:
                    return null;
            }
        });
    }
}
