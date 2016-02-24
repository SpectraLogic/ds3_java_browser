package com.spectralogic.dsbrowser.gui.components.settings;

import javafx.beans.binding.Bindings;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
    TextField numRolling;

    @FXML
    TextField logDirectory;

    @FXML
    TextField logSize;

    @Inject
    SettingsModel settings;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initPropertyPane();
        } catch(final Throwable e) {
            LOG.error("Failed to startup settings presenter");
        }
    }

    private void initPropertyPane() {
        Bindings.bindBidirectional(logDirectory.textProperty(), settings.logLocationProperty());
        Bindings.bindBidirectional(logSize.textProperty(), settings.logSizeProperty(), new NumberStringConverter());
        Bindings.bindBidirectional(numRolling.textProperty(), settings.numRolloversProperty(), new NumberStringConverter());
    }

    public void showFileExplorer(final MouseEvent event) {
        final Stage stage = new Stage();
        final DirectoryChooser directoryChooser =
                new DirectoryChooser();
        final File selectedDirectory =
                directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            this.settings.setLogLocation(selectedDirectory.getAbsolutePath());
        }
    }
}
