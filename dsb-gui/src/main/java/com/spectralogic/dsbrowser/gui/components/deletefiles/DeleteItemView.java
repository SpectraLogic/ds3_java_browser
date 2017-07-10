package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class DeleteItemView extends FXMLView {
    public DeleteItemView(final Ds3Task deleteTask) {
        super(name -> {
            if (name.equals(StringConstants.CASE_DELETETASK)) {
                return deleteTask;
            }
            return null;
        });
    }
}
