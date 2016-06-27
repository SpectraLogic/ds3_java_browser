package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.spectralogic.ds3client.commands.spectrads3.PutBucketSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.PutBucketSpectraS3Response;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.security.SignatureException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;


/**
 * Created by Rahul on 6/16/2016.
 */
public class CreateBucketPresenter implements Initializable{
    private final static Logger LOG = LoggerFactory.getLogger(CreateBucketPresenter.class);

    @FXML
    TextField bucketNameField;

    @FXML
    ComboBox dataPolicyCombo;

    @FXML
    Button createBucketButton;

    @Inject
    Workers workers;

    @Inject
    CreateBucketWithDataPoliciesModel createBucketWithDataPoliciesModel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOG.info("Initializing Create Bucket form");
        dataPolicyCombo.getItems().addAll(createBucketWithDataPoliciesModel.getDataPolicies().stream().map(value -> value.getDataPolicy()).collect(Collectors.toList()));
        bucketNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                createBucketButton.setDisable(false);
            } else {
                createBucketButton.setDisable(true);
            }
        });

    }

    public void createBucket() {
        LOG.info("Create Bucket called");

        LOG.info("session name"+ createBucketWithDataPoliciesModel.getSession().getSessionName());
        final Ds3Task task = new Ds3Task(createBucketWithDataPoliciesModel.getSession().getClient()) {
            @Override
            protected Object call() throws Exception {
                try {
                   PutBucketSpectraS3Response response = getClient().putBucketSpectraS3(new PutBucketSpectraS3Request(bucketNameField.getText())) ;
                } catch (final IOException | SignatureException e) {
                    LOG.error("Failed to create bucket" + e);
                }
                return null;
            }
        };
        workers.execute(task);
        task.setOnSucceeded(event -> Platform.runLater(() -> {
            LOG.info("succeed");

        }));
    }

    public void cancelCreateBucket() {
        LOG.info("Cancel create bucket called");
        closeDialog();
    }

    private void closeDialog() {
        final Stage popupStage = (Stage) bucketNameField.getScene().getWindow();
        popupStage.close();
    }
}
