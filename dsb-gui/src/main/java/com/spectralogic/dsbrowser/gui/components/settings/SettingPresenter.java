package com.spectralogic.dsbrowser.gui.components.settings;

import com.spectralogic.dsbrowser.gui.services.settings.LogSettings;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingPresenter implements Initializable {

    private final Logger LOG = LoggerFactory.getLogger(SettingPresenter.class);
    @FXML
    CheckBox debugLogging;

    @FXML
    TextField numRolling;

    @FXML
    TextField logDirectory;

    @FXML
    TextField logSize;

    @Inject
    SettingsStore settings;

    private LogSettings logSettings;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            this.logSettings = settings.getLogSettings();
            initPropertyPane();
        } catch(final Throwable e) {
            LOG.error("Failed to startup settings presenter");
        }
    }

    public void saveLogSettings() {

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
    }

    public void showFileExplorer(final MouseEvent event) {
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
