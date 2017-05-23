package com.spectralogic.dsbrowser.gui.components.about;

import com.google.inject.Inject;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.gui.util.Constants;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

@Presenter
public class AboutPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(AboutPresenter.class);

    @FXML
    private Label copyRightLabel1, copyRightLabel2;

    @FXML
    private Hyperlink hyperlink;

    @FXML
    private Label title;

    @FXML
    private Label buildVersion;

    private final ResourceBundle resourceBundle;

    @Inject
    public AboutPresenter(final ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        title.setText(resourceBundle.getString("title"));
        buildVersion.setText(resourceBundle.getString("buildVersion"));
        hyperlink.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(new URI(Constants.APACHE_URL));
            } catch (final IOException|URISyntaxException e) {
                LOG.error("Failed to open apache license in a browser", e);
            }
        });
        copyRightLabel1.setText(resourceBundle.getString("copyrightTxt1"));
        copyRightLabel2.setText(resourceBundle.getString("copyrightTxt2"));
    }
}
