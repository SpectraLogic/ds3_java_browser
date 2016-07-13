package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class DeleteFilesPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(DeleteFilesPresenter.class);

    @FXML
    private TextField deleteField;

    @FXML
    private Button deleteButton;

    @Inject
    private Workers workers;

    @Inject
    private Ds3Task deleteTask;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            deleteField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.equals("DELETE")) {
                    deleteButton.setDisable(false);
                } else {
                    deleteButton.setDisable(true);
                }
            });

            deleteField.setOnKeyReleased(event -> {
                if (!deleteButton.isDisabled() && event.getCode().equals(KeyCode.ENTER)) {
                    deleteFiles();
                }
            });

        } catch (final Throwable e) {
            LOG.error("Encountered an error making the delete file presenter", e);
        }
    }

    public void deleteFiles() {
        deleteTask.setOnCancelled(this::handle);
        deleteTask.setOnFailed(this::handle);
        deleteTask.setOnSucceeded(this::handle);

        workers.execute(deleteTask);

    }

    private void handle(final Event event) {
        closeDialog();
    }

    public void cancelDelete() {
        LOG.info("Cancelling delete files");
        closeDialog();
    }

    private void closeDialog() {
        final Stage popupStage = (Stage) deleteField.getScene().getWindow();
        popupStage.close();
    }
}
