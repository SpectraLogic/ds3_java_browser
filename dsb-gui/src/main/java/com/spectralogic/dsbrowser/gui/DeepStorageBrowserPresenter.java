package com.spectralogic.dsbrowser.gui;

import com.spectralogic.dsbrowser.gui.components.about.AboutView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelView;
import com.spectralogic.dsbrowser.gui.components.license.LicenseView;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.LocalFileTreeTableView;
import com.spectralogic.dsbrowser.gui.util.Popup;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
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
        try {
            LOG.info("Loading Main view");
            final LocalFileTreeTableView localTreeView = new LocalFileTreeTableView();
            final Ds3PanelView ds3PanelView = new Ds3PanelView();
            localTreeView.getViewAsync(fileSystem.getChildren()::add);
            ds3PanelView.getViewAsync(blackPearl.getChildren()::add);
        } catch (final Throwable e) {
            LOG.error("Encountered an error when creating Main view", e);
            throw e;
        }
    }

    public void showLicensePopup() {
        final LicenseView licenseView = new LicenseView();
        Popup.show(licenseView.getView(), "Licenses");
    }

    public void showAboutPopup() {
        final AboutView aboutView = new AboutView();
        Popup.show(aboutView.getView(), "About");

    }

    public void closeWindow() {
        Platform.exit();
    }

}
