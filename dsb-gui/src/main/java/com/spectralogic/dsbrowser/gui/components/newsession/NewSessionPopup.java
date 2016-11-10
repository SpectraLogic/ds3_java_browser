package com.spectralogic.dsbrowser.gui.components.newsession;

import com.spectralogic.dsbrowser.gui.util.Popup;

public final class NewSessionPopup {
    public static void show() {
        final NewSessionView view = new NewSessionView();
        Popup.show(view.getView(), "Sessions");
    }
}
