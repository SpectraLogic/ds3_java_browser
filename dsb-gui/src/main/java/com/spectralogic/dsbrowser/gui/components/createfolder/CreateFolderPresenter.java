package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.spectralogic.ds3client.commands.spectrads3.PutBulkJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.PutBulkJobSpectraS3Response;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.LogType;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CreateFolderPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(CreateFolderPresenter.class);

    @FXML
    private TextField folderNameField;

    @FXML
    private Label labelText;

    @FXML
    private Button createFolderButton, cancelCreateFolderButton;

    @Inject
    private Workers workers;

    @Inject
    private CreateFolderModel createFolderModel;

    @Inject
    private ResourceBundle resourceBundle;

    @Inject
    DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            initGUIElements();
            folderNameField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.equals("")) {
                    createFolderButton.setDisable(true);
                } else {
                    createFolderButton.setDisable(false);
                }
            });

            folderNameField.setOnKeyReleased(event -> {
                if (!createFolderButton.isDisabled() && event.getCode().equals(KeyCode.ENTER)) {
                    createFolder();
                }
            });

        } catch (final Throwable e) {
            LOG.error("Encountered an error making the create folder presenter", e);
        }

    }

    private void initGUIElements() {
        labelText.setText(resourceBundle.getString("labelText"));
        createFolderButton.setText(resourceBundle.getString("createFolderButton"));
        cancelCreateFolderButton.setText(resourceBundle.getString("cancelCreateFolderButton"));
    }

    public void createFolder() {

        final Ds3Task createFolderTask = new Ds3Task(createFolderModel.getClient()) {

            @Override
            protected Object call() throws Exception {
                try {
                    String location = "";
                    if (!createFolderModel.getLocation().equals(createFolderModel.getBucketName())) {
                        location = createFolderModel.getLocation();
                    }
                    final List<Ds3Object> ds3ObjectList = new ArrayList<>();
                    final Ds3Object object = new Ds3Object(location + folderNameField.textProperty().getValue() + "/", 0);
                    ds3ObjectList.add(object);
                    final PutBulkJobSpectraS3Response response = getClient().putBulkJobSpectraS3(new PutBulkJobSpectraS3Request(createFolderModel.getBucketName(), ds3ObjectList));
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Create folder response code: " + response.getStatusCode(), LogType.SUCCESS);
                        deepStorageBrowserPresenter.logText("Folder is created", LogType.SUCCESS);
                    });
                    return response;
                } catch (final Exception e) {
                    LOG.error("Failed to delete files" + e);
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Failed to create folder. Reason: " + e.toString(), LogType.ERROR);
                    });
                    return null;
                }
            }
        };

        createFolderTask.setOnCancelled(this::handle);
        createFolderTask.setOnFailed(this::handle);
        createFolderTask.setOnSucceeded(this::handle);

        workers.execute(createFolderTask);
    }

    private void handle(final Event event) {
        closeDialog();
    }

    public void cancel() {
        LOG.info("Cancelling create folder");
        closeDialog();
    }

    private void closeDialog() {
        final Stage popupStage = (Stage) folderNameField.getScene().getWindow();
        popupStage.close();
    }
}
