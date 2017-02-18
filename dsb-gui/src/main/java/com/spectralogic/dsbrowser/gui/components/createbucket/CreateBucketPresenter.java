package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateBucketTask;
import com.spectralogic.dsbrowser.gui.util.ImageURLs;
import com.spectralogic.dsbrowser.gui.util.LogType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.spectralogic.dsbrowser.gui.util.RefreshCompleteViewWorker;

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

    @Inject
    private Ds3Common ds3Common;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        LOG.info("Initializing Create Bucket form");
        ALERT.setHeaderText(null);
        ALERT.setTitle(resourceBundle.getString("createBucketError"));
        final Stage stage = (Stage) ALERT.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEP_STORAGE_BROWSER));
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
        try {
            final CreateBucketModel dataPolicy = createBucketWithDataPoliciesModel.getDataPolicies().stream()
                    .filter(i -> i.getDataPolicy().equals(dataPolicyCombo.getValue())).findFirst().orElse(null);
            if (null != dataPolicy) {
                final CreateBucketTask createBucketTask = new CreateBucketTask(dataPolicy,
                        createBucketWithDataPoliciesModel.getSession().getClient(), bucketNameField.getText().trim(),
                        deepStorageBrowserPresenter);
                workers.execute(createBucketTask);
                createBucketTask.setOnSucceeded(event -> Platform.runLater(() -> {
                    LOG.info("Bucket is created");
                    deepStorageBrowserPresenter.logText(resourceBundle.getString("bucketCreated"), LogType.SUCCESS);
                    ds3Common.getDs3TreeTableView().setRoot(new TreeItem<>());
                    RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers);
                    closeDialog();
                }));
                createBucketTask.setOnFailed(event -> {
                    Platform.runLater(() -> {
                        ALERT.setContentText(resourceBundle.getString("createBucketErrorAlert"));
                        ALERT.showAndWait();
                    });
                });
            } else {
                LOG.info("Data policy not found");
                Platform.runLater(() -> {
                    deepStorageBrowserPresenter.logText(resourceBundle.getString("dataPolicyNotFoundErr"), LogType.INFO);
                    ALERT.setContentText(resourceBundle.getString("dataPolicyNotFoundErr"));
                    ALERT.showAndWait();
                });
            }

        } catch (final Exception e) {
            LOG.error("Failed to create bucket", e);
            Platform.runLater(() -> {
                deepStorageBrowserPresenter.logText(resourceBundle.getString("createBucketFailedErr") + e, LogType.ERROR);
                ALERT.setContentText(resourceBundle.getString("createBucketFailedErrAlert"));
                ALERT.showAndWait();
            });
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
