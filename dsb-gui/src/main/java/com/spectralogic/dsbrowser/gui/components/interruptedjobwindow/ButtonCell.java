package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;

import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.BackgroundTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3CancelSingleJobTask;
import com.spectralogic.dsbrowser.gui.services.tasks.RecoverInterruptedJob;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

public class ButtonCell extends TreeTableCell<JobInfoModel, Boolean> {
    private final static Logger LOG = LoggerFactory.getLogger(ButtonCell.class);
    private final Button recoverButton = new Button();
    private final Button cancelButton = new Button();
    private final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();
    private final HBox hbox = createHBox();


    public ButtonCell(final JobWorkers jobWorkers, final Workers workers, final EndpointInfo endpointInfo, final JobInterruptionStore jobInterruptionStore, final JobInfoPresenter jobInfoPresenter, final SettingsStore settingsStore) {
        recoverButton.setOnAction(recoverEvent -> {
            LOG.info("Recover Job button clicked");
            if (CheckNetwork.isReachable(endpointInfo.getClient())) {
                endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("initiatingRecovery"), LogType.INFO);
                final String uuid = getTreeTableRow().getTreeItem().getValue().getJobId();
                final FilesAndFolderMap filesAndFolderMap = endpointInfo.getJobIdAndFilesFoldersMap().get(uuid);
                final RecoverInterruptedJob recoverInterruptedJob = new RecoverInterruptedJob(UUID.fromString(uuid), endpointInfo, jobInterruptionStore, settingsStore.getShowCachedJobSettings().getShowCachedJob());
                jobWorkers.execute(recoverInterruptedJob);
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
                jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);
                recoverInterruptedJob.setOnSucceeded(event -> {
                    RefreshCompleteViewWorker.refreshCompleteTreeTableView(endpointInfo.getDs3Common(), workers);
                    jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);
                });
                recoverInterruptedJob.setOnFailed(event -> {
                    endpointInfo.getDeepStorageBrowserPresenter().logText("Failed to recover " + filesAndFolderMap.getType() + " job " + endpointInfo.getEndpoint(), LogType.ERROR);
                    jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);
                });
                recoverInterruptedJob.setOnCancelled(event -> {
                    if (CheckNetwork.isReachable(endpointInfo.getClient())) {
                        final Ds3CancelSingleJobTask ds3CancelSingleJobTask = new Ds3CancelSingleJobTask(uuid, endpointInfo, jobInterruptionStore, resourceBundle.getString("recover"));
                        workers.execute(ds3CancelSingleJobTask);
                        ds3CancelSingleJobTask.setOnSucceeded(eventCancel -> {
                                    LOG.info("Cancellation of recovered job success");
                                    jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);
                                }
                        );

                    } else {
                        BackgroundTask.dumpTheStack(resourceBundle.getString("host") + endpointInfo.getClient().getConnectionDetails().getEndpoint() + StringConstants.SPACE + resourceBundle.getString("unreachable"));
                        Ds3Alert.show(endpointInfo.getEndpoint(), resourceBundle.getString("host") + endpointInfo.getClient().getConnectionDetails().getEndpoint() + StringConstants.SPACE + resourceBundle.getString("unreachable"), Alert.AlertType.INFORMATION);
                        endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("unableToReachNetwork"), LogType.ERROR);
                    }
                });

            } else {
                BackgroundTask.dumpTheStack(resourceBundle.getString("host") + StringConstants.SPACE + endpointInfo.getClient().getConnectionDetails().getEndpoint() + StringConstants.SPACE + resourceBundle.getString("unreachable"));
                Ds3Alert.show(endpointInfo.getEndpoint(), resourceBundle.getString("host") + StringConstants.SPACE + endpointInfo.getClient().getConnectionDetails().getEndpoint() + StringConstants.SPACE + resourceBundle.getString("unreachable"), Alert.AlertType.INFORMATION);
                endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("unableToReachNetwork"), LogType.ERROR);
            }
        });
        cancelButton.setOnAction(t -> {
            if (CheckNetwork.isReachable(endpointInfo.getClient())) {
                final String uuid = getTreeTableRow().getTreeItem().getValue().getJobId();
                final Ds3CancelSingleJobTask ds3CancelSingleJobTask = new Ds3CancelSingleJobTask(uuid, endpointInfo, jobInterruptionStore, resourceBundle.getString("recover"));
                workers.execute(ds3CancelSingleJobTask);
                ds3CancelSingleJobTask.setOnSucceeded(event -> {
                            LOG.info("Cancellation of interrupted job failed");
                            jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);
                        }
                );

            } else {
                BackgroundTask.dumpTheStack(resourceBundle.getString("host") + endpointInfo.getClient().getConnectionDetails().getEndpoint() + StringConstants.SPACE + resourceBundle.getString("unreachable"));
                Ds3Alert.show(endpointInfo.getEndpoint(), resourceBundle.getString("host") + endpointInfo.getClient().getConnectionDetails().getEndpoint() + StringConstants.SPACE + resourceBundle.getString("unreachable"), Alert.AlertType.INFORMATION);
                endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("unableToReachNetwork"), LogType.ERROR);
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

    private HBox createHBox() {
        final HBox hbox = new HBox();
        final ImageView recoverImageView = new ImageView(ImageURLs.RECOVER_IMAGE);
        recoverImageView.setFitHeight(15);
        recoverImageView.setFitWidth(15);
        recoverButton.setGraphic(recoverImageView);
        recoverButton.setStyle("-fx-background-color: transparent;");
        recoverButton.setTooltip(new Tooltip(resourceBundle.getString("recoverJob")));
        final ImageView cancelImageView = new ImageView(ImageURLs.CANCEL_RECOVER);
        cancelImageView.setFitHeight(15);
        cancelImageView.setFitWidth(15);
        cancelButton.setGraphic(cancelImageView);
        cancelButton.setStyle("-fx-background-color: transparent;");
        cancelButton.setTooltip(new Tooltip(resourceBundle.getString("cancelJob")));
        hbox.setSpacing(3.0);
        hbox.setAlignment(Pos.CENTER);
        hbox.getChildren().addAll(recoverButton, cancelButton);
        return hbox;
    }
}
