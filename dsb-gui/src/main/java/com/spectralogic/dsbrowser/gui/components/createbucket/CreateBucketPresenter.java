package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.spectralogic.ds3client.commands.spectrads3.PutBucketSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.PutBucketSpectraS3Response;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.LogType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CreateBucketPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(CreateBucketPresenter.class);

    private final Alert ALERT = new Alert(Alert.AlertType.INFORMATION);

    @FXML
    private TextField bucketNameField;

    @FXML
    private ComboBox dataPolicyCombo;

    @FXML
    private Label dataPolicyComboLabel, bucketNameFieldLabel;

    @FXML
    private Button createBucketButton;

    @Inject
    private Workers workers;

    @Inject
    private CreateBucketWithDataPoliciesModel createBucketWithDataPoliciesModel;

    @Inject
    private ResourceBundle resourceBundle;

    @Inject
    private DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    @Inject
    private Ds3PanelPresenter ds3PanelPresenter;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        LOG.info("Initializing Create Bucket form");

        ALERT.setTitle("Error while creating bucket");
        ALERT.setHeaderText(null);

        initGUIElements();

        //noinspection unchecked
        dataPolicyCombo.getItems().addAll(createBucketWithDataPoliciesModel.getDataPolicies().stream().map(value -> value.getDataPolicy()).collect(Collectors.toList()));

        bucketNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && (dataPolicyCombo.getValue()) != null) {
                createBucketButton.setDisable(false);
            } else {
                createBucketButton.setDisable(true);
            }
        });

        dataPolicyCombo.setOnAction(event -> {
            if (!bucketNameField.textProperty().getValue().isEmpty() && ((String) dataPolicyCombo.getValue()) != null) {
                createBucketButton.setDisable(false);
            } else {
                createBucketButton.setDisable(true);
            }
        });
    }

    private void initGUIElements() {
        bucketNameFieldLabel.setText(resourceBundle.getString("bucketNameFieldLabel"));
        dataPolicyComboLabel.setText(resourceBundle.getString("dataPolicyComboLabel"));
    }

    /**
     * Method to create bucket on blackpearl
     */
    public void createBucket() {
        LOG.info("Create Bucket called");

        final Ds3Task task = new Ds3Task(createBucketWithDataPoliciesModel.getSession().getClient()) {
            @Override
            protected Object call() throws Exception {
                try {
                    final CreateBucketModel dataPolicy = createBucketWithDataPoliciesModel.getDataPolicies().stream().filter(i -> i.getDataPolicy().equals(dataPolicyCombo.getValue())).findFirst().get();
                    final PutBucketSpectraS3Response response = getClient().putBucketSpectraS3(new PutBucketSpectraS3Request(bucketNameField.getText().trim()).withDataPolicyId(dataPolicy.getId()));
                    Platform.runLater(() -> {
                      //  ds3PanelPresenter.disableSearch(false);
                        //deepStorageBrowserPresenter.logText("Create bucket status code: " + response.getResponse().getStatusCode(), LogType.SUCCESS);
                        deepStorageBrowserPresenter.logText("Bucket is created.", LogType.SUCCESS);
                    });
                    return response;
                } catch (final IOException e) {
                    LOG.error("Failed to create bucket" + e);
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Failed to create bucket" + e.toString(), LogType.ERROR);
                        ALERT.setContentText("Failed to create bucket. Check Logs");
                        ALERT.showAndWait();
                    });
                    return null;
                }
            }
        };
        workers.execute(task);
        task.setOnSucceeded(event -> Platform.runLater(() -> {
            closeDialog();
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
