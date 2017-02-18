package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTablePresenter;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;

public class DeleteFilesView extends FXMLView {
    public DeleteFilesView(final Ds3Task deleteTask, final Ds3Common ds3Common) {
        super(name -> {
            switch (name) {
                case "deleteTask":
                    return deleteTask;
                case "ds3Common":
                    return ds3Common;
                default:
                    return null;
            }
        });
    }
}
