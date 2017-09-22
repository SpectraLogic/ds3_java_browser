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

package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.DeleteService;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteBucketTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteFilesTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteFoldersTask;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.LazyAlert;
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

@Presenter
public class DeleteItemPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(DeleteItemPresenter.class);

    private final LazyAlert alert = new LazyAlert("Error");

    @FXML
    private TextField deleteField;

    @FXML
    private Button deleteButton;

    @FXML
    private Label deleteLabel, deleteConfirmationInfoLabel;

    @ModelContext
    private Ds3Task deleteTask;

    private final Workers workers;
    private final Ds3Common ds3Common;
    private final ResourceBundle resourceBundle;
    private final LoggingService loggingService;
    private final DateTimeUtils dateTimeUtils;

    @Inject
    public DeleteItemPresenter(final Workers workers,
                               final Ds3Common ds3Common,
                               final ResourceBundle resourceBundle,
                               final DateTimeUtils dateTimeUtils,
                               final LoggingService loggingService) {
        this.workers = workers;
        this.ds3Common = ds3Common;
        this.resourceBundle = resourceBundle;
        this.loggingService = loggingService;
        this.dateTimeUtils = dateTimeUtils;
    }

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

        } catch (final Throwable t) {
            LOG.error("Encountered an error initializing the DeleteItemPresenter", t);
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
            } else if (valueTreeItem.getValue().getType().equals(Ds3TreeTableValue.Type.Directory)) {
                deleteLabel.setText(resourceBundle.getString("deleteFolder"));
                deleteConfirmationInfoLabel.setText(resourceBundle.getString("deleteFolderInfo"));
            } else {
                deleteLabel.setText(resourceBundle.getString("deleteBucket"));
                deleteConfirmationInfoLabel.setText(resourceBundle.getString("deleteBucketInfo"));
            }
        }
    }

    public void deleteItems() {
        closeDialog();
        deleteTask.setOnCancelled(event -> constructMessageForLog());
        deleteTask.setOnFailed(event -> constructMessageForLog());
        deleteTask.setOnSucceeded(event -> {
            loggingService.logMessage(resourceBundle.getString("deleteSuccess"), LogType.SUCCESS);
            LOG.info("Successfully deleted selected item(s).");

            DeleteService.managePathIndicator(ds3Common, workers, dateTimeUtils, loggingService);
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
        final String message;
        String alertMessage = null;

        if (deleteTask instanceof Ds3DeleteBucketTask) {
            alertMessage = resourceBundle.getString("deleteBucketErr");
        } else if (deleteTask instanceof Ds3DeleteFoldersTask) {
            alertMessage = resourceBundle.getString("folderDeleteFailed");
        } else if (deleteTask instanceof Ds3DeleteFilesTask) {
            alertMessage = resourceBundle.getString("deleteFailedError");
        }
        message = alertMessage + StringConstants.SPACE + deleteTask.getErrorMsg();

        LOG.error("Failed to delete selected item(s): {}", message);
        loggingService.logMessage(message, LogType.ERROR);

        closeDialog();
        alert.showAlert(alertMessage);
    }
}
