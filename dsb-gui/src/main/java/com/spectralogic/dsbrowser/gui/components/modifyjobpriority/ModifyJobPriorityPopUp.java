package com.spectralogic.dsbrowser.gui.components.modifyjobpriority;

import com.spectralogic.dsbrowser.gui.util.Popup;

import java.util.ResourceBundle;

public final class ModifyJobPriorityPopUp {

    public static void show(final ModifyJobPriorityModel value, final ResourceBundle resourceBundle) {
        final ModifyJobPriorityView view = new ModifyJobPriorityView(value);
        Popup.show(view.getView(), resourceBundle.getString("changeJobPriority"));
    }

}
