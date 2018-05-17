
/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;


public class CustomPasswordTextControl extends GridPane {
    private TextField textField;
    private PasswordField passwordField;
    private Button showButton;
    private final BooleanProperty isShown = new SimpleBooleanProperty(false);

    public CustomPasswordTextControl() {

        initTextField();
        initPasswordField();
        passwordField.textProperty().bindBidirectional(textField.textProperty());

        initShowButton();

        add(textField,0, 0);
        add(passwordField, 0, 0);
        add(showButton, 1, 0);
    }
    private void initShowButton() {
        showButton = new Button();
        showButton.setGraphic(Icon.getIcon(FontAwesomeIcon.EYE));

        showButton.setOnAction(SafeHandler.logHandle(event -> {
            isShown.setValue(!isShown.get());

            if (isShown.getValue()) {
                showButton.setGraphic(Icon.getIcon(FontAwesomeIcon.EYE_SLASH));
            } else {
                showButton.setGraphic(Icon.getIcon(FontAwesomeIcon.EYE));
            }
        }));
    }

    private void initPasswordField() {
        passwordField = new PasswordField();
        passwordField.managedProperty().bind(isShown.not());
        passwordField.visibleProperty().bind(isShown.not());
    }

    private void initTextField() {
        textField = new TextField();
        textField.managedProperty().bind(isShown);
        textField.visibleProperty().bind(isShown);
    }

    public void setText(final String text) {
        textField.setText(text);
    }

    public String getText() {
        return textField.getText();
    }

    public TextField getTextField() {
        return textField;
    }
}
