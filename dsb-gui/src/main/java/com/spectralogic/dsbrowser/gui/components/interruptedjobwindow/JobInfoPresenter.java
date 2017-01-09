package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;

import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Response;
import com.spectralogic.dsbrowser.gui.Main;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.util.CheckNetwork;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
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
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;


public class JobInfoPresenter implements Initializable {


    private final static Logger LOG = LoggerFactory.getLogger(Main.class);
    private final Alert CLOSECONFIRMATIONALERT = new Alert(
            Alert.AlertType.CONFIRMATION,
            ""
    );
    private final static Alert ALERT = new Alert(Alert.AlertType.INFORMATION);

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

    @Inject
    private Workers workers;

    @Inject
    private JobWorkers jobWorkers;

    @Inject
    private EndpointInfo endpointInfo;

    @Inject
    private JobInterruptionStore jobInterruptionStore;

    @Inject
    private Ds3Common ds3Common;

    private Stage stage;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        ALERT.setTitle("No network connection");
        ALERT.setHeaderText(null);

        initListeners();
        initTreeTableView();
    }

    private void initListeners() {
        cancelJobListButtons.setOnAction(event -> {
            if (CheckNetwork.isReachable(endpointInfo.getClient())) {
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
                if (jobIDMap != null && jobIDMap.size() != 0) {
                    final Button exitButton = (Button) CLOSECONFIRMATIONALERT.getDialogPane().lookupButton(
                            ButtonType.OK
                    );
                    final Button cancelButton = (Button) CLOSECONFIRMATIONALERT.getDialogPane().lookupButton(
                            ButtonType.CANCEL
                    );
                    exitButton.setText("Yes");
                    cancelButton.setText("No! I don't");

                    CLOSECONFIRMATIONALERT.setHeaderText("Are you really want to cancel all interrupted jobs");
                    CLOSECONFIRMATIONALERT.setContentText(jobIDMap.size() + " jobs will be cancelled. You can not recover them in future.");

                    final Optional<ButtonType> closeResponse = CLOSECONFIRMATIONALERT.showAndWait();

                    if (closeResponse.get().equals(ButtonType.OK)) {
                        cancelAllInterruptedJobs(jobIDMap);
                        event.consume();
                    }

                    if (closeResponse.get().equals(ButtonType.CANCEL)) {
                        event.consume();
                    }
                }
            } else {
                ALERT.setContentText("Host " + endpointInfo.getClient().getConnectionDetails().getEndpoint() + " is unreachable. Please check your connection");
                ALERT.showAndWait();
                LOG.info("Network in unreachable");
            }
        });
    }

    private void cancelAllInterruptedJobs(final Map<String, FilesAndFolderMap> jobIDMap) {

        final Task cancelJobId = new Task() {
            @Override
            protected String call() throws Exception {
                jobIDMap.entrySet().stream().forEach(i -> {
                    Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Initiating job cancel for " + i.getKey(), LogType.INFO));
                    try {
                        final CancelJobSpectraS3Response cancelJobSpectraS3Response = endpointInfo.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(i.getKey()));
                        LOG.info("Cancelled job.");
                    } catch (final IOException e) {
                        LOG.info("Unable to cancel job ", e);
                    } finally {
                        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, i.getKey(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter());
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
            //ParseJobInterruptionMap.refreshCompleteTreeTableView(endpointInfo, workers);
            if (cancelJobId.getValue() != null) {
                LOG.info("Cancelled job ", cancelJobId.getValue());
            } else {
                LOG.info("Cancelled to cancel job ");
            }
        });
    }

    private void initTreeTableView() {

        endpointInfo.getDeepStorageBrowserPresenter().logText("Loading interrupted jobs view", LogType.INFO);

        jobListTreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if (jobListTreeTable == null) {
            jobListTreeTable.setPlaceholder(new Label("Great! You don't have any interrupted jobs"));
            Platform.exit();
        }

        jobListTreeTable.setRowFactory(view -> new TreeTableRow<>());

        final TreeTableColumn<JobInfoModel, Boolean> actionColumn = new TreeTableColumn<>("Action");
        actionColumn.setSortable(false);
        actionColumn.setPrefWidth(120);

        actionColumn.setCellValueFactory(
                p -> new SimpleBooleanProperty(p.getValue() != null));

        actionColumn.setCellFactory(
                p -> new ButtonCell(jobWorkers, workers, endpointInfo, jobInterruptionStore, JobInfoPresenter.this));

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
            protected Object call() throws Exception {
                endpointInfo.getDeepStorageBrowserPresenter().logText("Loading interrupted jobs", LogType.INFO);
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
                if (jobIDMap != null) {
                    jobIDMap.entrySet().stream().forEach(i -> {
                        final JobInfoModel jobModel = new JobInfoModel(i.getValue().getType(), i.getKey(), i.getValue().getDate(), i.getValue().getTotalJobSize(), i.getKey(), i.getValue().getType(), "Interrupted", JobInfoModel.Type.JOBID, i.getValue().getTargetLocation(), i.getValue().getBucket());
                        rootTreeItem.getChildren().add(new JobInfoListTreeTableItem(i.getKey(), jobModel, jobIDMap, endpointInfo.getDs3Common().getCurrentSession().get(0), workers));
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
            sizeColumn.setCellFactory(c -> new TreeTableCell<JobInfoModel, Number>() {

                @Override
                protected void updateItem(final Number item, final boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(FileSizeFormat.getFileSizeType(item.longValue()));
                    }
                }
            });
        });
    }

    public void saveJobFileDialog() {
        if (CheckNetwork.isReachable(endpointInfo.getClient())) {
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
            if (jobIDMap != null) {
                jobIDMap.entrySet().stream().forEach(i -> {
                    try {
                        final RecoverInterruptedJob recoverInterruptedJob = new RecoverInterruptedJob(UUID.fromString(i.getKey()), endpointInfo, jobInterruptionStore);
                        jobWorkers.execute(recoverInterruptedJob);
                        final Map<String, FilesAndFolderMap> jobIDMapSec = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);
                        ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMapSec, endpointInfo.getDeepStorageBrowserPresenter());
                        refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);

                        recoverInterruptedJob.setOnSucceeded(event -> {
                            refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);
                            ParseJobInterruptionMap.refreshCompleteTreeTableView(ds3Common, workers);
                        });

                        recoverInterruptedJob.setOnFailed(event -> {
                            Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Failed to recover " + i.getValue().getType() + " job " + endpointInfo.getEndpoint(), LogType.ERROR));
                            refresh(jobListTreeTable, jobInterruptionStore, endpointInfo);
                            ParseJobInterruptionMap.refreshCompleteTreeTableView(ds3Common, workers);
                        });

                        recoverInterruptedJob.setOnCancelled(event -> {
                            try {
                                final CancelJobSpectraS3Response cancelJobSpectraS3Response = endpointInfo.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(i.getKey()));
                                Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Cancel job status : 200", LogType.SUCCESS));
                                ParseJobInterruptionMap.refreshCompleteTreeTableView(ds3Common, workers);
                            } catch (final IOException e1) {
                                Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Failed to cancel job: " + e1.toString(), LogType.ERROR));
                            } finally {
                                final Map<String, FilesAndFolderMap> jobIDMapSecond = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, i.getKey(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter());
                                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMapSecond, endpointInfo.getDeepStorageBrowserPresenter());
                                ParseJobInterruptionMap.refreshCompleteTreeTableView(ds3Common, workers);
                            }
                        });
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } else {
            ALERT.setContentText("Host " + endpointInfo.getClient().getConnectionDetails().getEndpoint() + " is unreachable. Please check your connection");
            ALERT.showAndWait();
            LOG.info("Host network is unreachable");
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
            protected Object call() throws Exception {
                Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Loading interrupted jobs", LogType.INFO));
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), null);

                if (jobIDMap.size() == 0) {
                    Platform.runLater(() -> stage.close());
                }

                jobIDMap.entrySet().stream().forEach(i -> {
                    final FilesAndFolderMap fileAndFolder = i.getValue();
                    final JobInfoModel jobModel = new JobInfoModel(fileAndFolder.getType(), i.getKey(), fileAndFolder.getDate(), fileAndFolder.getTotalJobSize(), i.getKey(), fileAndFolder.getType(), "Interrupted", JobInfoModel.Type.JOBID, fileAndFolder.getTargetLocation(), fileAndFolder.getBucket());
                    rootTreeItem.getChildren().add(new JobInfoListTreeTableItem(i.getKey(), jobModel, jobIDMap, endpointInfo.getDs3Common().getCurrentSession().get(0), workers));
                });

                return null;
            }
        };

        workers.execute(getJobIDs);

        progress.progressProperty().bind(getJobIDs.progressProperty());

        getJobIDs.setOnSucceeded(event -> {
            treeTableView.setRoot(rootTreeItem);
            treeTableView.setPlaceholder(new Label("Great! You don't have any interrupted jobs"));
        });

        getJobIDs.setOnCancelled(event -> {
            treeTableView.setRoot(rootTreeItem);
            treeTableView.setPlaceholder(new Label("Great! You don't have any interrupted jobs"));
        });

        getJobIDs.setOnFailed(event -> {
            treeTableView.setRoot(rootTreeItem);
            treeTableView.setPlaceholder(new Label("Great! You don't have any interrupted jobs"));
        });
    }
}
