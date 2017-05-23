package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateFolderTask;
import com.spectralogic.dsbrowser.gui.util.Ds3Alert;
import com.spectralogic.dsbrowser.gui.util.PathUtil;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

@Presenter
public class CreateFolderPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(CreateFolderPresenter.class);

    @FXML
    private TextField folderNameField;

    @FXML
    private Label labelText;

    @FXML
    private Button createFolderButton, cancelCreateFolderButton;

    @ModelContext
    private CreateFolderModel createFolderModel;

    private final Workers workers;
    private final ResourceBundle resourceBundle;
    private final LoggingService loggingService;

    @Inject
    public CreateFolderPresenter(final Workers workers,
                                 final ResourceBundle resourceBundle,
                                 final LoggingService loggingService) {
        this.workers = workers;
        this.resourceBundle = resourceBundle;
        this.loggingService = loggingService;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initGUIElements();
            folderNameField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.equals(StringConstants.EMPTY_STRING)) {
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

        } catch (final Exception e) {
            LOG.error("Encountered an error making the create folder presenter", e);
        }
    }

    private void initGUIElements() {
        labelText.setText(resourceBundle.getString("labelText"));
        createFolderButton.setText(resourceBundle.getString("createFolderButton"));
        cancelCreateFolderButton.setText(resourceBundle.getString("cancelCreateFolderButton"));
    }

    public void createFolder() {
        try {
            final String location = PathUtil.getFolderLocation(createFolderModel.getLocation(), createFolderModel
                    .getBucketName());
            //Instantiating create folder task
            final CreateFolderTask createFolderTask = new CreateFolderTask(createFolderModel.getClient(),
                    createFolderModel, folderNameField.textProperty().getValue(),
                    PathUtil.getDs3ObjectList(location, folderNameField.textProperty().getValue()),
                    loggingService, resourceBundle);
            workers.execute(createFolderTask);
            //Handling task actions
            createFolderTask.setOnSucceeded(event -> {
                this.closeDialog();
                loggingService.logMessage(folderNameField.textProperty().getValue() + StringConstants.SPACE
                        + resourceBundle.getString("folderCreated"), LogType.SUCCESS);
            });
            createFolderTask.setOnCancelled(event -> this.closeDialog());
            createFolderTask.setOnFailed(event -> {
                this.closeDialog();
                Ds3Alert.show(resourceBundle.getString("createFolderErrAlert"), resourceBundle.getString("createFolderErrLogs"), Alert.AlertType.ERROR);
            });
        } catch (final Exception e) {
            LOG.error("Failed to create folder", e);
            loggingService.logMessage(resourceBundle.getString("createFolderErr") + StringConstants.SPACE
                    + folderNameField.textProperty().getValue().trim() + StringConstants.SPACE
                    + resourceBundle.getString("txtReason") + StringConstants.SPACE + e, LogType.ERROR);
            Ds3Alert.show(resourceBundle.getString("createFolderErrAlert"), resourceBundle.getString("createFolderErrLogs"), Alert.AlertType.ERROR);
        }
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
