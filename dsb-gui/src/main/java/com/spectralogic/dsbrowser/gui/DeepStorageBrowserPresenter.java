package com.spectralogic.dsbrowser.gui;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;

import javafx.scene.control.*;
import javafx.scene.shape.Rectangle;
import org.controlsfx.control.TaskProgressView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spectralogic.dsbrowser.gui.components.about.AboutView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelView;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.LocalFileTreeTableView;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPopup;
import com.spectralogic.dsbrowser.gui.components.settings.SettingsView;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.util.Popup;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;

public class DeepStorageBrowserPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(DeepStorageBrowserPresenter.class);

    @FXML
    private AnchorPane fileSystem;

    @FXML
    private AnchorPane blackPearl;

    @FXML
    private SplitPane jobSplitter;

    @FXML
    private CheckMenuItem jobsMenuItem, darkViewCheckMenuItem, lightViewCheckMenuItem;

    @FXML
    private TabPane jobSelector;

    @FXML
    Rectangle rectangle;

    @FXML
    private MenuItem versionMenuItem, licenseMenuItem, aboutMenuItem, helpMenuItem, themeMenuItem, closeMenuItem, sessionsMenuItem, settingsMenuItem;

    @FXML
    private Menu fileMenu, helpMenu, viewMenu;

    @Inject
    private JobWorkers jobWorkers;

    private TaskProgressView<Ds3JobTask> jobProgressView;

    @Inject
    private ResourceBundle resourceBundle;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        try {
            LOG.info("Loading Main view");
            jobProgressView = new TaskProgressView<>();
            Bindings.bindContentBidirectional(jobWorkers.getTasks(), jobProgressView.getTasks());

            fileMenu.setText(resourceBundle.getString("fileMenu"));
            sessionsMenuItem.setText(resourceBundle.getString("sessionsMenuItem"));
            settingsMenuItem.setText(resourceBundle.getString("settingsMenuItem"));
            closeMenuItem.setText(resourceBundle.getString("closeMenuItem"));
            viewMenu.setText(resourceBundle.getString("viewMenu"));
            jobsMenuItem.setText(resourceBundle.getString("jobsMenuItem"));
            helpMenu.setText(resourceBundle.getString("helpMenu"));
            aboutMenuItem.setText(resourceBundle.getString("aboutMenuItem"));

            jobsMenuItem.setOnAction(event -> {
                if (jobsMenuItem.isSelected()) {
                    // jobSelector.setMinHeight(250);
                    jobSplitter.getItems().add(jobSelector);
                    jobSelector.getSelectionModel().selectLast();
                    jobSelector.getSelectionModel().getSelectedItem().setContent(rectangle);
                    rectangle.setHeight(700);
                    jobSelector.getSelectionModel().selectFirst();
                    jobSelector.getSelectionModel().getSelectedItem().setContent(jobProgressView);
                    jobSplitter.setDividerPositions(0.75);

                } else {
                    jobSplitter.getItems().remove(jobSelector);
                    jobSplitter.setDividerPositions(1.0);
                }
            }) ;
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
        Popup.show(settingsView.getView(), "Logging Settings");
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
