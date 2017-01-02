package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;

import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Response;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.util.CheckNetwork;
import com.spectralogic.dsbrowser.gui.util.ImageURLs;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

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


    public ButtonCell(final JobWorkers jobWorkers, final Workers workers, final EndpointInfo endpointInfo, final JobInterruptionStore jobInterruptionStore, final JobInfoPresenter jobInfoPresenter) {

        ALERT.setTitle(endpointInfo.getEndpoint());
        ALERT.setHeaderText(null);
        this.endpointInfo = endpointInfo;
        this.jobInterruptionStore = jobInterruptionStore;
        this.workers = workers;
        this.endpoints = jobInterruptionStore.getJobIdsModel().getEndpoints();
        this.jobInfoPresenter = jobInfoPresenter;

        recoverButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(final ActionEvent t) {
                if (CheckNetwork.isReachable(endpointInfo.getClient())) {
                    endpointInfo.getDeepStorageBrowserPresenter().logText("Initiating job recovery", LogType.INFO);
                    final String uuid = getTreeTableRow().getTreeItem().getValue().getJobId();
                    final FilesAndFolderMap filesAndFolderMap = endpointInfo.getJobIdAndFilesFoldersMap().get(uuid);
                    final RecoverInterruptedJob recoverInterruptedJob = new RecoverInterruptedJob(UUID.fromString(uuid), endpointInfo, jobInterruptionStore);
                    jobWorkers.execute(recoverInterruptedJob);
                    final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
                    ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
                    jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);

                    recoverInterruptedJob.setOnSucceeded(event -> {
                        ParseJobInterruptionMap.refreshCompleteTreeTableView(endpointInfo.getDs3Common(), workers);
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
                    ALERT.setContentText("Host " + endpointInfo.getClient().getConnectionDetails().getEndpoint() + " is unreachable. Please check your connection");
                    ALERT.showAndWait();
                    Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Unable to reach network", LogType.ERROR));
                }
            }
        });

        cancelButton.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(final ActionEvent t) {

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
                    ALERT.setContentText("Host " + endpointInfo.getClient().getConnectionDetails().getEndpoint() + "is unreachable. Please check your connection");
                    ALERT.showAndWait();
                    Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Unable to reach network", LogType.ERROR));
                }

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
