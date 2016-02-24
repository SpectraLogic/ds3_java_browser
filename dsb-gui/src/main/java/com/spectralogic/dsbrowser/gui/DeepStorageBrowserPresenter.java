package com.spectralogic.dsbrowser.gui;

import com.spectralogic.dsbrowser.gui.components.about.AboutView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelView;
import com.spectralogic.dsbrowser.gui.components.license.LicenseView;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.LocalFileTreeTableView;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPopup;
import com.spectralogic.dsbrowser.gui.components.settings.SettingsView;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.util.Popup;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.controlsfx.control.TaskProgressView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class DeepStorageBrowserPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(DeepStorageBrowserPresenter.class);

    @FXML
    AnchorPane fileSystem;

    @FXML
    AnchorPane blackPearl;

    @FXML
    SplitPane jobSplitter;

    @FXML
    CheckMenuItem jobsMenuItem;

    @Inject
    JobWorkers jobWorkers;

    TaskProgressView<Ds3JobTask> jobProgressView;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        try {
            LOG.info("Loading Main view");
            jobProgressView = new TaskProgressView<>();
            Bindings.bindContentBidirectional(jobWorkers.getTasks(), jobProgressView.getTasks());
            jobsMenuItem.setOnAction(event -> {
                if (jobsMenuItem.isSelected()) {
                    jobSplitter.getItems().add(jobProgressView);
                    jobSplitter.setDividerPositions(0.75);
                } else {
                    jobSplitter.setDividerPositions(1.0);
                    jobSplitter.getItems().remove(jobProgressView);
                }
            });
            final LocalFileTreeTableView localTreeView = new LocalFileTreeTableView();
            final Ds3PanelView ds3PanelView = new Ds3PanelView();
            localTreeView.getViewAsync(fileSystem.getChildren()::add);
            ds3PanelView.getViewAsync(blackPearl.getChildren()::add);
        } catch (final Throwable e) {
            LOG.error("Encountered an error when creating Main view", e);
            throw e;
        }
    }

    public void showSettingsPopup() {
        final SettingsView settingsView = new SettingsView();
        Popup.show(settingsView.getView(), "Settings");
    }

    public void showLicensePopup() {
        final LicenseView licenseView = new LicenseView();
        Popup.show(licenseView.getView(), "Licenses");
    }

    public void showAboutPopup() {
        final AboutView aboutView = new AboutView();
        Popup.show(aboutView.getView(), "About");
    }

    public void showSessionPopup() {
        NewSessionPopup.show();
    }

    public void closeWindow() {
        Platform.exit();
    }

}
