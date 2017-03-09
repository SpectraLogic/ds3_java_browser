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

package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;

import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Response;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class ButtonCell extends TreeTableCell<JobInfoModel, Boolean> {

    private final static Alert ALERT = new Alert(Alert.AlertType.INFORMATION);
    private final Button recoverButton = new Button();
    private final Button cancelButton = new Button();
    final HBox hbox = createHBox();
    private final EndpointInfo endpointInfo;
    private final JobInterruptionStore jobInterruptionStore;
    private final Workers workers;
    private final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints;
    private final JobInfoPresenter jobInfoPresenter;
    private final LoggingService loggingService;


    public ButtonCell(final JobWorkers jobWorkers, final Workers workers, final EndpointInfo endpointInfo, final JobInterruptionStore jobInterruptionStore, final JobInfoPresenter jobInfoPresenter, final LoggingService loggingService) {

        ALERT.setTitle(endpointInfo.getEndpoint());
        ALERT.setHeaderText(null);
        final Stage stage = (Stage) ALERT.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEPSTORAGEBROWSER));
        this.endpointInfo = endpointInfo;
        this.jobInterruptionStore = jobInterruptionStore;
        this.workers = workers;
        this.endpoints = jobInterruptionStore.getJobIdsModel().getEndpoints();
        this.jobInfoPresenter = jobInfoPresenter;
        this.loggingService = loggingService;

        recoverButton.setOnAction(t -> {
            if (CheckNetwork.isReachable(endpointInfo.getClient())) {
                endpointInfo.getDeepStorageBrowserPresenter().logText("Initiating job recovery", LogType.INFO);
                final String uuid = getTreeTableRow().getTreeItem().getValue().getJobId();
                final FilesAndFolderMap filesAndFolderMap = endpointInfo.getJobIdAndFilesFoldersMap().get(uuid);
                final RecoverInterruptedJob recoverInterruptedJob = new RecoverInterruptedJob(UUID.fromString(uuid),
                        endpointInfo, jobInterruptionStore, jobInfoPresenter, getTreeTableView());
                jobWorkers.execute(recoverInterruptedJob);

                recoverInterruptedJob.setOnSucceeded(event -> {
                    ParseJobInterruptionMap.refreshCompleteTreeTableView(endpointInfo.getDs3Common(), workers, loggingService);
                    jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);
                });

                recoverInterruptedJob.setOnFailed(event -> {
                    Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Failed to recover " + filesAndFolderMap.getType() + " job " + endpointInfo.getEndpoint(), LogType.ERROR));
                    jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);
                });

                recoverInterruptedJob.setOnCancelled(event -> {
                    try {
                        final CancelJobSpectraS3Response cancelJobSpectraS3Response = endpointInfo.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(uuid));
                        Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Cancel job status: " + cancelJobSpectraS3Response, LogType.SUCCESS));
                    } catch (final IOException e1) {
                        if (!(e1 instanceof FailedRequestException)) {
                            Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Failed to cancel job: " + e1.toString(), LogType.ERROR));
                        }
                    } finally {
                        final Map<String, FilesAndFolderMap> jobIDMapSec = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, uuid, endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter());
                        ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMapSec, endpointInfo.getDeepStorageBrowserPresenter());
                    }
                });

            } else {
                ErrorUtils.dumpTheStack("Host " + endpointInfo.getClient().getConnectionDetails().getEndpoint() + " is unreachable. Please check your connection");
                ALERT.setContentText("Host " + endpointInfo.getClient().getConnectionDetails().getEndpoint() + " is unreachable. Please check your connection");
                ALERT.showAndWait();
                Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Unable to reach network", LogType.ERROR));
            }
        });

        cancelButton.setOnAction(t -> {

            if (CheckNetwork.isReachable(endpointInfo.getClient())) {
                final String uuid = getTreeTableRow().getTreeItem().getValue().getJobId();
                try {
                    final CancelJobSpectraS3Response cancelJobSpectraS3Response = endpointInfo.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(uuid));
                    Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Cancel job status: " + cancelJobSpectraS3Response, LogType.SUCCESS));
                } catch (final IOException e1) {
                    Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Failed to cancel job: " + e1.toString(), LogType.ERROR));
                } finally {
                    final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, uuid, endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter());
                    ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
                    jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);
                }
            } else {
                ErrorUtils.dumpTheStack("Host " + endpointInfo.getClient().getConnectionDetails().getEndpoint() + " is unreachable. Please check your connection");
                ALERT.setContentText("Host " + endpointInfo.getClient().getConnectionDetails().getEndpoint() + "is unreachable. Please check your connection");
                ALERT.showAndWait();
                Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Unable to reach network", LogType.ERROR));
            }

        });
    }


    //Display button if the row is not empty
    @Override
    protected void updateItem(final Boolean t, final boolean empty) {
        super.updateItem(t, empty);
        if (getTreeTableRow().getTreeItem() != null) {
            if (getTreeTableRow().getTreeItem().getValue().getType().equals(JobInfoModel.Type.JOBID)) {
                setGraphic(hbox);
            } else {
                setGraphic(null);
            }
        } else {
            setGraphic(null);
        }
    }

    public HBox createHBox() {
        final HBox hbox = new HBox();

        final ImageView recoverImageView = new ImageView(ImageURLs.RECOVERIMAGE);
        recoverImageView.setFitHeight(15);
        recoverImageView.setFitWidth(15);

        recoverButton.setGraphic(recoverImageView);
        recoverButton.setStyle("-fx-background-color: transparent;");
        recoverButton.setTooltip(new Tooltip("Recover job"));

        final ImageView cancelImageView = new ImageView(ImageURLs.CANCELRECOVER);
        cancelImageView.setFitHeight(15);
        cancelImageView.setFitWidth(15);
        cancelButton.setGraphic(cancelImageView);
        cancelButton.setStyle("-fx-background-color: transparent;");
        cancelButton.setTooltip(new Tooltip("Cancel job"));

        hbox.setSpacing(3.0);
        hbox.setAlignment(Pos.CENTER);
        hbox.getChildren().addAll(recoverButton, cancelButton);

        return hbox;
    }
}
