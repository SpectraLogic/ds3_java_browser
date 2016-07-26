package com.spectralogic.dsbrowser.gui;

import com.spectralogic.dsbrowser.gui.components.about.AboutView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelView;
import com.spectralogic.dsbrowser.gui.components.jobpriority.JobSettingsView;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.LocalFileTreeTableView;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPopup;
import com.spectralogic.dsbrowser.gui.components.settings.SettingsView;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.gui.util.Popup;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.controlsfx.control.TaskProgressView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class DeepStorageBrowserPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(DeepStorageBrowserPresenter.class);

    @FXML
    public AnchorPane fileSystem;

    @FXML
    public AnchorPane blackPearl;

    @FXML
    private SplitPane jobSplitter;

    @FXML
    private CheckMenuItem jobsMenuItem, logsMenuItem;

    @FXML
    private TabPane bottomTabPane;

    @FXML
    private Tab jobsTab, logsTab;

    @FXML
    private TextFlow logTextFlow;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private MenuItem aboutMenuItem, closeMenuItem, sessionsMenuItem, settingsMenuItem, jobSettingsItem;

    @FXML
    private Menu fileMenu, helpMenu, viewMenu;

    @Inject
    private JobWorkers jobWorkers;

    private TaskProgressView<Ds3JobTask> jobProgressView;

    @Inject
    private ResourceBundle resourceBundle;

    @Inject
    public SavedJobPrioritiesStore savedJobPrioritiesStore;

    @Inject
    private SettingsStore settingsStore;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        try {
            LOG.info("Loading Main view" + settingsStore.getProcessSettings());
            jobProgressView = new TaskProgressView<>();
            Bindings.bindContentBidirectional(jobWorkers.getTasks(), jobProgressView.getTasks());

            //Setting up labels from resource file
            fileMenu.setText(resourceBundle.getString("fileMenu"));
            sessionsMenuItem.setText(resourceBundle.getString("sessionsMenuItem"));
            settingsMenuItem.setText(resourceBundle.getString("settingsMenuItem"));
            jobSettingsItem.setText(resourceBundle.getString("jobSettingsItem"));
            closeMenuItem.setText(resourceBundle.getString("closeMenuItem"));
            viewMenu.setText(resourceBundle.getString("viewMenu"));
            jobsMenuItem.setText(resourceBundle.getString("jobsMenuItem"));
            logsMenuItem.setText(resourceBundle.getString("logsMenuItem"));
            helpMenu.setText(resourceBundle.getString("helpMenu"));
            aboutMenuItem.setText(resourceBundle.getString("aboutMenuItem"));

            jobsMenuItem.selectedProperty().setValue(true);
            logsMenuItem.selectedProperty().setValue(true);
            jobsTab.setContent(jobProgressView);
            logsTab.setContent(scrollPane);
            jobSplitter.getItems().add(bottomTabPane);
            jobSplitter.setDividerPositions(0.75);

            jobsMenuItem.setOnAction(event -> {
                if (jobsMenuItem.isSelected()) {
                    jobsTab.setContent(jobProgressView);
                    bottomTabPane.getTabs().add(0, jobsTab);
                    bottomTabPane.getSelectionModel().select(jobsTab);
                    if (!jobSplitter.getItems().stream().anyMatch(i -> i instanceof TabPane)) {
                        jobSplitter.getItems().add(bottomTabPane);
                        jobSplitter.setDividerPositions(0.75);
                    }
                } else {
                    if (!logsMenuItem.isSelected())
                        jobSplitter.getItems().remove(bottomTabPane);
                    bottomTabPane.getTabs().remove(jobsTab);
                }
            });

            logsMenuItem.setOnAction(event -> {
                if (logsMenuItem.isSelected()) {
                    logsTab.setContent(scrollPane);
                    bottomTabPane.getTabs().add(logsTab);
                    bottomTabPane.getSelectionModel().select(logsTab);
                    if (!jobSplitter.getItems().stream().anyMatch(i -> i instanceof TabPane)) {
                        jobSplitter.getItems().add(bottomTabPane);
                        jobSplitter.setDividerPositions(0.75);
                    }
                } else {
                    if (!jobsMenuItem.isSelected())
                        jobSplitter.getItems().remove(bottomTabPane);
                    bottomTabPane.getTabs().remove(logsTab);
                }
            });

            final LocalFileTreeTableView localTreeView = new LocalFileTreeTableView(this);
            final Ds3PanelView ds3PanelView = new Ds3PanelView(this);
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

    public void showJobSettingsPopup() {
        final JobSettingsView jobSettingsView = new JobSettingsView();
        Popup.show(jobSettingsView.getView(), "Job Settings");
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


    public void logText(final String log, final LogType type) {
        final Text t = new Text();
        switch (type) {
            case SUCCESS:
                t.getStyleClass().add("successText");
                break;
            case ERROR:
                t.getStyleClass().add("errorText");
                break;
            default:
                t.getStyleClass().add("logText");
        }
        t.setText(formattedString(log));
        logTextFlow.getChildren().add(t);
        scrollPane.setVvalue(1.0);
    }

    private String formattedString(final String log) {
        return ">> " + log + "\n";
    }
}
