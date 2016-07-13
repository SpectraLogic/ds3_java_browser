package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.spectralogic.ds3client.commands.spectrads3.PutBulkJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.PutBulkJobSpectraS3Response;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CreateFolderPresenter implements Initializable {

    private final static org.slf4j.Logger LOG = LoggerFactory.getLogger(CreateFolderPresenter.class);

    @FXML
    private TextField folderNameField;

    @FXML
    private Button createFolderButton;

    @FXML
    private Button cancel;

    @Inject
    private Workers workers;

    @Inject
    private CreateFolderModel createFolderModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
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
            LOG.error("Encountered an error making the delete file presenter", e);
        }

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
                    List<Ds3Object> ds3ObjectList = new ArrayList<>();
                    Ds3Object object = new Ds3Object(location + folderNameField.textProperty().getValue() + "/", 0);
                    ds3ObjectList.add(object);
                    PutBulkJobSpectraS3Response response = createFolderModel.getClient().putBulkJobSpectraS3(new PutBulkJobSpectraS3Request(createFolderModel.getBucketName(), ds3ObjectList));
                    System.out.println(response.getResult().getName());
                } catch (final Exception e) {
                    LOG.error("Failed to delete files" + e);
                }
                return null;
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
        LOG.info("Cancelling delete files");
        closeDialog();
    }

    private void closeDialog() {
        final Stage popupStage = (Stage) folderNameField.getScene().getWindow();
        popupStage.close();
    }
}
