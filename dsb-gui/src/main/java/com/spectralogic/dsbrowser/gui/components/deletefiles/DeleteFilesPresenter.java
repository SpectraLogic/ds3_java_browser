package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.spectralogic.ds3client.commands.DeleteObjectsRequest;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DeleteFilesPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(DeleteFilesPresenter.class);

    @FXML
    TextField deleteField;

    @FXML
    Button deleteButton;

    @Inject
    String bucketName;

    @Inject
    ArrayList<Ds3TreeTableValue> files;

    @Inject
    Session session;

    @Inject
    Workers workers;

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

        } catch (final Throwable e) {
            LOG.error("Encountered an error making the delete file presenter", e);
        }
    }

    public void deleteFiles() {

        final Task task = new Task() {
            @Override
            protected Object call() throws Exception {
                try {
                    session.getClient().deleteObjects(new DeleteObjectsRequest(bucketName, files.stream().map(Ds3TreeTableValue::getFullName).collect(Collectors.toList())));
                } catch (final IOException | SignatureException e) {
                    LOG.error("Failed to delete files" + e);
                }
                return null;
            }
        };

        task.setOnCancelled(this::handle);
        task.setOnFailed(this::handle);
        task.setOnSucceeded(this::handle);

        workers.execute(task);

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
