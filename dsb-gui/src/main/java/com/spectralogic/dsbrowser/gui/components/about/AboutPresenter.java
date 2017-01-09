package com.spectralogic.dsbrowser.gui.components.about;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class AboutPresenter implements Initializable {
    @FXML
    private Label copyRightLabel1, copyRightLabel2;
    @FXML
    private Hyperlink hyperlink;

    private ResourceBundle resourceBundle = null;

    @FXML
    private Label title;

    @FXML
    private Label buildVersion;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.resourceBundle = ResourceBundle.getBundle("lang", new Locale("en"));
        title.setText(resourceBundle.getString("title"));
        buildVersion.setText(resourceBundle.getString("buildVersion"));
        hyperlink.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(new URI("http://www.apache.org/licenses/LICENSE-2.0"));
            } catch (final IOException e1) {
                e1.printStackTrace();
            } catch (final URISyntaxException e1) {
                e1.printStackTrace();
            }
        });
        copyRightLabel1.setText(resourceBundle.getString("copyrightTxt1"));
        copyRightLabel2.setText(resourceBundle.getString("copyrightTxt2"));
    }
}
