package com.spectralogic.dsbrowser.gui.components.settings;

import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.logservice.LogService;
import com.spectralogic.dsbrowser.gui.services.settings.LogSettings;
import com.spectralogic.dsbrowser.gui.services.settings.ProcessSettings;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;

public class SettingPresenter implements Initializable {

    private final Logger LOG = LoggerFactory.getLogger(SettingPresenter.class);
    @FXML
    private CheckBox debugLogging;

    @FXML
    private TextField numRolling;

    @FXML
    private TextField logDirectory;

    @FXML
    private TextField logSize;

    @FXML
    private TextField performanceFieldValue;

    @FXML
    private Tab loggingTab, performanceTab;

    @Inject
    private SettingsStore settings;

    @Inject
    private LogService logService;

    private LogSettings logSettings;

    private ProcessSettings processSettings;

    @FXML
    private Label performanceLabel;

    @FXML
    private Label locationSetting;

    @FXML
    private Label logSizeSetting;

    @FXML
    private Label savedLogSetting;

    @FXML
    private Label enableLoggingSetting;

    @FXML
    private Button saveSettingsButton, saveSettingsPerforanceButton;

    @FXML
    private Button cancelSettingsButton, cancelSettingsPerforanceButton;

    @FXML
    private Button browseButton;

    @Inject
    private ResourceBundle resourceBundle;

    @Inject
    private JobWorkers jobWorkers;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            this.logSettings = settings.getLogSettings();
            this.processSettings = settings.getProcessSettings();
            initGUIElements();
            initPropertyPane();
        } catch (final Throwable e) {
            LOG.error("Failed to startup settings presenter");
        }
    }

    public void saveLogSettings() {
        LOG.info("Updating logging settings");
        settings.setLogSettings(logSettings);
        settings.setProcessSettings(processSettings);
        logService.setLogSettings(logSettings);
        jobWorkers.setWorkers(Executors.newFixedThreadPool(processSettings.getMaximumNumberOfParallelThreads()));
        closeDialog();
    }

    public void closeDialog() {
        final Stage popupStage = (Stage) logSize.getScene().getWindow();
        popupStage.close();
    }

    private void initPropertyPane() {
        Bindings.bindBidirectional(logDirectory.textProperty(), logSettings.logLocationProperty());
        Bindings.bindBidirectional(logSize.textProperty(), logSettings.logSizeProperty(), new NumberStringConverter());
        Bindings.bindBidirectional(numRolling.textProperty(), logSettings.numRolloversProperty(), new NumberStringConverter());
        Bindings.bindBidirectional(debugLogging.selectedProperty(), logSettings.debugLoggingProperty());
        Bindings.bindBidirectional(performanceFieldValue.textProperty(), processSettings.maximumNumberOfParallelThreadsProperty(), new NumberStringConverter());

    }

    private void initGUIElements() {
        performanceLabel.setText(resourceBundle.getString("performanceLabel"));
        locationSetting.setText(resourceBundle.getString("locationSetting"));
        logSizeSetting.setText(resourceBundle.getString("logSizeSetting"));
        savedLogSetting.setText(resourceBundle.getString("savedLogSetting"));
        enableLoggingSetting.setText(resourceBundle.getString("enableLoggingSetting"));
        saveSettingsButton.setText(resourceBundle.getString("saveSettingsButton"));
        cancelSettingsButton.setText(resourceBundle.getString("cancelSettingsButton"));
        browseButton.setText(resourceBundle.getString("browseButton"));
        saveSettingsPerforanceButton.setText(resourceBundle.getString("saveSettingsPerforanceButton"));
        cancelSettingsPerforanceButton.setText(resourceBundle.getString("cancelSettingsPerforanceButton"));
        performanceTab.setText(resourceBundle.getString("performanceTab"));
        loggingTab.setText(resourceBundle.getString("loggingTab"));

        performanceFieldValue.setText("" + processSettings.getMaximumNumberOfParallelThreads());
    }

    public void showFileExplorer() {
        final Stage stage = new Stage();
        final DirectoryChooser directoryChooser =
                new DirectoryChooser();
        final File selectedDirectory =
                directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            this.logSettings.setLogLocation(selectedDirectory.getAbsolutePath());
        }
    }
}
