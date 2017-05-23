package com.spectralogic.dsbrowser.gui.components.settings;

import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.JobSettings;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.logservice.ApplicationLoggerSettings;
import com.spectralogic.dsbrowser.gui.services.settings.*;
import com.spectralogic.dsbrowser.gui.util.LazyAlert;
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

    private final LazyAlert alert = new LazyAlert("Error");

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
    private Label performanceLabel, showCachedJob;

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
    private Tooltip enableFilePropertiesTooltip, showCachedJobTooltip;

    @FXML
    private Label enableFileProperties;

    @FXML
    private CheckBox filePropertiesCheckbox, showCachedJobCheckbox;

    private final ResourceBundle resourceBundle;
    private final JobWorkers jobWorkers;
    private final SavedJobPrioritiesStore savedJobPrioritiesStore;
    private final SettingsStore settingsStore;
    private final ApplicationLoggerSettings applicationLoggerSettings;

    private JobSettings jobSettings;
    private FilePropertiesSettings filePropertiesSettings;
    private ShowCachedJobSettings showCachedJobSettings;
    private LogSettings logSettings;
    private ProcessSettings processSettings;

    @Inject
    public SettingPresenter(final ResourceBundle resourceBundle,
                            final JobWorkers jobWorkers,
                            final SavedJobPrioritiesStore savedJobPrioritiesStore,
                            final SettingsStore settingsStore,
                            final ApplicationLoggerSettings applicationLoggerSettings) {
        this.resourceBundle = resourceBundle;
        this.jobWorkers = jobWorkers;
        this.savedJobPrioritiesStore = savedJobPrioritiesStore;
        this.settingsStore = settingsStore;
        this.applicationLoggerSettings = applicationLoggerSettings;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            this.logSettings = settingsStore.getLogSettings();
            this.processSettings = settingsStore.getProcessSettings();
            this.jobSettings = savedJobPrioritiesStore.getJobSettings();
            this.filePropertiesSettings = settingsStore.getFilePropertiesSettings();
            this.showCachedJobSettings = settingsStore.getShowCachedJobSettings();
            initGUIElements();
            initPropertyPane();
        } catch (final Exception e) {
            LOG.error("Failed to init SettingPresenter", e);
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
            alert.showAlert(resourceBundle.getString("filePropertiesSettingsUpdated"));
        } catch (final Exception e) {
            LOG.error("Failed to save file properties", e);
        }
    }

    public void savePerformanceSettings() {
        LOG.info("Updating maximum number of Threads");
        settingsStore.setProcessSettings(processSettings);
        jobWorkers.setWorkers(Executors.newFixedThreadPool(processSettings.getMaximumNumberOfParallelThreads()));
        alert.showAlert(resourceBundle.getString("performanceSettingsUpdated"));
    }

    public void saveLogSettings() {
        LOG.info("Updating logging settingsStore");
        settingsStore.setLogSettings(logSettings);
        applicationLoggerSettings.setLogSettings(logSettings);
        alert.showAlert(resourceBundle.getString("loggingSettingsUpdated"));
    }

    public void saveJobSettings() {
        LOG.info("Updating jobs settingsStore");
        try {
            jobSettings.setGetJobPriority(getJobPriority.getSelectionModel().getSelectedItem());
            jobSettings.setPutJobPriority(putJobPriority.getSelectionModel().getSelectedItem());
            SavedJobPrioritiesStore.saveSavedJobPriorties(savedJobPrioritiesStore);
            if (showCachedJobCheckbox.isSelected()) {
                settingsStore.setShowCachedJobSettings(true);
            } else {
                settingsStore.setShowCachedJobSettings(false);
            }
            alert.showAlert(resourceBundle.getString("jobsSettingsUpdated"));
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
        Bindings.bindBidirectional(showCachedJobCheckbox.selectedProperty(), showCachedJobSettings.showCachedJobEnableProperty());
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
        showCachedJob.setText(resourceBundle.getString("showCachedJob"));
        showCachedJobTooltip.setText(resourceBundle.getString("showCachedJobTooltip"));
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
        putJobPriority.getItems().add(resourceBundle.getString("defaultPolicyText"));
        getJobPriority.getItems().add(resourceBundle.getString("defaultPolicyText"));
        enableFileProperties.setText(resourceBundle.getString("enableFileProperties"));
        saveFilePropertiesEnableButton.setText(resourceBundle.getString("saveFilePropertiesEnableButton"));
        cancelFilePropertiesEnableButton.setText(resourceBundle.getString("cancelFilePropertiesEnableButton"));
        filePropertiesCheckbox.setSelected(filePropertiesSettings.isFilePropertiesEnabled());
        final Priority[] priorities = PriorityFilter.filterPriorities(Priority.values());
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
