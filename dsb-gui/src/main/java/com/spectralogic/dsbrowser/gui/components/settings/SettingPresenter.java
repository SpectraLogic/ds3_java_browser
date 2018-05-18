/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
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

package com.spectralogic.dsbrowser.gui.components.settings;

import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.JobSettings;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.logservice.ApplicationLoggerSettings;
import com.spectralogic.dsbrowser.gui.services.settings.*;
import com.spectralogic.dsbrowser.gui.util.AlertService;
import com.spectralogic.dsbrowser.gui.util.PriorityFilter;
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

@Presenter
public class SettingPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(SettingPresenter.class);

    @FXML
    private ComboBox<String> getJobPriority, putJobPriority;

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

    @FXML
    private Button saveFilePropertiesEnableButton, cancelFilePropertiesEnableButton;

    @FXML
    private Label putJobPriorityText;

    @FXML
    private Label getJobPriorityText;

    @FXML
    private Tab jobPriority;

    @FXML
    private Button saveSettingsJobButton;

    @FXML
    private Button cancelSettingsJobButton;

    @FXML
    private Tab fileProperties;

    @FXML
    private Tooltip enableFilePropertiesTooltip;

    @FXML
    private Label enableFileProperties;

    @FXML
    private CheckBox filePropertiesCheckbox;

    private final ResourceBundle resourceBundle;
    private final JobWorkers jobWorkers;
    private final SavedJobPrioritiesStore savedJobPrioritiesStore;
    private final SettingsStore settingsStore;
    private final ApplicationLoggerSettings applicationLoggerSettings;

    private JobSettings jobSettings;
    private FilePropertiesSettings filePropertiesSettings;
    private LogSettings logSettings;
    private ProcessSettings processSettings;
    private final AlertService alert;

    @Inject
    public SettingPresenter(final ResourceBundle resourceBundle,
                            final JobWorkers jobWorkers,
                            final SavedJobPrioritiesStore savedJobPrioritiesStore,
                            final SettingsStore settingsStore,
                            final AlertService alertService,
                            final ApplicationLoggerSettings applicationLoggerSettings) {
        this.resourceBundle = resourceBundle;
        this.jobWorkers = jobWorkers;
        this.savedJobPrioritiesStore = savedJobPrioritiesStore;
        this.settingsStore = settingsStore;
        this.applicationLoggerSettings = applicationLoggerSettings;
        this.alert = alertService;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            this.logSettings = settingsStore.getLogSettings();
            this.processSettings = settingsStore.getProcessSettings();
            this.jobSettings = savedJobPrioritiesStore.getJobSettings();
            this.filePropertiesSettings = settingsStore.getFilePropertiesSettings();
            initGUIElements();
            initPropertyPane();
        } catch (final Throwable t) {
            LOG.error("Failed to initialize SettingPresenter", t);
        }
    }

    public void saveFilePropertiesSettings() {
        LOG.info("Updating fileProperties settingsStore");
        try {
            if (filePropertiesCheckbox.isSelected()) {
                settingsStore.setFilePropertiesSettings(true);
            } else {
                settingsStore.setFilePropertiesSettings(false);
            }
            alert.info("filePropertiesSettingsUpdated");
        } catch (final Exception e) {
            LOG.error("Failed to save file properties", e);
        }
    }

    public void savePerformanceSettings() {
        LOG.info("Updating maximum number of Threads");
        settingsStore.setProcessSettings(processSettings);
        jobWorkers.setWorkers(Executors.newFixedThreadPool(processSettings.getMaximumNumberOfParallelThreads()));
        alert.info("performanceSettingsUpdated");
    }

    public void saveLogSettings() {
        LOG.info("Updating logging settingsStore");
        settingsStore.setLogSettings(logSettings);
        applicationLoggerSettings.setLogSettings(logSettings);
        alert.info("loggingSettingsUpdated");
    }

    public void saveJobSettings() {
        LOG.info("Updating jobs settingsStore");
        try {
            jobSettings.setGetJobPriority(getJobPriority.getSelectionModel().getSelectedItem());
            jobSettings.setPutJobPriority(putJobPriority.getSelectionModel().getSelectedItem());
            SavedJobPrioritiesStore.saveSavedJobPriorties(savedJobPrioritiesStore);
            alert.info("jobsSettingsUpdated");
        } catch (final Exception e) {
            LOG.error("Failed to save job priorities", e);
        }
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
        Bindings.bindBidirectional(filePropertiesCheckbox.selectedProperty(), filePropertiesSettings.filePropertiesEnableProperty());
    }

    private void initGUIElements() {
        putJobPriorityText.setText(resourceBundle.getString("putJobPriorityText"));
        getJobPriorityText.setText(resourceBundle.getString("getJobPriorityText"));
        jobPriority.setText(resourceBundle.getString("jobPriority"));
        saveSettingsJobButton.setText(resourceBundle.getString("saveSettingsJobButton"));
        cancelSettingsJobButton.setText(resourceBundle.getString("cancelSettingsJobButton"));
        fileProperties.setText(resourceBundle.getString("fileProperties"));
        enableFilePropertiesTooltip.setText(resourceBundle.getString("enableFilePropertiesTooltip"));
        performanceLabel.setText(resourceBundle.getString("performanceLabel"));
        locationSetting.setText(resourceBundle.getString("locationSetting"));
        logSizeSetting.setText(resourceBundle.getString("logSizeSetting"));
        savedLogSetting.setText(resourceBundle.getString("savedLogSetting"));
        enableLoggingSetting.setText(resourceBundle.getString("enableLoggingSetting"));
        saveSettingsButton.setText(resourceBundle.getString("saveSettingsButton"));
        cancelSettingsButton.setText(resourceBundle.getString("cancelSettingsButton"));
        browseButton.setText(resourceBundle.getString("browseButton"));
        saveSettingsPerforanceButton.setText(resourceBundle.getString("saveSettingsPerformanceButton"));
        cancelSettingsPerforanceButton.setText(resourceBundle.getString("cancelSettingsPerformanceButton"));
        performanceTab.setText(resourceBundle.getString("performanceTab"));
        loggingTab.setText(resourceBundle.getString("loggingTab"));
        putJobPriority.getItems().add(resourceBundle.getString("defaultPolicyText"));
        getJobPriority.getItems().add(resourceBundle.getString("defaultPolicyText"));
        enableFileProperties.setText(resourceBundle.getString("enableFileProperties"));
        saveFilePropertiesEnableButton.setText(resourceBundle.getString("saveFilePropertiesEnableButton"));
        cancelFilePropertiesEnableButton.setText(resourceBundle.getString("cancelFilePropertiesEnableButton"));
        filePropertiesCheckbox.setSelected(filePropertiesSettings.isFilePropertiesEnabled());
        final Priority[] priorities = PriorityFilter.priorities;
        for (final Priority priority : priorities) {
            putJobPriority.getItems().add(priority.toString());
            getJobPriority.getItems().add(priority.toString());
        }
        putJobPriority.getSelectionModel().select(jobSettings.getPutJobPriority());
        getJobPriority.getSelectionModel().select(jobSettings.getGetJobPriority());
        performanceFieldValue.setText(String.valueOf(processSettings.getMaximumNumberOfParallelThreads()));
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
