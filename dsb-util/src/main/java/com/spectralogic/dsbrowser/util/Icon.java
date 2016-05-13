package com.spectralogic.dsbrowser.util;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.paint.Paint;

public final class Icon {
    public static FontAwesomeIconView getIcon(final FontAwesomeIcon name) {
        return new FontAwesomeIconView(name);
    }

    public static FontAwesomeIconView getIcon(final FontAwesomeIcon name, final Paint color) {
        final FontAwesomeIconView icon = new FontAwesomeIconView(name);
        icon.setFill(color);
        return icon;
    }
}
