package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.*;
import com.spectralogic.ds3client.commands.spectrads3.*;
import com.spectralogic.ds3client.models.BucketDetails;
import com.spectralogic.ds3client.models.ListBucketResult;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketPopup;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketWithDataPoliciesModel;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderModel;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderPopup;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.*;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.RecoverInterruptedJob;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.Ds3GetJob;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeTableItem;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.LocalFileTreeTableProvider;
import com.spectralogic.dsbrowser.gui.components.modifyjobpriority.ModifyJobPriorityModel;
import com.spectralogic.dsbrowser.gui.components.modifyjobpriority.ModifyJobPriorityPopUp;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPopup;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap.setButtonAndCountNumber;

public class Ds3PanelPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PanelPresenter.class);
    private final static Alert ALERT = new Alert(Alert.AlertType.INFORMATION);

    private final Image LENSICON = new Image(ImageURLs.LENSICON);
    private final Image CROSSICON = new Image(ImageURLs.CROSSICON);

    @FXML
    private Label ds3PathIndicator;

    @FXML
    private Tooltip ds3PathIndicatorTooltip;

    @FXML
    private Button ds3Refresh, ds3NewFolder, ds3NewBucket, ds3DeleteButton, newSessionButton, ds3TransferLeft;

    @FXML
    private Tooltip ds3RefreshToolTip, ds3NewFolderToolTip, ds3NewBucketToolTip, ds3DeleteButtonToolTip, ds3TransferLeftToolTip;

    @FXML
    private TextField ds3PanelSearch;

    @FXML
    private Tab addNewTab;

    @FXML
    private TabPane ds3SessionTabPane;

    @FXML
    private ImageView imageView, imageViewForTooltip;

    @FXML
    private Ds3TreeTablePresenter ds3TreeTablePresenter;

    @Inject
    private Ds3SessionStore store;

    @Inject
    private Workers workers;

    @Inject
    private JobWorkers jobWorkers;

    @Inject
    private ResourceBundle resourceBundle;

    @Inject
    private SavedJobPrioritiesStore savedJobPrioritiesStore;

    @Inject
    private JobInterruptionStore jobInterruptionStore;

    @Inject
    private SettingsStore settingsStore;

    @Inject
    private DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    @Inject
    private LocalFileTreeTableProvider provider;

    @Inject
    private DataFormat dataFormat;

    @Inject
    private Ds3Common ds3Common;

    private TreeTableView<Ds3TreeTableValue> ds3TreeTableView = null;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Loading Ds3PanelPresenter");
            ALERT.setTitle("Error");
            ALERT.setHeaderText(null);

            ds3PathIndicator = makeSelectable(ds3PathIndicator);
            initMenuItems();
            initButtons();
            initTab();
            initTabPane();
            initListeners();
            ds3Common.setDs3PanelPresenter(this);
            ds3Common.setDeepStorageBrowserPresenter(deepStorageBrowserPresenter);
            final BackgroundTask backgroundTask = new BackgroundTask(ds3Common, workers);
            workers.execute(backgroundTask);
        } catch (final Throwable e) {
            LOG.error("Encountered error when creating Ds3PanelPresenter", e);
            throw e;
        }
    }

    private Label makeSelectable(final Label label) {
        final StackPane textStack = new StackPane();
        final TextField textField = new TextField(label.getText());
        textField.setEditable(false);
        textField.setStyle(
                "-fx-background-color: transparent; -fx-background-insets: 0; -fx-background-radius: 0; -fx-padding: 0;"
        );
        // the invisible label is a hack to get the textField to size like a label.
        final Label invisibleLabel = new Label();
        invisibleLabel.textProperty().bind(label.textProperty());
        invisibleLabel.setVisible(false);
        textStack.getChildren().addAll(invisibleLabel, textField);
        label.textProperty().bindBidirectional(textField.textProperty());
        label.setGraphic(textStack);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        return label;
    }

    public Label getDs3PathIndicator() {
        return ds3PathIndicator;
    }

    public Tooltip getDs3PathIndicatorTooltip() {
        return ds3PathIndicatorTooltip;
    }


    public TreeTableView<Ds3TreeTableValue> getDs3TreeTableView() {
        return ds3TreeTableView;
    }

    private void initListeners() {
        ds3DeleteButton.setOnAction(event -> ds3DeleteObjects());

        ds3Refresh.setOnAction(event -> ParseJobInterruptionMap.refreshCompleteTreeTableView(ds3Common, workers));

        ds3NewFolder.setOnAction(event -> ds3NewFolder());

        ds3TransferLeft.setOnAction(event -> ds3TransferToLocal());

        ds3NewBucket.setOnAction(event -> {
            LOG.info("Create Bucket Prompt");
            final Session session = getSession();
            if (session != null) {
                deepStorageBrowserPresenter.logText("Fetching data policies", LogType.INFO);
                final Task<CreateBucketWithDataPoliciesModel> getDataPolicies = new Task<CreateBucketWithDataPoliciesModel>() {

                    @Override
                    protected CreateBucketWithDataPoliciesModel call() throws Exception {
                        final Ds3Client client = session.getClient();
                        final ImmutableList<CreateBucketModel> buckets = client.getDataPoliciesSpectraS3(new GetDataPoliciesSpectraS3Request()).getDataPolicyListResult().
                                getDataPolicies().stream().map(bucket -> new CreateBucketModel(bucket.getName(), bucket.getId())).collect(GuavaCollectors.immutableList());
                        final ImmutableList<CreateBucketWithDataPoliciesModel> dataPoliciesList = buckets.stream().map(policies ->
                                new CreateBucketWithDataPoliciesModel(buckets, session, workers)).collect(GuavaCollectors.immutableList());
                        Platform.runLater(() -> deepStorageBrowserPresenter.logText("Successfully retrieved data policies", LogType.SUCCESS));
                        return dataPoliciesList.stream().findFirst().orElse(null);
                    }
                };

                workers.execute(getDataPolicies);

                getDataPolicies.setOnSucceeded(taskEvent -> Platform.runLater(() -> {
                    LOG.info("Launching create bucket popup" + getDataPolicies.getValue().getDataPolicies().size());
                    CreateBucketPopup.show(getDataPolicies.getValue(), deepStorageBrowserPresenter);
                    ParseJobInterruptionMap.refreshCompleteTreeTableView(ds3Common, workers);
                }));

            } else {
                ALERT.setContentText("Invalid session!");
                ALERT.showAndWait();
            }
        });

        store.getObservableList().addListener((ListChangeListener<Session>) c -> {
            if (c.next() && c.wasAdded()) {
                final List<? extends Session> newItems = c.getAddedSubList();
                newItems.stream().forEach(newSession -> {
                    addNewTab.setTooltip(new Tooltip(resourceBundle.getString("newSessionToolTip")));
                    final Ds3TreeTableView newTreeView = new Ds3TreeTableView(newSession, deepStorageBrowserPresenter, this, ds3Common);
                    final Tab treeTab = new Tab(newSession.getSessionName() + "-" + newSession.getEndpoint(), newTreeView.getView());
                    treeTab.setOnClosed(event -> {
                        try {
                            final Tab closedTab = (Tab) event.getSource();
                            if (closedTab != null) {
                                final Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(closedTab.getText())).findFirst().orElse(null);
                                if (session != null) {
                                    final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), session.getEndpoint() + ":" + session.getPortNo(), deepStorageBrowserPresenter.getJobProgressView(), null);
                                    ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);
                                    ParseJobInterruptionMap.cancelAllRunningJobsBySession(jobWorkers, jobInterruptionStore, LOG, workers, session);
                                }
                            } else {
                                ParseJobInterruptionMap.setButtonAndCountNumber(null, deepStorageBrowserPresenter);
                                ParseJobInterruptionMap.cancelAllRunningJobsBySession(jobWorkers, jobInterruptionStore, LOG, workers, newSession);
                            }
                            store.removeSession(newSession);
                            ds3Common.getExpandedNodesInfo().remove(newSession.getSessionName() + "-" + newSession.getEndpoint());
                            if (ds3Common.getCurrentSession().contains(newSession)) {
                                ds3Common.getCurrentSession().clear();
                                ds3Common.getCurrentTabPane().clear();
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }

                        ds3PathIndicator.setText("");
                        deepStorageBrowserPresenter.logText(newSession.getSessionName() + "-" + newSession.getEndpoint() + " closed.", LogType.ERROR);
                        if (store.size() == 0) {
                            addNewTab.setTooltip(null);
                        }
                    });
                    treeTab.setTooltip(new Tooltip(newSession.getSessionName() + "-" + newSession.getEndpoint()));
                    final int totalTabs = ds3SessionTabPane.getTabs().size();
                    ds3SessionTabPane.getTabs().add(totalTabs - 1, treeTab);
                    ds3SessionTabPane.getSelectionModel().select(treeTab);
                    deepStorageBrowserPresenter.logText("Starting " + newSession.getSessionName() + "-" + newSession.getEndpoint() + " session", LogType.SUCCESS);
                });
            }
        });

        ds3SessionTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
                    try {
                        final VBox vbox = (VBox) newTab.getContent();
                        @SuppressWarnings("unchecked")
                        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView1 = (TreeTableView<Ds3TreeTableValue>) vbox.getChildren().stream().filter(i -> i instanceof TreeTableView).findFirst().orElse(null);
                        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView1.getSelectionModel().getSelectedItems()
                                .stream().collect(GuavaCollectors.immutableList());

                        if (ds3Common.getCurrentSession().size() > 0) {
                            ds3Common.getCurrentSession().clear();
                            ds3Common.getCurrentTabPane().clear();
                        }

                        final Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(newTab.getText())).findFirst().orElse(null);
                        ds3Common.getCurrentSession().add(session);
                        ds3Common.getCurrentTabPane().add(ds3SessionTabPane);
                        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), session.getEndpoint() + ":" + session.getPortNo(), deepStorageBrowserPresenter.getJobProgressView(), null);
                        setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);

                        if (values.size() == 0) {
                            ds3PathIndicator.setText("");
                        } else {
                            final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = values.stream().findFirst().orElse(null);
                            if (ds3TreeTableValueTreeItem != null) {
                                final Ds3TreeTableValue value = ds3TreeTableValueTreeItem.getValue();
                                if (!value.getType().equals(Ds3TreeTableValue.Type.Bucket)) {
                                    ds3PathIndicator.setText(value.getBucketName() + "/" + value.getFullName());
                                } else {
                                    ds3PathIndicator.setText(value.getBucketName());
                                }
                            }
                        }
                    } catch (final Exception e) {
                        LOG.info("Not able to parse");
                    }
                }
        );

        ds3SessionTabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
            if (c.next() && c.wasRemoved()) {
                if (ds3SessionTabPane.getTabs().size() == 1) {
                    disableMenu(true);
                }
            } else if (c.wasAdded()) {
                disableMenu(false);
            }
        });

        deepStorageBrowserPresenter.getJobProgressView().setGraphicFactory(task -> {

            final Button button = new Button();
            final ImageView imageView = new ImageView();
            imageView.setImage(new Image(ImageURLs.SETTINGSICON));
            button.setGraphic(imageView);
            button.setTooltip(new Tooltip("View/Modify Job Priority"));
            button.setOnAction(event -> {
                UUID jobId = null;
                if (task instanceof Ds3PutJob) {
                    final Ds3PutJob ds3PutJob = (Ds3PutJob) task;
                    jobId = ds3PutJob.getJobId();
                }
                if (task instanceof Ds3GetJob) {
                    final Ds3GetJob ds3GetJob = (Ds3GetJob) task;
                    jobId = ds3GetJob.getJobId();
                }
                if (task instanceof RecoverInterruptedJob) {
                    final RecoverInterruptedJob recoverInterruptedJob = (RecoverInterruptedJob) task;
                    jobId = recoverInterruptedJob.getUuid();
                }
                final Session session = getSession();
                if (session != null) {
                    final UUID finalJobId = jobId;
                    final Task<ModifyJobPriorityModel> getJobPriority = new Task<ModifyJobPriorityModel>() {
                        @Override
                        protected ModifyJobPriorityModel call() throws Exception {
                            final Ds3Client client = session.getClient();
                            final GetJobSpectraS3Response jobSpectraS3 = client.getJobSpectraS3(new GetJobSpectraS3Request(finalJobId));
                            return new ModifyJobPriorityModel(finalJobId, jobSpectraS3.getMasterObjectListResult().getPriority().toString(), session);
                        }
                    };
                    workers.execute(getJobPriority);
                    getJobPriority.setOnSucceeded(eventPriority -> Platform.runLater(() -> {
                        LOG.info("Launching metadata popup");
                        ModifyJobPriorityPopUp.show(getJobPriority.getValue());
                    }));
                }
            });

            return button;
        });
    }

    private Session getSession() {
        return ds3Common.getCurrentSession().stream().findFirst().orElse(null);
    }

    private void ds3TransferToLocal() {
        final Session session = getSession();
        if (session != null) {
            try {
                final ObservableList<javafx.scene.Node> list = deepStorageBrowserPresenter.getFileSystem().getChildren();
                final VBox vbox = (VBox) list.stream().filter(i -> i instanceof VBox).findFirst().orElse(null);
                final ObservableList<javafx.scene.Node> children = vbox.getChildren();
                @SuppressWarnings("unchecked")
                final TreeTableView<FileTreeModel> treeTable = (TreeTableView<FileTreeModel>) children.stream().filter(i -> i instanceof TreeTableView).findFirst().orElse(null);
                final Pane pane = (Pane) children.stream().filter(i -> i instanceof Pane).findFirst().orElse(null);
                final Label fileRootItemLabel = (Label) pane.getChildren().stream().filter(i -> i instanceof Label).findFirst().orElse(null);
                final String fileRootItem = fileRootItemLabel.getText();
                final ObservableList<TreeItem<FileTreeModel>> selectedItemsAtDestination = treeTable.getSelectionModel().getSelectedItems();

                if (fileRootItem.equals("My Computer")) {
                    if (selectedItemsAtDestination.isEmpty()) {
                        LOG.error("Location not selected");
                        ALERT.setContentText("Please select destination location");
                        ALERT.showAndWait();
                        return;
                    }
                }

                if (selectedItemsAtDestination.size() > 1) {
                    ALERT.setContentText("Multiple destinations not allowed. Please select One.");
                    ALERT.showAndWait();
                    return;
                }

                final List<FileTreeModel> selectedItemsAtDestinationList = selectedItemsAtDestination.stream().map(TreeItem::getValue).collect(Collectors.toList());
                @SuppressWarnings("unchecked")
                final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = getTreeTableView();
                final ImmutableList<TreeItem<Ds3TreeTableValue>> selectedItemsAtSourceLocation = ds3TreeTableView.getSelectionModel().getSelectedItems()
                        .stream().collect(GuavaCollectors.immutableList());

                if (selectedItemsAtSourceLocation.isEmpty()) {
                    LOG.error("Files not selected");
                    ALERT.setContentText("Please select files to transfer");
                    ALERT.showAndWait();
                    return;
                }

                final List<Ds3TreeTableValue> selectedItemsAtSourceLocationList = selectedItemsAtSourceLocation.stream().map(TreeItem::getValue).collect(Collectors.toList());
                final List<Ds3TreeTableValueCustom> selectedItemsAtSourceLocationListCustom = selectedItemsAtSourceLocationList.stream().map(v -> new Ds3TreeTableValueCustom(v.getBucketName(), v.getFullName(), v.getType(), v.getSize(), v.getLastModified(), v.getOwner(), v.isSearchOn())).collect(Collectors.toList());

                final Path localPath;
                final FileTreeModel selectedAtDest = selectedItemsAtDestinationList.stream().findFirst().orElse(null);
                if (selectedAtDest == null) {
                    localPath = Paths.get(fileRootItem);
                } else if (selectedAtDest.getType().equals(FileTreeModel.Type.File)) {
                    localPath = selectedAtDest.getPath().getParent();
                } else
                    localPath = selectedAtDest.getPath();

                final String priority = (!savedJobPrioritiesStore.getJobSettings().getGetJobPriority().equals(resourceBundle.getString("defaultPolicyText"))) ? savedJobPrioritiesStore.getJobSettings().getGetJobPriority() : null;

                final Ds3GetJob getJob = new Ds3GetJob(selectedItemsAtSourceLocationListCustom, localPath, session.getClient(),
                        deepStorageBrowserPresenter, priority, settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads(), jobInterruptionStore, ds3Common);
                jobWorkers.execute(getJob);

                getJob.setOnSucceeded(event -> {
                    LOG.info("Succeed");
                    refreshLocalSideView(selectedItemsAtDestination, treeTable, fileRootItemLabel, fileRootItem);
                });

                getJob.setOnFailed(e -> {
                    LOG.info("Get Job failed");
                    refreshLocalSideView(selectedItemsAtDestination, treeTable, fileRootItemLabel, fileRootItem);
                });

                getJob.setOnCancelled(e -> {
                    LOG.info("Get Job cancelled");
                    if (getJob.getJobId() != null) {
                        try {
                            final CancelJobSpectraS3Response cancelJobSpectraS3Response = session.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(getJob.getJobId()));
                            ParseJobInterruptionMap.removeJobID(jobInterruptionStore, getJob.getJobId().toString(), getJob.getDs3Client().getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter);
                            deepStorageBrowserPresenter.logText("GET Job Cancelled", LogType.ERROR);
                        } catch (final IOException e1) {
                            LOG.info("Failed to cancel job", LogType.ERROR);
                        }
                    }
                    refreshLocalSideView(selectedItemsAtDestination, treeTable, fileRootItemLabel, fileRootItem);
                });

            } catch (final Exception e) {
                deepStorageBrowserPresenter.logText("Something went wrong", LogType.ERROR);
                ALERT.setContentText("Something went wrong");
                ALERT.showAndWait();
            }
        } else {
            ALERT.setContentText("Invalid Session!");
            ALERT.showAndWait();
        }

    }

    private void refreshLocalSideView(final ObservableList<TreeItem<FileTreeModel>> selectedItemsAtDestination, final TreeTableView<FileTreeModel> treeTable, final Label fileRootItemLabel, final String fileRootItem) {
        if (selectedItemsAtDestination.stream().findFirst().isPresent()) {
            final TreeItem<FileTreeModel> selectedItem = selectedItemsAtDestination.stream().findFirst().orElse(null);
            if (selectedItem != null) {
                if (selectedItem instanceof FileTreeTableItem) {
                    final FileTreeTableItem fileTreeTableItem = (FileTreeTableItem) selectedItem;
                    fileTreeTableItem.refresh();
                    treeTable.getSelectionModel().clearSelection();
                    treeTable.getSelectionModel().select(selectedItem);
                }
            }
        } else {
            final TreeItem<FileTreeModel> rootTreeItem = new TreeItem<>();
            rootTreeItem.setExpanded(true);
            treeTable.setShowRoot(false);

            final Stream<FileTreeModel> rootItems = provider.getRoot(fileRootItem);
            fileRootItemLabel.setText(fileRootItem);
            rootItems.forEach(ftm -> {
                final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, ftm, workers);
                rootTreeItem.getChildren().add(newRootTreeItem);
            });

            treeTable.setRoot(rootTreeItem);
        }
    }

    private void ds3DeleteObjects() {
        final Session session = getSession();
        if (session != null) {
            ds3TreeTableView = getTreeTableView();
            final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView.getSelectionModel().getSelectedItems()
                    .stream().collect(GuavaCollectors.immutableList());

            if (values.isEmpty()) {
                LOG.error("No files selected");
                ALERT.setContentText("No files selected");
                ALERT.showAndWait();
                return;
            }

            if (values.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
                LOG.error("You can not delete from here. Please go to specific location and delete object(s)");
                ALERT.setContentText("You can not delete from here. Please go to object(s) location and delete object(s)");
                ALERT.showAndWait();
                return;
            }

            if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.Directory)) {
                LOG.error("You can only recursively delete a folder.  Please select the folder to delete, Right click, and select 'Delete Folder...'");
                ALERT.setContentText("You can only recursively delete a folder.  Please select the folder to delete, Right click, and select 'Delete Folder...'");
                ALERT.showAndWait();
                return;
            }

            if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.Bucket)) {
                final String bucketName = ds3TreeTableView.getSelectionModel().getSelectedItem().getValue().getBucketName();
                deleteBucket(session, bucketName, values);
            }

            if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.File)) {
                deleteFiles(session, values);
            }
        } else {
            ALERT.setContentText("Invalid Session!");
            ALERT.showAndWait();
        }

    }

    /**
     * Delete a Single Selected Spectra S3 bucket
     *
     * @param session    session object
     * @param bucketName bucket name
     * @param values     selected items
     */
    private void deleteBucket(final Session session, final String bucketName, final ImmutableList<TreeItem<Ds3TreeTableValue>> values) {
        LOG.info("Got delete bucket event");

        final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());

        if (buckets.size() > 1) {
            deepStorageBrowserPresenter.logText("The user selected objects from multiple buckets.  This is not allowed.", LogType.ERROR);
            LOG.error("The user selected objects from multiple buckets.  This is not allowed.");
            ALERT.setContentText("The user selected objects from multiple buckets.  This is not allowed.");
            ALERT.showAndWait();
            return;
        }
        final TreeItem<Ds3TreeTableValue> value = values.stream().findFirst().orElse(null);
        if (!checkIfBucketEmpty(value.getValue().getBucketName())) {
            Platform.runLater(() -> {
                deepStorageBrowserPresenter.logText("Failed to delete Bucket as it contains files/folders", LogType.ERROR);
                ALERT.setContentText("You can not delete bucket as it contains files/folders");
                ALERT.showAndWait();
            });
        } else {
            final Ds3Task deleteBucketTask = new Ds3Task(session.getClient()) {
                @Override
                protected Object call() throws Exception {
                    try {
                        getClient().deleteBucketSpectraS3(new DeleteBucketSpectraS3Request(bucketName).withForce(true));
                    } catch (final IOException e) {
                        LOG.error("Failed to delte Bucket " + e);
                        Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete bucket", LogType.ERROR));
                        ALERT.setContentText("Failed to delete Bucket");
                        ALERT.showAndWait();
                    }
                    return null;
                }
            };
            DeleteFilesPopup.show(deleteBucketTask, this, null);
            values.stream().forEach(file -> refresh(file.getParent()));
            ParseJobInterruptionMap.refreshCompleteTreeTableView(ds3Common, workers);
            ds3PathIndicator.setText("");
        }
    }

    /**
     * Delete multiple selected files
     *
     * @param session session object
     * @param values  selected items
     */
    private void deleteFiles(final Session session, final ImmutableList<TreeItem<Ds3TreeTableValue>> values) {
        LOG.info("Got delete files event");

        final Ds3Task deleteFilesTask = new Ds3Task(session.getClient()) {

            final ArrayList<Ds3TreeTableValue> filesToDelete = new ArrayList<>(values
                    .stream()
                    .map(TreeItem::getValue)
                    .collect(Collectors.toList())
            );

            final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());

            @Override
            protected Object call() throws Exception {
                try {
                    final DeleteObjectsResponse response = getClient().deleteObjects(new DeleteObjectsRequest(buckets.get(0), filesToDelete.stream().map(Ds3TreeTableValue::getFullName).collect(Collectors.toList())));
                    Platform.runLater(() -> {
                        // deepStorageBrowserPresenter.logText("Delete response code: " + response.getStatusCode(), LogType.SUCCESS);
                        deepStorageBrowserPresenter.logText("Successfully deleted file(s)", LogType.SUCCESS);
                    });
                } catch (final IOException e) {
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to delete files" + e.toString(), LogType.ERROR));
                    LOG.error("Failed to delete files" + e);
                    ALERT.setContentText("Failed to delete files");
                    ALERT.showAndWait();
                }
                return null;
            }
        };
        DeleteFilesPopup.show(deleteFilesTask, this, null);
        values.stream().forEach(file -> refresh(file.getParent()));
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = getTreeTableView();
        ds3TreeTableView.getSelectionModel().clearSelection();
        ds3PathIndicator.setText("");
    }

    private void ds3NewFolder() {
        LOG.info("Create New Folder Prompt");

        final Session session = getSession();
        if (session != null) {
            @SuppressWarnings("unchecked")
            final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = getTreeTableView();
            final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView.getSelectionModel().getSelectedItems()
                    .stream().collect(GuavaCollectors.immutableList());

            if (values.isEmpty()) {
                deepStorageBrowserPresenter.logText("Select bucket/folder where you want to create an empty folder.", LogType.ERROR);
                ALERT.setContentText("Location is not selected");
                ALERT.showAndWait();
                return;
            }

            if (values.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
                LOG.error("You can not create folder here. Please refresh your view");
                ALERT.setContentText("You can not create folder here. Please refresh your view");
                ALERT.showAndWait();
                return;
            }

            if (values.size() > 1) {
                LOG.error("Only a single location can be selected to create empty folder");
                ALERT.setContentText("Only a single location can be selected to create empty folder");
                ALERT.showAndWait();
                return;
            }

            final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = values.stream().findFirst().orElse(null);
            //Can not assign final as assigning value again in next step
            final String location = ds3TreeTableValueTreeItem.getValue().getFullName();
            final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());
            CreateFolderPopup.show(new CreateFolderModel(session.getClient(), location, buckets.get(0)), deepStorageBrowserPresenter);
            refresh(ds3TreeTableValueTreeItem);
        } else {
            ALERT.setContentText("Invalid Session!");
            ALERT.showAndWait();
        }
    }

    private void initTabPane() {
        ds3SessionTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (ds3SessionTabPane.getTabs().size() > 1 && newValue == addNewTab) {
                // popup new session dialog box
                final int sessionCount = store.size();
                newSessionDialog();
                if (sessionCount == store.size()) {
                    // Do not select the new value if NewSessionDialog fails
                    ds3SessionTabPane.getSelectionModel().select(oldValue);
                }
            }
        });
        ds3SessionTabPane.getTabs().addListener((ListChangeListener<? super Tab>) c -> {
            if (c.next() && c.wasRemoved()) {
                // TODO prompt the user to save each session that was closed, if it is not already in the saved session store
            }
        });
    }

    private void refresh(final TreeItem<Ds3TreeTableValue> modifiedTreeItem) {
        LOG.info("Running refresh of row");
        deepStorageBrowserPresenter.logText("Running refresh of row", LogType.INFO);
        /*if (modifiedTreeItem instanceof Ds3TreeTableItem) {
            LOG.info("Refresh row");
            final Ds3TreeTableItem ds3TreeTableItem = (Ds3TreeTableItem) modifiedTreeItem;
            ds3TreeTableItem.refresh();
        }*/
        if (modifiedTreeItem instanceof Ds3TreeTableItem) {
            final Ds3TreeTableItem item;
            if (modifiedTreeItem.getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
                item = (Ds3TreeTableItem) modifiedTreeItem.getParent();
            } else {
                item = (Ds3TreeTableItem) modifiedTreeItem;
            }

            if (item.isExpanded()) {
                item.refresh();
            } else if (item.isAccessedChildren()) {
                item.setExpanded(true);
                item.refresh();
            } else {
                item.setExpanded(true);
            }
        }
    }

    private TreeTableView<Ds3TreeTableValue> getTreeTableView() {
        final VBox vbox = (VBox) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
        //noinspection unchecked
        return (TreeTableView<Ds3TreeTableValue>) vbox.getChildren().stream().filter(i -> i instanceof TreeTableView).findFirst().orElse(null);
    }

    public void deleteDialog() {
        // TODO get the currently selected tab, get the presenter for that tab, and then launch the delete dialog
        ds3SessionTabPane.getSelectionModel().getSelectedItem();
    }

    public void newSessionDialog() {
        NewSessionPopup.show();
    }

    private void initTab() {
        addNewTab.setGraphic(Icon.getIcon(FontAwesomeIcon.PLUS));
    }

    private void initMenuItems() {

        ds3RefreshToolTip.setText(resourceBundle.getString("ds3RefreshToolTip"));

        ds3NewFolderToolTip.setText(resourceBundle.getString("ds3NewFolderToolTip"));

        ds3NewBucketToolTip.setText(resourceBundle.getString("ds3NewBucketToolTip"));

        ds3DeleteButtonToolTip.setText(resourceBundle.getString("ds3DeleteButtonToolTip"));

        ds3PanelSearch.textProperty().addListener((observable, oldValue, newValue) -> {

            final Image icon = (newValue == null || newValue.isEmpty()) ? LENSICON : CROSSICON;
            imageView.setImage(icon);
            imageView.setMouseTransparent(icon == LENSICON);

            if (newValue.isEmpty()) {
                ParseJobInterruptionMap.refreshCompleteTreeTableView(ds3Common, workers);
            }

        });

        imageView.setOnMouseClicked(event -> ds3PanelSearch.setText(""));

        ds3PanelSearch.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                filterChanged(ds3PanelSearch.getText());
            }
        });

        if (ds3SessionTabPane.getTabs().size() == 1) {
            disableMenu(true);
        }
    }

    public Ds3PanelPresenter() {
        super();
    }

    public void filterChanged(final String newValue) {
        ds3PathIndicator.setText("Searching..");
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = getTreeTableView();
        final Session session = getSession();

        if (newValue.isEmpty()) {
            ParseJobInterruptionMap.refreshCompleteTreeTableView(ds3Common, workers);
        } else {
            try {
                final GetServiceResponse response = session.getClient().getService(new GetServiceRequest());
                final List<BucketDetails> buckets = response.getListAllMyBucketsResult().getBuckets();
                final SearchJob searchJob = new SearchJob(buckets, deepStorageBrowserPresenter, ds3TreeTableView, ds3PathIndicator, newValue, session, workers);

                workers.execute(searchJob);

                searchJob.setOnSucceeded(event -> LOG.info("Search completed!"));
                searchJob.setOnCancelled(event -> LOG.info("Search cancelled"));

            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void disableSearch(final boolean disable) {
        ds3PanelSearch.setDisable(disable);
    }

    private void initButtons() {
        newSessionButton.setText(resourceBundle.getString("newSessionButton"));
        ds3TransferLeft.setText(resourceBundle.getString("ds3TransferLeft"));
        ds3TransferLeftToolTip.setText(resourceBundle.getString("ds3TransferLeftToolTip"));
        final Tooltip imageToolTip = new Tooltip(resourceBundle.getString("imageViewForTooltip"));
        imageToolTip.setMaxWidth(150);
        imageToolTip.setWrapText(true);
        Tooltip.install(imageViewForTooltip, imageToolTip);

    }

    private void disableMenu(final boolean disable) {
        imageViewForTooltip.setDisable(disable);
        ds3Refresh.setDisable(disable);
        ds3NewFolder.setDisable(disable);
        ds3NewBucket.setDisable(disable);
        ds3DeleteButton.setDisable(disable);
        ds3PanelSearch.setDisable(disable);
        ds3TransferLeft.setDisable(disable);
    }

    /**
     * check if bucket contains files or folders
     *
     * @param bucketName
     * @return true if bucket is empty else return false
     */
    private boolean checkIfBucketEmpty(final String bucketName) {

        try {
            final GetBucketRequest request = new GetBucketRequest(bucketName).withDelimiter("/").withMaxKeys(1);
            final Session session = getSession();
            final GetBucketResponse bucketResponse = session.getClient().getBucket(request);
            final ListBucketResult listBucketResult = bucketResponse.getListBucketResult();
            if (listBucketResult.getObjects().size() == 0 && listBucketResult.getCommonPrefixes().size() == 0) {
                return true;
            } else {
                return false;
            }

        } catch (final Exception e) {
            LOG.error("could not get bucket response", e);
            return false;
        }

    }

    public String getSearchedText() {
        return ds3PanelSearch.getText();
    }

}


