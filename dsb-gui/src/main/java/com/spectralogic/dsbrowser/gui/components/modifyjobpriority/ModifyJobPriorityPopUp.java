package com.spectralogic.dsbrowser.gui.components.modifyjobpriority;

import com.spectralogic.dsbrowser.gui.util.Popup;

public final class ModifyJobPriorityPopUp {

    public static void show(final ModifyJobPriorityModel value) {
        final ModifyJobPriorityView view = new ModifyJobPriorityView(value);
        Popup.show(view.getView(), "Change Job Priority");
    }

}
