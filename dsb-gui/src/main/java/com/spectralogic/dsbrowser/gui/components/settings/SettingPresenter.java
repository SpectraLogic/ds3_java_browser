package com.spectralogic.dsbrowser.gui.components.settings;

import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.JobSettings;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.logservice.LogService;
import com.spectralogic.dsbrowser.gui.services.settings.FilePropertiesSettings;
import com.spectralogic.dsbrowser.gui.services.settings.LogSettings;
import com.spectralogic.dsbrowser.gui.services.settings.ProcessSettings;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.PriorityFilter;
import com.sun.org.apache.xpath.internal.SourceTree;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.converter.BooleanStringConverter;
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
    private ComboBox<String> getJobPriority, putJobPriority;

    @FXML
    private CheckBox isDefaultCheckBox, debugLogging;

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

    @Inject
    private SavedJobPrioritiesStore jobPrioritiesStore;

    private JobSettings jobSettings;

    private FilePropertiesSettings filePropertiesSettings;

    @FXML
    private Label enableFileProperties;

    @FXML
    private CheckBox filePropertiesCheckbox;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            this.logSettings = settings.getLogSettings();
            this.processSettings = settings.getProcessSettings();
            this.jobSettings = jobPrioritiesStore.getJobSettings();
            this.filePropertiesSettings = settings.getFilePropertiesSettings();
            initGUIElements();
            initPropertyPane();
        } catch (final Throwable e) {
            LOG.error("Failed to startup settings presenter");
        }
    }

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


    public void saveFilePropertiesSettings() {
        LOG.info("Updating fileProperties settings");
        try {
            if (filePropertiesCheckbox.isSelected()) {
                settings.setFilePropertiesSettings(true);
            } else {
                settings.setFilePropertiesSettings(false);
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
        closeDialog();
    }

    public void saveLogSettings() {
        LOG.info("Updating logging settings");
        settings.setLogSettings(logSettings);
        settings.setProcessSettings(processSettings);
        logService.setLogSettings(logSettings);
        jobWorkers.setWorkers(Executors.newFixedThreadPool(processSettings.getMaximumNumberOfParallelThreads()));
        closeDialog();
    }

    public void saveJobSettings() {
        LOG.info("Updating jobs settings");
        try {
            jobSettings.setGetJobPriority(getJobPriority.getSelectionModel().getSelectedItem());
            jobSettings.setPutJobPriority(putJobPriority.getSelectionModel().getSelectedItem());
            jobPrioritiesStore.saveSavedJobPriorties(jobPrioritiesStore);
        } catch (final Exception e) {
            e.printStackTrace();
        }
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
        Bindings.bindBidirectional(null, filePropertiesSettings.filePropertiesEnableProperty(), new BooleanStringConverter());
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
        saveSettingsPerforanceButton.setText(resourceBundle.getString("saveSettingsPerforanceButton"));
        cancelSettingsPerforanceButton.setText(resourceBundle.getString("cancelSettingsPerforanceButton"));
        performanceTab.setText(resourceBundle.getString("performanceTab"));
        loggingTab.setText(resourceBundle.getString("loggingTab"));
        putJobPriority.getItems().add(resourceBundle.getString("defaultPolicyText"));
        getJobPriority.getItems().add(resourceBundle.getString("defaultPolicyText"));
        enableFileProperties.setText(resourceBundle.getString("enableFileProperties"));
        saveFilePropertiesEnableButton.setText(resourceBundle.getString("saveFilePropertiesEnableButton"));
        cancelFilePropertiesEnableButton.setText(resourceBundle.getString("cancelFilePropertiesEnableButton"));
        filePropertiesCheckbox.setSelected(filePropertiesSettings.getFilePropertiesEnable().booleanValue());
        final Priority[] priorities = PriorityFilter.filterPriorities(Priority.values());
        for (final Priority priority : priorities) {
            putJobPriority.getItems().add(priority.toString());
            getJobPriority.getItems().add(priority.toString());
        }
        putJobPriority.getSelectionModel().select(jobSettings.getPutJobPriority().toString());
        getJobPriority.getSelectionModel().select(jobSettings.getGetJobPriority().toString());
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
