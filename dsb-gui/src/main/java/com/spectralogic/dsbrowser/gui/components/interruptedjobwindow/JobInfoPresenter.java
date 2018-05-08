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

import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobService.JobService;
import com.spectralogic.dsbrowser.gui.services.jobService.JobTask;
import com.spectralogic.dsbrowser.gui.services.jobService.JobTaskElement;
import com.spectralogic.dsbrowser.gui.services.jobService.RecoverJob;
import com.spectralogic.dsbrowser.gui.services.jobService.factories.RecoverJobFactory;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3CancelSingleJobTask;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.*;

@Presenter
public class JobInfoPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(JobInfoPresenter.class);


    @FXML
    private TreeTableView<JobInfoModel> jobListTreeTable;

    @FXML
    private TextField logSize;

    @FXML
    private Button saveJobListButtons, cancelJobListButtons;

    @FXML
    private TreeTableColumn sizeColumn;

    @FXML
    private TreeTableColumn jobIdColumn;

    @ModelContext
    private EndpointInfo endpointInfo;

    private final ResourceBundle resourceBundle;
    private final Workers workers;
    private final JobInterruptionStore jobInterruptionStore;
    private final LoggingService loggingService;
    private final ButtonCell.ButtonCellFactory buttonCellFactory;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final RecoverJobFactory recoverJobFactory;
    private final RefreshCompleteViewWorker refreshCompleteViewWorker;
    private Stage stage;

    @Inject
    public JobInfoPresenter(final ResourceBundle resourceBundle,
            final Workers workers,
            final JobInterruptionStore jobInterruptionStore,
            final ButtonCell.ButtonCellFactory buttonCellFactory,
            final LoggingService loggingService,
            final RecoverJobFactory recoverJobFactory,
            final RefreshCompleteViewWorker refreshCompleteViewWorker,
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        this.resourceBundle = resourceBundle;
        this.refreshCompleteViewWorker = refreshCompleteViewWorker;
        this.workers = workers;
        this.jobInterruptionStore = jobInterruptionStore;
        this.loggingService = loggingService;
        this.recoverJobFactory = recoverJobFactory;
        this.buttonCellFactory = buttonCellFactory;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initListeners();
            initTreeTableView();
        } catch (final Throwable t) {
            LOG.error("Encountered error when initializing JobInfoPresenter", t);
        }
    }

    private void initListeners() {
        cancelJobListButtons.setOnAction(SafeHandler.logHandle(event -> {
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), deepStorageBrowserPresenter.getJobProgressView(), null);
            if (jobIDMap != null && jobIDMap.size() != 0) {
                final Optional<ButtonType> closeResponse = Ds3Alert.showConfirmationAlert(
                        resourceBundle.getString("confirmation"),
                        jobIDMap.size() + StringConstants.SPACE + resourceBundle.getString("jobsWillBeCancelled"),
                        Alert.AlertType.CONFIRMATION,
                        resourceBundle.getString("reallyWantToCancel"),
                        resourceBundle.getString("exitBtnJobCancelConfirm"),
                        resourceBundle.getString("cancelBtnJobCancelConfirm"));
                if (closeResponse.isPresent()) {
                    if (closeResponse.get().equals(ButtonType.OK)) {
                        cancelAllInterruptedJobs(jobIDMap);
                    }
                    event.consume();
                }
            }
        }));
    }

    private void cancelAllInterruptedJobs(final Map<String, FilesAndFolderMap> jobIDMap) {
        // TODO pull into a service which returns an Observable
        final Task cancelJobId = new Task() {
            @Override
            protected String call() {
                jobIDMap.forEach((key, value) -> {
                    loggingService.logMessage("Initiating job cancel for " + key, LogType.INFO);
                    try {
                        endpointInfo.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(key));
                        LOG.info("Canceled job.");
                    } catch (final IOException e) {
                        LOG.error("Unable to cancel job ", e);
                    } finally {
                        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, key, endpointInfo.getEndpoint(), deepStorageBrowserPresenter, loggingService);
                        ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);
                    }
                });
                return null;
            }
        };
        cancelJobId.setOnSucceeded(SafeHandler.logHandle(event -> {
            refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);
            if (cancelJobId.getValue() != null) {
                LOG.info("Canceled job {}", cancelJobId.getValue());
            } else {
                LOG.info("Canceled to cancel job ");
            }
        }));
        workers.execute(cancelJobId);
        refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);
    }

    private void initTreeTableView() {
        loggingService.logMessage("Loading interrupted jobs view", LogType.INFO);

        jobListTreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if (jobListTreeTable == null) {
            jobListTreeTable.setPlaceholder(new Label(resourceBundle.getString("dontHaveInterruptedJobs")));
            Platform.exit();
        }
        jobListTreeTable.setRowFactory(view -> new TreeTableRow<>());
        final TreeTableColumn<JobInfoModel, Boolean> actionColumn = new TreeTableColumn<>("Action");
        actionColumn.setSortable(false);
        actionColumn.setPrefWidth(120);
        actionColumn.setCellValueFactory(
                p -> new SimpleBooleanProperty(p.getValue() != null));
        actionColumn.setCellFactory(getTreeTableColumnTreeTableCellCallback());

        jobListTreeTable.getColumns().add(actionColumn);
        final TreeItem<JobInfoModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        jobListTreeTable.setShowRoot(false);
        final Node oldPlaceHolder = jobListTreeTable.getPlaceholder();
        final ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(90, 90);
        jobListTreeTable.setPlaceholder(new StackPane(progress));

        final Task getJobIDs = new Task() {
            @Override
            protected Object call() {
                loggingService.logMessage("Loading interrupted jobs", LogType.INFO);

                //to show jobs in reverse order
                final Map<String, FilesAndFolderMap> jobIDHashMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), deepStorageBrowserPresenter.getJobProgressView(), null);
                final TreeMap<String, FilesAndFolderMap> jobIDTreeMap = new TreeMap(jobIDHashMap);
                final Map<String, FilesAndFolderMap> jobIDMap = jobIDTreeMap.descendingMap();
                if (jobIDMap != null) {
                    jobIDMap.forEach((key, value) -> {
                        final JobInfoModel jobModel = new JobInfoModel(value.getType(), key, value.getDate(), value.getTotalJobSize(), key, value.getType(), "Interrupted", JobInfoModel.Type.JOBID, value.getTargetLocation(), value.getBucket());
                        rootTreeItem.getChildren().add(new JobInfoListTreeTableItem(key, jobModel, jobIDMap, endpointInfo.getDs3Common().getCurrentSession(), workers));
                    });
                }
                return rootTreeItem;
            }
        };
        progress.progressProperty().bind(getJobIDs.progressProperty());
        getJobIDs.setOnSucceeded(SafeHandler.logHandle(event -> {
            jobListTreeTable.setPlaceholder(oldPlaceHolder);
            jobListTreeTable.setRoot(rootTreeItem);
            sizeColumn.setCellFactory(c -> new ValueTreeTableCell<JobInfoModel>());
        }));
        workers.execute(getJobIDs);
    }

    @NotNull
    private Callback<TreeTableColumn<JobInfoModel, Boolean>, TreeTableCell<JobInfoModel, Boolean>> getTreeTableColumnTreeTableCellCallback() {
        return cell -> {
            final ButtonCell buttonCell = buttonCellFactory.createButtonCell(endpointInfo);
            buttonCell.setRecoverHandler(SafeHandler.logHandle(recoverJobEvent -> {
                LOG.info("Recover Interrupted Job button clicked");
                loggingService.logMessage(resourceBundle.getString("initiatingRecovery"), LogType.INFO);

                final String jobId = buttonCell.getTreeTableRow().getTreeItem().getValue().getJobId();
                recoverJobFactory.create(UUID.fromString(jobId), endpointInfo, () -> {
                    refreshCompleteViewWorker.refreshCompleteTreeTableView();
                    refresh(buttonCell.getTreeTableView(), jobInterruptionStore, endpointInfo);
                    return Unit.INSTANCE;
                });

                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(),
                        endpointInfo.getEndpoint(),
                        endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(),
                        null);
                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
                refresh(buttonCell.getTreeTableView(), jobInterruptionStore, endpointInfo);
            }));

            buttonCell.setCancelHandler(SafeHandler.logHandle(cancelJobEvent -> {
                final String jobId = buttonCell.getTreeTableRow().getTreeItem().getValue().getJobId();
                final Ds3CancelSingleJobTask ds3CancelSingleJobTask = new Ds3CancelSingleJobTask(
                        jobId,
                        endpointInfo,
                        jobInterruptionStore,
                        resourceBundle.getString("recover"),
                        loggingService);
                ds3CancelSingleJobTask.setOnSucceeded(SafeHandler.logHandle(event -> {
                    LOG.info("Cancellation of interrupted job " + jobId + " succeeded");
                    refresh(buttonCell.getTreeTableView(), jobInterruptionStore, endpointInfo);
                }));
                ds3CancelSingleJobTask.setOnFailed(SafeHandler.logHandle(cancelJobTaskFailedEvent -> {
                    LOG.info("Cancellation of interrupted job " + jobId + " failed");
                }));

                workers.execute(ds3CancelSingleJobTask);
            }));

            return buttonCell;
        };
    }

    public void saveJobFileDialog() {
        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), deepStorageBrowserPresenter.getJobProgressView(), null);
        if (jobIDMap != null) {
            jobIDMap.forEach((key, value) -> {
                recoverJobFactory.create(UUID.fromString(key), endpointInfo, () -> {
                    refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);
                    refreshCompleteViewWorker.refreshCompleteTreeTableView();
                    return Unit.INSTANCE;
                });

                final Map<String, FilesAndFolderMap> jobIDMapSec = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), deepStorageBrowserPresenter.getJobProgressView(), null);
                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMapSec, deepStorageBrowserPresenter);
                refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);
            });
        }
    }

    public void refresh(final TreeTableView<JobInfoModel> treeTableView, final JobInterruptionStore jobInterruptionStore, final EndpointInfo endpointInfo) {
        if (stage == null) {
            stage = (Stage) treeTableView.getScene().getWindow();
        }
        final TreeItem<JobInfoModel> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        treeTableView.setShowRoot(false);
        final ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(90, 90);
        treeTableView.setPlaceholder(new StackPane(progress));
        final Task getJobIDs = new Task() {
            @Override
            protected Optional<Object> call() {
                loggingService.logMessage("Loading interrupted jobs", LogType.INFO);
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), deepStorageBrowserPresenter.getJobProgressView(), null);
                if (jobIDMap != null) {
                    if (jobIDMap.size() == 0) {
                        Platform.runLater(() -> stage.close());
                    }
                    jobIDMap.forEach((key, fileAndFolder) -> {
                        final JobInfoModel jobModel = new JobInfoModel(fileAndFolder.getType(), key, fileAndFolder.getDate(), fileAndFolder.getTotalJobSize(), key, fileAndFolder.getType(), "Interrupted", JobInfoModel.Type.JOBID, fileAndFolder.getTargetLocation(), fileAndFolder.getBucket());
                        rootTreeItem.getChildren().add(new JobInfoListTreeTableItem(key, jobModel, jobIDMap, endpointInfo.getDs3Common().getCurrentSession(), workers));
                    });
                }
                return Optional.empty();
            }
        };
        progress.progressProperty().bind(getJobIDs.progressProperty());
        getJobIDs.setOnSucceeded(SafeHandler.logHandle(event -> {
            treeTableView.setRoot(rootTreeItem);
        }));
        getJobIDs.setOnCancelled(SafeHandler.logHandle(event -> {
            treeTableView.setRoot(rootTreeItem);
        }));
        getJobIDs.setOnFailed(SafeHandler.logHandle((WorkerStateEvent event) -> {
            LOG.error("Get Job IDs failed", event.getSource().getException());
            loggingService.logMessage("Get Job IDs failed", LogType.ERROR);
            treeTableView.setRoot(rootTreeItem);
        }));
        workers.execute(getJobIDs);
    }
}
