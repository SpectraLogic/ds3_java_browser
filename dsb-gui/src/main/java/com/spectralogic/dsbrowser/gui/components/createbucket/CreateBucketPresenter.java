package com.spectralogic.dsbrowser.gui.components.createbucket;

import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateBucketTask;
import com.spectralogic.dsbrowser.gui.util.LazyAlert;
import com.spectralogic.dsbrowser.gui.util.RefreshCompleteViewWorker;
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

    private final LazyAlert alert = new LazyAlert("Error");

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
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final LoggingService loggingService;

    @Inject
    public CreateBucketPresenter(final Workers workers,
                                 final ResourceBundle resourceBundle,
                                 final Ds3Common ds3Common,
                                 final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
                                 final LoggingService loggingService) {
        this.workers = workers;
        this.resourceBundle = resourceBundle;
        this.ds3Common = ds3Common;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.loggingService = loggingService;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        LOG.info("Initializing Create Bucket form");
        initGUIElements();
        //noinspection unchecked
        dataPolicyCombo.getItems().addAll(createBucketWithDataPoliciesModel.getDataPolicies().stream().map(CreateBucketModel::getDataPolicy).collect(Collectors.toList()));
        bucketNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && (dataPolicyCombo.getValue()) != null) {
                createBucketButton.setDisable(false);
            } else {
                createBucketButton.setDisable(true);
            }
        });
        dataPolicyCombo.setOnAction(event -> {
            if (!bucketNameField.textProperty().getValue().isEmpty() && dataPolicyCombo.getValue() != null) {
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
            final Optional<CreateBucketModel> first = createBucketWithDataPoliciesModel.getDataPolicies().stream()
                    .filter(i -> i.getDataPolicy().equals(dataPolicyCombo.getValue())).findFirst();

            if (first.isPresent()) {
                final CreateBucketModel dataPolicy = first.get();

                final CreateBucketTask createBucketTask = new CreateBucketTask(dataPolicy,
                        createBucketWithDataPoliciesModel.getSession().getClient(),
                        bucketNameField.getText().trim(),
                        resourceBundle,
                        loggingService);
                workers.execute(createBucketTask);
                createBucketTask.setOnSucceeded(event -> {
                    LOG.info("Created bucket [{}]", bucketNameField.getText().trim());
                    loggingService.logMessage(resourceBundle.getString("bucketCreated"), LogType.SUCCESS);
                    Platform.runLater(() -> {
                        ds3Common.getDs3TreeTableView().setRoot(new TreeItem<>());
                        RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers, loggingService);
                        closeDialog();
                    });
                });
                createBucketTask.setOnFailed(event -> {
                    alert.showAlert(resourceBundle.getString("createBucketErrorAlert"));
                });
            } else {
                LOG.info("Data policy not found");
                loggingService.logMessage(resourceBundle.getString("dataPolicyNotFoundErr"), LogType.INFO);
                alert.showAlert(resourceBundle.getString("dataPolicyNotFoundErr"));
            }


        } catch (final Exception e) {
            LOG.error("Failed to create bucket", e);
            loggingService.logMessage(resourceBundle.getString("createBucketFailedErr") + e, LogType.ERROR);
            alert.showAlert(resourceBundle.getString("createBucketErrorAlert"));
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
