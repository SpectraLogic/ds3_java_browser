package com.spectralogic.dsbrowser.gui.util;

import javafx.scene.Node;

public class Ds3Button extends javafx.scene.control.Button {

    public Ds3Button( ) {
        super ( );
        bindFocusToDefault ( );
    }

    public Ds3Button(final String text ) {
        super ( text );
        bindFocusToDefault ( );
    }

    public Ds3Button(final String text, final Node graphic ) {
        super ( text, graphic );
        bindFocusToDefault ( );
    }

    private void bindFocusToDefault ( ) {
        defaultButtonProperty().bind(focusedProperty());
    }

}