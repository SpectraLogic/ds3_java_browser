package com.spectralogic.dsbrowser.gui;

import com.spectralogic.dsbrowser.gui.components.license.LicenseView;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.LocalFileTreeTableView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class DeepStorageBrowserPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(DeepStorageBrowserPresenter.class);

    @FXML
    AnchorPane fileSystem;

    @FXML
    AnchorPane blackPearl;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        LOG.info("Loading Main view");
        final LocalFileTreeTableView localTreeView = new LocalFileTreeTableView();

        localTreeView.getViewAsync(fileSystem.getChildren()::add);
    }

    public void showLicensePopup() {
        final Stage popup = new Stage();
        popup.setMaxWidth(1000);
        final LicenseView licenseView = new LicenseView();
        final Scene popupScene = new Scene(licenseView.getView());
        popup.setScene(popupScene);
        popup.setTitle("Licenses");
        popup.showAndWait();
    }

    public void showAboutPopup() {

    }

    public void closeWindow() {
        Platform.exit();
    }

}
