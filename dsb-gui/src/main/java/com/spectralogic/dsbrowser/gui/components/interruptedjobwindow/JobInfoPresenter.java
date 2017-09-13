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
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.tasks.RecoverInterruptedJob;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.*;

@Presenter
public class JobInfoPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(JobInfoPresenter.class);

    private final LazyAlert alert = new LazyAlert("Error");

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
    private final Ds3Common ds3Common;
    private final Workers workers;
    private final JobWorkers jobWorkers;
    private final JobInterruptionStore jobInterruptionStore;
    private final LoggingService loggingService;
    private final RecoverInterruptedJob.RecoverInterruptedJobFactory recoverInterruptedJobFactory;
    private final ButtonCell.ButtonCellFactory buttonCellFactory;

    private Stage stage;

    @Inject
    public JobInfoPresenter(final ResourceBundle resourceBundle,
                            final Ds3Common ds3Common,
                            final Workers workers,
                            final JobWorkers jobWorkers,
                            final JobInterruptionStore jobInterruptionStore,
                            final RecoverInterruptedJob.RecoverInterruptedJobFactory recoverInterruptedJobFactory,
                            final ButtonCell.ButtonCellFactory buttonCellFactory,
                            final LoggingService loggingService) {
        this.resourceBundle = resourceBundle;
        this.ds3Common = ds3Common;
        this.workers = workers;
        this.jobWorkers = jobWorkers;
        this.jobInterruptionStore = jobInterruptionStore;
        this.loggingService = loggingService;
        this.recoverInterruptedJobFactory = recoverInterruptedJobFactory;
        this.buttonCellFactory = buttonCellFactory;
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
        cancelJobListButtons.setOnAction(event -> {
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
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
        });
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
                        LOG.info("Cancelled job.");
                    } catch (final IOException e) {
                        LOG.error("Unable to cancel job ", e);
                    } finally {
                        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, key, endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter(), loggingService);
                        ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
                    }
                });
                return null;
            }
        };
        workers.execute(cancelJobId);
        refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);
        cancelJobId.setOnSucceeded(event -> {
            refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);
            if (cancelJobId.getValue() != null) {
                LOG.info("Cancelled job {}", cancelJobId.getValue());
            } else {
                LOG.info("Cancelled to cancel job ");
            }
        });
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
        actionColumn.setCellFactory(
                p -> buttonCellFactory.createButtonCell(endpointInfo));
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
                final Map<String, FilesAndFolderMap> jobIDHashMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
                final TreeMap<String, FilesAndFolderMap> jobIDTreeMap = new TreeMap(jobIDHashMap);
                final Map<String, FilesAndFolderMap> jobIDMap = jobIDTreeMap.descendingMap();
                if (jobIDMap != null) {
                    jobIDMap.entrySet().forEach(i -> {
                        final JobInfoModel jobModel = new JobInfoModel(i.getValue().getType(), i.getKey(), i.getValue().getDate(), i.getValue().getTotalJobSize(), i.getKey(), i.getValue().getType(), "Interrupted", JobInfoModel.Type.JOBID, i.getValue().getTargetLocation(), i.getValue().getBucket());
                        rootTreeItem.getChildren().add(new JobInfoListTreeTableItem(i.getKey(), jobModel, jobIDMap, endpointInfo.getDs3Common().getCurrentSession(), workers));
                    });
                }
                return rootTreeItem;
            }
        };
        progress.progressProperty().bind(getJobIDs.progressProperty());
        workers.execute(getJobIDs);
        getJobIDs.setOnSucceeded(event -> {
            jobListTreeTable.setPlaceholder(oldPlaceHolder);
            jobListTreeTable.setRoot(rootTreeItem);
            sizeColumn.setCellFactory(c -> new ValueTreeTableCell<JobInfoModel>());
        });
    }

    public void saveJobFileDialog() {
        if (CheckNetwork.isReachable(endpointInfo.getClient())) {
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
            if (jobIDMap != null) {
                jobIDMap.entrySet().forEach(i -> {
                    final RecoverInterruptedJob recoverInterruptedJob = recoverInterruptedJobFactory.createRecoverInterruptedJob(UUID.fromString(i.getKey()), endpointInfo);
                    jobWorkers.execute(recoverInterruptedJob);
                    final Map<String, FilesAndFolderMap> jobIDMapSec = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
                    ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMapSec, endpointInfo.getDeepStorageBrowserPresenter());
                    refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);
                    recoverInterruptedJob.setOnSucceeded(event -> {
                        refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);
                        RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers, loggingService);
                    });
                    recoverInterruptedJob.setOnFailed(event -> {
                        loggingService.logMessage("Failed to recover " + i.getValue().getType() + " job " + endpointInfo.getEndpoint(), LogType.ERROR);
                        refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);
                        RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers, loggingService);
                    });
                    recoverInterruptedJob.setOnCancelled(event -> {
                        try {
                            endpointInfo.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(i.getKey()));
                            loggingService.logMessage("Cancel job status : 200", LogType.SUCCESS);
                            RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers, loggingService);
                        } catch (final IOException e) {
                            loggingService.logMessage("Failed to cancel job: " + e, LogType.ERROR);
                        } finally {
                            final Map<String, FilesAndFolderMap> jobIDMapSecond = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, i.getKey(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter(), loggingService);
                            ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMapSecond, endpointInfo.getDeepStorageBrowserPresenter());
                            RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers, loggingService);
                        }
                    });
                });
            }
        } else {
            final String errorMsg = resourceBundle.getString("host") + endpointInfo.getClient().getConnectionDetails().getEndpoint() + resourceBundle.getString(" unreachable");
            ErrorUtils.dumpTheStack(errorMsg);
            alert.showAlert(errorMsg);
            LOG.info("Network is unreachable");
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
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
                if (jobIDMap == null) {
                    if (jobIDMap.size() == 0) {
                        Platform.runLater(() -> stage.close());
                    }
                    jobIDMap.entrySet().stream().forEach(i -> {
                        final FilesAndFolderMap fileAndFolder = i.getValue();
                        final JobInfoModel jobModel = new JobInfoModel(fileAndFolder.getType(), i.getKey(), fileAndFolder.getDate(), fileAndFolder.getTotalJobSize(), i.getKey(), fileAndFolder.getType(), "Interrupted", JobInfoModel.Type.JOBID, fileAndFolder.getTargetLocation(), fileAndFolder.getBucket());
                        rootTreeItem.getChildren().add(new JobInfoListTreeTableItem(i.getKey(), jobModel, jobIDMap, endpointInfo.getDs3Common().getCurrentSession(), workers));
                    });
                }
                return Optional.empty();
            }
        };
        workers.execute(getJobIDs);
        progress.progressProperty().bind(getJobIDs.progressProperty());
        getJobIDs.setOnSucceeded(event -> {
            treeTableView.setRoot(rootTreeItem);
            treeTableView.setPlaceholder(new Label(resourceBundle.getString("dontHaveInterruptedJobs")));
        });
        getJobIDs.setOnCancelled(event -> {
            treeTableView.setRoot(rootTreeItem);
            treeTableView.setPlaceholder(new Label(resourceBundle.getString("dontHaveInterruptedJobs")));
        });
        getJobIDs.setOnFailed(event -> {
            treeTableView.setRoot(rootTreeItem);
            treeTableView.setPlaceholder(new Label(resourceBundle.getString("dontHaveInterruptedJobs")));
        });
    }
}
