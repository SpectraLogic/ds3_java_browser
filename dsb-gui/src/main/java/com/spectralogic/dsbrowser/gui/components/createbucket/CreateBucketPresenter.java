/*
 * ****************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.spectralogic.ds3client.commands.spectrads3.PutBucketSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.PutBucketSpectraS3Response;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.ImageURLs;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Presenter
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

    @ModelContext
    private CreateBucketWithDataPoliciesModel createBucketWithDataPoliciesModel;

    private final Workers workers;
    private final ResourceBundle resourceBundle;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    @Inject
    public CreateBucketPresenter(final Workers workers, final ResourceBundle resourceBundle, final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        this.workers = workers;
        this.resourceBundle = resourceBundle;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        LOG.info("Initializing Create Bucket form");

        ALERT.setTitle("Error while creating bucket");

        final Stage stage = (Stage) ALERT.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEPSTORAGEBROWSER));
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
