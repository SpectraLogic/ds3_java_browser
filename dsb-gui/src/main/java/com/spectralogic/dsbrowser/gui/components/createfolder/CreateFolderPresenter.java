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

package com.spectralogic.dsbrowser.gui.components.createfolder;

import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateFolderTask;
import com.spectralogic.dsbrowser.gui.util.LazyAlert;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
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
    private static final String CREATE_FOLDER_ERR_LOGS = "createFolderErrLogs";

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
    private final LazyAlert alert;

    @Inject
    public CreateFolderPresenter(final Workers workers,
                                 final ResourceBundle resourceBundle,
                                 final LoggingService loggingService) {
        this.workers = workers;
        this.resourceBundle = resourceBundle;
        this.loggingService = loggingService;
        this.alert = new LazyAlert(resourceBundle);
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
            folderNameField.setOnKeyReleased(SafeHandler.logHandle(event -> {
                if (!createFolderButton.isDisabled() && event.getCode().equals(KeyCode.ENTER)) {
                    createFolder();
                }
            }));
        } catch (final Throwable t) {
            LOG.error("Encountered an error initializing the CreateFolderPresenter", t);
        }
    }

    private void initGUIElements() {
        labelText.setText(resourceBundle.getString("labelText"));
        createFolderButton.setText(resourceBundle.getString("createFolderButton"));
        cancelCreateFolderButton.setText(resourceBundle.getString("cancelCreateFolderButton"));
    }

    public void createFolder() {
        //Instantiating create folder task
        final CreateFolderTask createFolderTask = new CreateFolderTask(createFolderModel.getClient(),
                createFolderModel.getBucketName().trim(), folderNameField.textProperty().getValue().trim(),
                loggingService, resourceBundle);
        //Handling task actions
        createFolderTask.setOnSucceeded(SafeHandler.logHandle(event -> {
            this.closeDialog();
            loggingService.logMessage(folderNameField.textProperty().getValue() + StringConstants.SPACE
                    + resourceBundle.getString("folderCreated"), LogType.SUCCESS);
        }));
        createFolderTask.setOnCancelled(SafeHandler.logHandle(event -> this.closeDialog()));
        createFolderTask.setOnFailed(SafeHandler.logHandle(event -> {
            alert.error(CREATE_FOLDER_ERR_LOGS);
            this.closeDialog();
        }));
        workers.execute(createFolderTask);
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
