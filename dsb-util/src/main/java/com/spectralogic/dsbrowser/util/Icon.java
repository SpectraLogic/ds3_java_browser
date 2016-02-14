package com.spectralogic.dsbrowser.util;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.Node;

public final class Icon {
    public static FontAwesomeIconView getIcon(final FontAwesomeIcon name) {
        return new FontAwesomeIconView(name);
    }

}
