package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.DeleteObjectsRequest;
import com.spectralogic.ds3client.commands.DeleteObjectsResponse;
import com.spectralogic.ds3client.commands.GetServiceRequest;
import com.spectralogic.ds3client.commands.GetServiceResponse;
import com.spectralogic.ds3client.commands.spectrads3.*;
import com.spectralogic.ds3client.models.BucketDetails;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketPopup;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketWithDataPoliciesModel;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderModel;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderPopup;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.*;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.Ds3GetJob;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeTableItem;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.LocalFileTreeTableProvider;
import com.spectralogic.dsbrowser.gui.components.modifyjobpriority.ModifyJobPriorityModel;
import com.spectralogic.dsbrowser.gui.components.modifyjobpriority.ModifyJobPriorityPopUp;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPopup;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
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

public class Ds3PanelPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PanelPresenter.class);
    private final static Alert ALERT = new Alert(Alert.AlertType.INFORMATION);
    final Image LENSICON = new Image(Ds3PanelPresenter.class.getResource("/images/lens.png").toString());
    final Image CROSSICON = new Image(Ds3PanelPresenter.class.getResource("/images/cross.png").toString());

    @FXML
    private Label ds3PathIndicator;

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
    private ImageView imageViewForTooltip;

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
    private SettingsStore settingsStore;

    @Inject
    private DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    public TreeTableView<Ds3TreeTableValue> ds3TreeTableView = null;

    @Inject
    private LocalFileTreeTableProvider provider;

    @Inject
    private DataFormat dataFormat;

    @FXML
    private ImageView imageView;

    @Inject
    Ds3Common ds3Common;

    @FXML
    private Ds3TreeTablePresenter ds3TreeTablePresenter;

    private SearchJob searchJob = null;

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

    private void initListeners() {

        ds3DeleteButton.setOnAction(event -> ds3DeleteObjects());

        ds3Refresh.setOnAction(event -> refreshCompleteTreeTableView());

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
                        Platform.runLater(() -> {
                            deepStorageBrowserPresenter.logText("Successfully retrieved data policies", LogType.SUCCESS);
                        });
                        if (dataPoliciesList.stream().findFirst().isPresent())
                            return dataPoliciesList.stream().findFirst().get();
                        else return null;
                    }
                };

                workers.execute(getDataPolicies);

                getDataPolicies.setOnSucceeded(taskEvent -> Platform.runLater(() -> {
                    LOG.info("Launching create bucket popup" + getDataPolicies.getValue().getDataPolicies().size());
                    CreateBucketPopup.show(getDataPolicies.getValue(), deepStorageBrowserPresenter);
                    refreshCompleteTreeTableView();
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
                        store.removeSession(newSession);
                        ds3Common.getExpandedNodesInfo().remove(newSession.getSessionName() + "-" + newSession.getEndpoint());
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
                        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView1 = (TreeTableView<Ds3TreeTableValue>) vbox.getChildren().stream().filter(i -> i instanceof TreeTableView).findFirst().get();
                        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView1.getSelectionModel().getSelectedItems()
                                .stream().collect(GuavaCollectors.immutableList());
                        if (values.size() == 0) {
                            ds3PathIndicator.setText("");
                        } else {
                            ds3PathIndicator.setText(values.stream().findFirst().get().getValue().getFullName());
                        }
                    } catch (Exception e) {
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
            imageView.setImage(new Image(Ds3PanelPresenter.class.getResource("/images/Settings.gif").toString()));
            button.setGraphic(imageView);
            button.setTooltip(new Tooltip("View/Modify Job Priority"));
            button.setOnAction(event -> {
                UUID jobId = null;
                if (task.getTitle().contains("PUT")) {
                    final Ds3PutJob ds3PutJob = (Ds3PutJob) task;
                    jobId = ds3PutJob.getJobId();
                }
                if (task.getTitle().contains("GET")) {
                    final Ds3GetJob ds3GetJob = (Ds3GetJob) task;
                    jobId = ds3GetJob.getJobId();
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
        if (store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(ds3SessionTabPane.getSelectionModel().getSelectedItem().getText())).findFirst().isPresent())
            return store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(ds3SessionTabPane.getSelectionModel().getSelectedItem().getText())).findFirst().get();
        else
            return null;
    }

    private void ds3TransferToLocal() {
        final Session session = getSession();
        if (session != null) {
            try {
                final ObservableList<javafx.scene.Node> list = deepStorageBrowserPresenter.getFileSystem().getChildren();
                final VBox vbox = (VBox) list.stream().filter(i -> i instanceof VBox).findFirst().get();
                final ObservableList<javafx.scene.Node> children = vbox.getChildren();
                @SuppressWarnings("unchecked")
                final TreeTableView<FileTreeModel> treeTable = (TreeTableView<FileTreeModel>) children.stream().filter(i -> i instanceof TreeTableView).findFirst().get();
                final Pane pane = (Pane) children.stream().filter(i -> i instanceof Pane).findFirst().get();
                final Label fileRootItemLabel = (Label) pane.getChildren().stream().filter(i -> i instanceof Label).findFirst().get();
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

                final List<FileTreeModel> selectedItemsAtDestinationList = selectedItemsAtDestination.stream().map(v -> v.getValue()).collect(Collectors.toList());
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

                final List<Ds3TreeTableValue> selectedItemsAtSourceLocationList = selectedItemsAtSourceLocation.stream().map(v -> v.getValue()).collect(Collectors.toList());
                final List<Ds3TreeTableValueCustom> selectedItemsAtSourceLocationListCustom = selectedItemsAtSourceLocationList.stream().map(v -> new Ds3TreeTableValueCustom(v.getBucketName(), v.getFullName(), v.getType(), v.getSize(), v.getLastModified(), v.getOwner(), v.isSearchOn())).collect(Collectors.toList());

                Path localPath = null;

                if (selectedItemsAtDestinationList.size() == 0) {
                    localPath = Paths.get(fileRootItem);
                } else if (selectedItemsAtDestinationList.stream().findFirst().get().getType().equals(FileTreeModel.Type.File)) {
                    localPath = selectedItemsAtDestinationList.stream().findFirst().get().getPath().getParent();
                } else
                    localPath = selectedItemsAtDestinationList.stream().findFirst().get().getPath();

                final String priority = (!savedJobPrioritiesStore.getJobSettings().getGetJobPriority().equals(resourceBundle.getString("defaultPolicyText"))) ? savedJobPrioritiesStore.getJobSettings().getGetJobPriority() : null;

                final Ds3GetJob getJob = new Ds3GetJob(selectedItemsAtSourceLocationListCustom, localPath, session.getClient(),
                        deepStorageBrowserPresenter, priority, settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads());
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

                    try {
                        final CancelJobSpectraS3Response cancelJobSpectraS3Response = session.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(getJob.getJobId()).withForce(true));
                        deepStorageBrowserPresenter.logText("GET Job Cancelled. Response code:" + cancelJobSpectraS3Response.getResponse().getStatusCode(), LogType.ERROR);
                    } catch (IOException e1) {
                        deepStorageBrowserPresenter.logText(" Failed to cancel job. ", LogType.ERROR);
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
            final TreeItem<FileTreeModel> selectedItem = selectedItemsAtDestination.stream().findFirst().get();
            if (selectedItem instanceof FileTreeTableItem) {
                final FileTreeTableItem fileTreeTableItem = (FileTreeTableItem) selectedItem;
                fileTreeTableItem.refresh();
                treeTable.getSelectionModel().clearSelection();
                treeTable.getSelectionModel().select(selectedItem);
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

        final Ds3Task deleteBucketTask = new Ds3Task(session.getClient()) {
            @Override
            protected Object call() throws Exception {
                try {
                    getClient().deleteBucketSpectraS3(new DeleteBucketSpectraS3Request(bucketName).withForce(true));
                } catch (final IOException e) {
                    LOG.error("Failed to delte Bucket " + e);
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Failed to delete bucket", LogType.ERROR);
                    });
                    ALERT.setContentText("Failed to delte Bucket");
                    ALERT.showAndWait();
                }
                return null;
            }
        };
        DeleteFilesPopup.show(deleteBucketTask, this, null);
        values.stream().forEach(file -> refresh(file.getParent()));
        refreshCompleteTreeTableView();
        ds3PathIndicator.setText("");
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
                        deepStorageBrowserPresenter.logText("Delete response code: " + response.getStatusCode(), LogType.SUCCESS);
                        deepStorageBrowserPresenter.logText("Successfully deleted file(s)", LogType.SUCCESS);
                    });
                } catch (final IOException e) {
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Failed to delete files" + e.toString(), LogType.ERROR);
                    });
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

            //Can not assign final as assigning value again in next step
            String location = values.stream().findFirst().get().getValue().getFullName();
            if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.File)) {
                location = values.stream().findFirst().get().getValue().getFullName();
            }

            final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());

            CreateFolderPopup.show(new CreateFolderModel(session.getClient(), location, buckets.get(0)), deepStorageBrowserPresenter);
            final TreeItem item = values.stream().findFirst().get();
            if (item.isExpanded())
                refresh(item);
            else
                item.setExpanded(true);

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
        if (modifiedTreeItem instanceof Ds3TreeTableItem) {
            LOG.info("Refresh row");
            final Ds3TreeTableItem ds3TreeTableItem = (Ds3TreeTableItem) modifiedTreeItem;
            ds3TreeTableItem.refresh();
        }
    }

    public void refreshCompleteTreeTableView() {

        deepStorageBrowserPresenter.logText("Refreshing session " + ds3SessionTabPane.getSelectionModel().getSelectedItem().getText(), LogType.INFO);
        LOG.info("session" + ds3SessionTabPane.getSelectionModel().getSelectedItem().getText());
        final Session session = getSession();
        @SuppressWarnings("unchecked")
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = getTreeTableView();
        final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();

        final Ds3Task getBucketTask = new Ds3Task(session.getClient()) {

            @Override
            protected Object call() throws Exception {
                try {
                    final GetServiceResponse response = session.getClient().getService(new GetServiceRequest());
                    final List<Ds3TreeTableValue> buckets = response.getListAllMyBucketsResult()
                            .getBuckets().stream()
                            .map(bucket -> {
                                HBox hbox = new HBox();
                                hbox.getChildren().add(new Label("----"));
                                hbox.setAlignment(Pos.CENTER);
                                return new Ds3TreeTableValue(bucket.getName(), bucket.getName(), Ds3TreeTableValue.Type.Bucket, FileSizeFormat.getFileSizeType(0), DateFormat.formatDate(bucket.getCreationDate()), "--", false, hbox);
                            })
                            .collect(Collectors.toList());
                    buckets.sort(Comparator.comparing(t -> t.getName().toLowerCase()));
                    final ImmutableList<Ds3TreeTableItem> treeItems = buckets.stream().map(value -> new Ds3TreeTableItem(value.getName(), session, value, workers)).collect(GuavaCollectors.immutableList());
                    rootTreeItem.getChildren().addAll(treeItems);
                    Platform.runLater(() -> {
                        ds3TreeTableView.setRoot(rootTreeItem);
                    });
                } catch (final Exception e) {
                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Failed to delete files" + e.toString(), LogType.ERROR);
                    });
                    LOG.error("Failed to delete files" + e);
                }
                return null;
            }
        };

        workers.execute(getBucketTask);

        getBucketTask.setOnSucceeded(event -> {
            LOG.info("Succeed");
            final ObservableList<TreeItem<Ds3TreeTableValue>> children = ds3TreeTableView.getRoot().getChildren();

            children.stream().forEach(i -> i.expandedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

                    final BooleanProperty bb = (BooleanProperty) observable;
                    final TreeItem<Ds3TreeTableValue> bean = (TreeItem<Ds3TreeTableValue>) bb.getBean();
                    ds3Common.getExpandedNodesInfo().put(session.getSessionName() + "-" + session.getEndpoint(), bean);
                }
            }));

            if (ds3Common.getExpandedNodesInfo().containsKey(session.getSessionName() + "-" + session.getEndpoint())) {
                final TreeItem<Ds3TreeTableValue> item = ds3Common.getExpandedNodesInfo().get(session.getSessionName() + "-" + session.getEndpoint());
                final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = children.stream().filter(i -> i.getValue().getBucketName().equals(item.getValue().getBucketName())).findFirst().get();
                ds3TreeTableValueTreeItem.setExpanded(false);
                if (!ds3TreeTableValueTreeItem.isLeaf() && !ds3TreeTableValueTreeItem.isExpanded()) {
                    LOG.info("Expanding closed row");
                    ds3TreeTableView.getSelectionModel().select(ds3TreeTableValueTreeItem);
                    ds3TreeTableValueTreeItem.setExpanded(true);
                }
            }
        });

        ds3PathIndicator.setText("");
    }

    private TreeTableView<Ds3TreeTableValue> getTreeTableView() {
        final VBox vbox = (VBox) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
        return (TreeTableView<Ds3TreeTableValue>) vbox.getChildren().stream().filter(i -> i instanceof TreeTableView).findFirst().get();
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
                refreshCompleteTreeTableView();
            }

        });

        imageView.setOnMouseClicked(event -> {
            searchJob.cancel(true);
            ds3PanelSearch.setText("");
        });

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

    private void filterChanged(final String newValue) {
        ds3PathIndicator.setText("Searching..");
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = getTreeTableView();
        final Session session = getSession();

        if (newValue.isEmpty()) {
            refreshCompleteTreeTableView();
        } else {
            try {
                final GetServiceResponse response = session.getClient().getService(new GetServiceRequest());
                final List<BucketDetails> buckets = response.getListAllMyBucketsResult().getBuckets();
                searchJob = new SearchJob(buckets, deepStorageBrowserPresenter, ds3TreeTableView, ds3PathIndicator, newValue, session, workers);

                workers.execute(searchJob);

                searchJob.setOnSucceeded(event -> {
                    LOG.info("Search completed!");
                });
                searchJob.setOnCancelled(event -> {
                    LOG.info("Search cancelled");
                });

            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
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
}


