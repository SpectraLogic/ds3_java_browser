package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTablePresenter;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;

public class DeleteFilesView extends FXMLView {
    public DeleteFilesView(final Ds3Task deleteTask, final Ds3TreeTablePresenter ds3TreeTablePresenter, final Ds3PanelPresenter ds3PanelPresenter) {
        super(name -> {
            switch (name) {
                case "deleteTask":
                    return deleteTask;
                case "ds3TreeTablePresenter":
                    return ds3TreeTablePresenter;
                case "ds3PanelPresenter":
                    return ds3PanelPresenter;
                default:
                    return null;
            }
        });
    }
}
