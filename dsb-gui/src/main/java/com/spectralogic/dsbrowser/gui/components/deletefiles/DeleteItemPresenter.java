package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.DeleteService;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteBucketTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteFilesTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteFolderTask;
import com.spectralogic.dsbrowser.gui.util.Ds3Alert;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class DeleteItemPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(DeleteItemPresenter.class);

    @FXML
    private TextField deleteField;

    @FXML
    private Button deleteButton;

    @FXML
    private Label deleteLabel, deleteConfirmationInfoLabel;

    @Inject
    private Workers workers;

    @Inject
    private Ds3Task deleteTask;

    @Inject
    private Ds3Common ds3Common;

    @Inject
    private ResourceBundle resourceBundle;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            deleteButton.setDisable(true);
            ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = null;
            if (ds3Common.getDs3TreeTableView() != null) {
                selectedItems = ds3Common
                        .getDs3TreeTableView().getSelectionModel().getSelectedItems();
            }
            callToChangeLabelText(selectedItems);
            deleteField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.equals(StringConstants.DELETE)) {
                    deleteButton.setDisable(false);
                } else {
                    deleteButton.setDisable(true);
                }
            });
            deleteField.setOnKeyReleased(event -> {
                if (!deleteButton.isDisabled() && event.getCode().equals(KeyCode.ENTER)) {
                    deleteItems();
                }
            });

        } catch (final Exception e) {
            LOG.error("Encountered an error making the delete file presenter", e);
        }
    }

    private void callToChangeLabelText(ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems) {
        if (Guard.isNullOrEmpty(selectedItems)) {
            selectedItems = FXCollections.observableArrayList();
            selectedItems.add(ds3Common.getDs3TreeTableView().getRoot());
        }
        changeLabelText(selectedItems);
    }

    private void changeLabelText(final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems) {
        final Optional<TreeItem<Ds3TreeTableValue>> first = selectedItems.stream().findFirst();
        if(first.isPresent()) {
            final TreeItem<Ds3TreeTableValue> valueTreeItem = first.get();
            if (valueTreeItem.getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
                deleteLabel.setText(resourceBundle.getString("deleteFiles"));
                deleteConfirmationInfoLabel.setText(resourceBundle.getString("deleteFileInfo"));
            } else if (null != valueTreeItem && valueTreeItem.getValue().getType().equals(Ds3TreeTableValue.Type.Directory)) {
                deleteLabel.setText(resourceBundle.getString("deleteFolder"));
                deleteConfirmationInfoLabel.setText(resourceBundle.getString("deleteFolderInfo"));
            } else {
                deleteLabel.setText(resourceBundle.getString("deleteBucket"));
                deleteConfirmationInfoLabel.setText(resourceBundle.getString("deleteBucketInfo"));
            }
        }
    }

    public void deleteItems() {

        deleteTask.setOnCancelled(event -> constructMessageForLog());
        deleteTask.setOnFailed(event -> constructMessageForLog());
        deleteTask.setOnSucceeded(event -> {
            DeleteService.managePathIndicator(ds3Common, workers);
            printLog(LogType.SUCCESS, resourceBundle.getString("deleteSuccess"), null);
        });
        workers.execute(deleteTask);
    }

    public void cancelDelete() {
        LOG.info("Cancelling delete files");
        closeDialog();
    }

    private void closeDialog() {
        final Stage popupStage = (Stage) deleteField.getScene().getWindow();
        popupStage.close();
    }

    private void constructMessageForLog() {
        String message = null;
        String alertMessage = null;

        if (deleteTask instanceof Ds3DeleteBucketTask) {
            alertMessage = resourceBundle.getString("deleteBucketErr");
            message = alertMessage + StringConstants.SPACE
                    + ((Ds3DeleteBucketTask) deleteTask).getErrorMsg();
        } else if (deleteTask instanceof Ds3DeleteFolderTask) {
            alertMessage = resourceBundle.getString("folderDeleteFailed");
            message = alertMessage + StringConstants.SPACE
                    + ((Ds3DeleteFolderTask) deleteTask).getErrorMsg();
        } else if (deleteTask instanceof Ds3DeleteFilesTask) {
            alertMessage = resourceBundle.getString("deleteFailedError");
            message = alertMessage + StringConstants.SPACE
                    + ((Ds3DeleteFilesTask) deleteTask).getErrorMsg();
        }

        printLog(LogType.ERROR, message, alertMessage);
    }

    private void printLog(final LogType type, final String message, final String alertMessage) {
        ds3Common.getDeepStorageBrowserPresenter().logText(message, type);
        closeDialog();
        if (type.equals(LogType.ERROR)) {
            LOG.error("Failed to delete selected item(s) ", message);
            Ds3Alert.show(null, alertMessage, Alert.AlertType.INFORMATION);
        } else {
            LOG.error("Success to delete selected item(s):{} ", message);
        }
    }
}
