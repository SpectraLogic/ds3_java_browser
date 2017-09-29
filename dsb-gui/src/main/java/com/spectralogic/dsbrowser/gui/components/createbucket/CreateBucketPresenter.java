/*
 * ******************************************************************************
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
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateBucketTask;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.LazyAlert;
import com.spectralogic.dsbrowser.gui.util.RefreshCompleteViewWorker;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Presenter
public class CreateBucketPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(CreateBucketPresenter.class);
    private static final String CREATE_BUCKET_ERROR_ALERT = "createBucketErrorAlert";
    private static final String DATA_POLICY_NOT_FOUND_ERR = "dataPolicyNotFoundErr";

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
    private final Ds3Common ds3Common;
    private final LoggingService loggingService;
    private final DateTimeUtils dateTimeUtils;
    private final LazyAlert alert;

    @Inject
    public CreateBucketPresenter(final Workers workers,
                                 final ResourceBundle resourceBundle,
                                 final Ds3Common ds3Common,
                                 final DateTimeUtils dateTimeUtils,
                                 final LoggingService loggingService) {
        this.workers = workers;
        this.resourceBundle = resourceBundle;
        this.ds3Common = ds3Common;
        this.dateTimeUtils = dateTimeUtils;
        this.loggingService = loggingService;
        this.alert = new LazyAlert(resourceBundle);
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Initializing Create Bucket form");
            initGUIElements();

            //noinspection unchecked
            dataPolicyCombo.getItems().addAll(createBucketWithDataPoliciesModel.getDataPolicies().stream()
                    .map(CreateBucketModel::getDataPolicy).collect(Collectors.toList()));
            bucketNameField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.isEmpty() && (dataPolicyCombo.getValue()) != null) {
                    createBucketButton.setDisable(false);
                } else {
                    createBucketButton.setDisable(true);
                }
            });
            dataPolicyCombo.setOnAction(SafeHandler.logHandle(event -> {
                if (!bucketNameField.textProperty().getValue().isEmpty() && dataPolicyCombo.getValue() != null) {
                    createBucketButton.setDisable(false);
                } else {
                    createBucketButton.setDisable(true);
                }
            }));
        } catch (final Throwable t) {
            LOG.error("Encountered an error initializing the CreateBucketPresenter", t);
        }
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
        try {
            final Optional<CreateBucketModel> first = createBucketWithDataPoliciesModel.getDataPolicies().stream()
                    .filter(i -> i.getDataPolicy().equals(dataPolicyCombo.getValue())).findFirst();

            if (first.isPresent()) {
                final CreateBucketModel dataPolicy = first.get();

                final CreateBucketTask createBucketTask = new CreateBucketTask(dataPolicy,
                        createBucketWithDataPoliciesModel.getSession().getClient(),
                        bucketNameField.getText().trim(),
                        resourceBundle,
                        loggingService);
                createBucketTask.setOnSucceeded(SafeHandler.logHandle(event -> {
                    LOG.info("Created bucket [{}]", bucketNameField.getText().trim());
                    loggingService.logMessage(resourceBundle.getString("bucketCreated"), LogType.SUCCESS);
                    Platform.runLater(() -> {
                        ds3Common.getDs3TreeTableView().setRoot(new TreeItem<>());
                        RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers, dateTimeUtils, loggingService);
                        closeDialog();
                    });
                }));
                createBucketTask.setOnFailed(SafeHandler.logHandle(event -> {
                    alert.error(CREATE_BUCKET_ERROR_ALERT);
                }));
                workers.execute(createBucketTask);
            } else {
                LOG.info("Data policy not found");
                loggingService.logMessage(resourceBundle.getString(DATA_POLICY_NOT_FOUND_ERR), LogType.INFO);
                alert.error(DATA_POLICY_NOT_FOUND_ERR);
            }


        } catch (final Exception e) {
            LOG.error("Failed to create bucket", e);
            loggingService.logMessage(resourceBundle.getString("createBucketFailedErr") + e, LogType.ERROR);
            alert.error(CREATE_BUCKET_ERROR_ALERT);
        }
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
