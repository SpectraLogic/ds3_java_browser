package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTablePresenter;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class DeleteFilesView extends FXMLView {
    public DeleteFilesView(final Ds3Task deleteTask, final Ds3TreeTablePresenter ds3TreeTablePresenter, final Ds3PanelPresenter ds3PanelPresenter, final Ds3Common ds3Common) {
        super(name -> {
            switch (name) {
                case StringConstants.CASE_DELETETASK:
                    return deleteTask;
                case StringConstants.CASE_DS3TREETABLEPRESENTER:
                    return ds3TreeTablePresenter;
                case StringConstants.CASE_DS3PANELPRESENTER:
                    return ds3PanelPresenter;
                case StringConstants.CASE_DS3COMMON:
                    return ds3Common;
                default:
                    return null;
            }
        });
    }
}
