package com.spectralogic.dsbrowser.gui.util;

import javafx.application.Platform;

final public class UIThreadUtil {
    public final static void runInFXThread(final Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
