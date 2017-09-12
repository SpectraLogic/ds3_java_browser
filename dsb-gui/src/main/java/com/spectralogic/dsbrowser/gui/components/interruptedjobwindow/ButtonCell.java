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

package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3CancelSingleJobTask;
import com.spectralogic.dsbrowser.gui.services.tasks.RecoverInterruptedJob;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.geometry.Pos;
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
    private final LazyAlert alert = new LazyAlert("Error");
    private final RecoverInterruptedJob.RecoverInterruptedJobFactory recoverInterruptedJobFactory;

    @Inject
    public ButtonCell(final JobWorkers jobWorkers,
                      final Workers workers,
                      @Assisted final EndpointInfo endpointInfo,
                      final JobInterruptionStore jobInterruptionStore,
                      final JobInfoPresenter jobInfoPresenter,
                      final LoggingService loggingService,
                      final RecoverInterruptedJob.RecoverInterruptedJobFactory recoverInterruptedJobFactory) {
        this.recoverInterruptedJobFactory = recoverInterruptedJobFactory;
        recoverButton.setOnAction(recoverEvent -> {
            LOG.info("Recover Job button clicked");
            if (CheckNetwork.isReachable(endpointInfo.getClient())) {
                loggingService.logMessage(resourceBundle.getString("initiatingRecovery"), LogType.INFO);
                final String uuid = getTreeTableRow().getTreeItem().getValue().getJobId();
                final FilesAndFolderMap filesAndFolderMap = endpointInfo.getJobIdAndFilesFoldersMap().get(uuid);

                final RecoverInterruptedJob recoverInterruptedJob = recoverInterruptedJobFactory.createRecoverInterruptedJob(UUID.fromString(uuid), endpointInfo);
                jobWorkers.execute(recoverInterruptedJob);

                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
                jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);

                recoverInterruptedJob.setOnSucceeded(event -> {
                    RefreshCompleteViewWorker.refreshCompleteTreeTableView(endpointInfo.getDs3Common(), workers, loggingService);
                    jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);
                });
                recoverInterruptedJob.setOnFailed(event -> {
                    loggingService.logMessage("Failed to recover " + filesAndFolderMap.getType() + " job " + endpointInfo.getEndpoint(), LogType.ERROR);
                    jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);
                });
                recoverInterruptedJob.setOnCancelled(event -> {
                    if (CheckNetwork.isReachable(endpointInfo.getClient())) {
                        final Ds3CancelSingleJobTask ds3CancelSingleJobTask = new Ds3CancelSingleJobTask(uuid, endpointInfo, jobInterruptionStore, resourceBundle.getString("recover"), loggingService);
                        workers.execute(ds3CancelSingleJobTask);
                        ds3CancelSingleJobTask.setOnSucceeded(eventCancel -> {
                                    LOG.info("Cancellation of recovered job success");
                                    jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);
                                }
                        );

                    } else {
                        ErrorUtils.dumpTheStack(resourceBundle.getString("host") + endpointInfo.getClient().getConnectionDetails().getEndpoint() + StringConstants.SPACE + resourceBundle.getString("unreachable"));
                        alert.showAlert(resourceBundle.getString("host") + endpointInfo.getClient().getConnectionDetails().getEndpoint() + StringConstants.SPACE + resourceBundle.getString("unreachable"));
                        loggingService.logMessage(resourceBundle.getString("unableToReachNetwork"), LogType.ERROR);
                    }
                });

            } else {
                final String alertMsg = resourceBundle.getString("host") + StringConstants.SPACE + endpointInfo.getClient().getConnectionDetails().getEndpoint() + StringConstants.SPACE + resourceBundle.getString("unreachable");
                ErrorUtils.dumpTheStack(alertMsg);
                alert.showAlert(alertMsg);
                loggingService.logMessage(resourceBundle.getString("unableToReachNetwork"), LogType.ERROR);
            }
        });
        cancelButton.setOnAction(t -> {
            if (CheckNetwork.isReachable(endpointInfo.getClient())) {
                final String uuid = getTreeTableRow().getTreeItem().getValue().getJobId();
                final Ds3CancelSingleJobTask ds3CancelSingleJobTask = new Ds3CancelSingleJobTask(uuid, endpointInfo, jobInterruptionStore, resourceBundle.getString("recover"), loggingService);
                workers.execute(ds3CancelSingleJobTask);
                ds3CancelSingleJobTask.setOnSucceeded(event -> {
                            LOG.info("Cancellation of interrupted job failed");
                            jobInfoPresenter.refresh(getTreeTableView(), jobInterruptionStore, endpointInfo);
                        }
                );

            } else {
                final String errorMsg = resourceBundle.getString("host") + endpointInfo.getClient().getConnectionDetails().getEndpoint() + StringConstants.SPACE + resourceBundle.getString("unreachable");
                ErrorUtils.dumpTheStack(errorMsg);
                alert.showAlert(errorMsg);
                loggingService.logMessage(resourceBundle.getString("unableToReachNetwork"), LogType.ERROR);
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

    public interface ButtonCellFactory {
        ButtonCell createButtonCell(final EndpointInfo endpointInfo);
    }
}
