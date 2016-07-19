package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.DeleteObjectsRequest;
import com.spectralogic.ds3client.commands.DeleteObjectsResponse;
import com.spectralogic.ds3client.commands.spectrads3.DeleteBucketSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetDataPoliciesSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetObjectsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetObjectsSpectraS3Response;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketPopup;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketWithDataPoliciesModel;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderModel;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderPopup;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTablePresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableView;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.Ds3GetJob;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeTableItem;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.LocalFileTreeTableProvider;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPopup;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Ds3PanelPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PanelPresenter.class);

    private final static Alert ALERT = new Alert(Alert.AlertType.INFORMATION);

    @FXML
    public Label ds3PathIndicator;

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
    private DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    public TreeTableView<Ds3TreeTableValue> ds3TreeTableView = null;

    @Inject
    private LocalFileTreeTableProvider provider;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Loading Ds3PanelPresenter");
            ALERT.setTitle("Error");
            ALERT.setHeaderText(null);
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

    private void initListeners() {

        ds3DeleteButton.setOnAction(event -> ds3DeleteObjects());

        ds3Refresh.setOnAction(event -> refreshCompleteTreeTableView());

        ds3NewFolder.setOnAction(event -> ds3NewFolder());

        ds3TransferLeft.setOnAction(event -> ds3TransferToLocal());

        ds3NewBucket.setOnAction(event -> {
            LOG.info("Create Bucket Prompt");
            final Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(ds3SessionTabPane.getSelectionModel().getSelectedItem().getText())).findFirst().get();
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
                        return dataPoliciesList.get(0);
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
                    final Ds3TreeTableView newTreeView = new Ds3TreeTableView(newSession, deepStorageBrowserPresenter, this);
                    final Tab treeTab = new Tab(newSession.getSessionName() + "-" + newSession.getEndpoint(), newTreeView.getView());
                    treeTab.setOnClosed(event -> {
                        store.removeSession(newSession);
                        deepStorageBrowserPresenter.logText(newSession.getSessionName() + "-" + newSession.getEndpoint() + " closed.", LogType.ERROR);
                    });
                    treeTab.setTooltip(new Tooltip(newSession.getSessionName() + "-" + newSession.getEndpoint()));
                    final int totalTabs = ds3SessionTabPane.getTabs().size();
                    ds3SessionTabPane.getTabs().add(totalTabs - 1, treeTab);
                    ds3SessionTabPane.getSelectionModel().select(treeTab);
                    deepStorageBrowserPresenter.logText("Starting " + newSession.getSessionName() + "-" + newSession.getEndpoint() + " session", LogType.SUCCESS);
                });
            }
        });

        ds3SessionTabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
            if (c.next() && c.wasRemoved()) {
                if (ds3SessionTabPane.getTabs().size() == 1) {
                    disableMenu(true);
                }
            } else if (c.wasAdded()) {
                disableMenu(false);
            }
        });

    }

    private void ds3TransferToLocal() {
        final Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(ds3SessionTabPane.getSelectionModel().getSelectedItem().getText())).findFirst().get();
        if (session != null) {
            try {
                final ObservableList<javafx.scene.Node> list = deepStorageBrowserPresenter.fileSystem.getChildren();
                final VBox vbox = (VBox) list.stream().filter(i -> i instanceof VBox).findFirst().get();
                final ObservableList<javafx.scene.Node> children = vbox.getChildren();
                @SuppressWarnings("unchecked")
                final TreeTableView<FileTreeModel> treeTable = (TreeTableView<FileTreeModel>) children.stream().filter(i -> i instanceof TreeTableView).findFirst().get();
                final ObservableList<TreeItem<FileTreeModel>> selectedItemsAtDestination = treeTable.getSelectionModel().getSelectedItems();

                if (selectedItemsAtDestination.isEmpty()) {
                    LOG.error("Location not selected");
                    ALERT.setContentText("Please select destination location");
                    ALERT.showAndWait();
                    return;
                }

                if (selectedItemsAtDestination.size() > 1) {
                    ALERT.setContentText("Multiple destinations not allowed. Please select One.");
                    ALERT.showAndWait();
                    return;
                }

                final List<FileTreeModel> selectedItemsAtDestinationList = selectedItemsAtDestination.stream().map(v -> v.getValue()).collect(Collectors.toList());
                @SuppressWarnings("unchecked")
                final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = (TreeTableView<Ds3TreeTableValue>) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
                final ImmutableList<TreeItem<Ds3TreeTableValue>> selectedItemsAtSourceLocation = ds3TreeTableView.getSelectionModel().getSelectedItems()
                        .stream().collect(GuavaCollectors.immutableList());

                if (selectedItemsAtSourceLocation.isEmpty()) {
                    LOG.error("Files not selected");
                    ALERT.setContentText("Please select files to transfer");
                    ALERT.showAndWait();
                    return;
                }

                final List<Ds3TreeTableValue> selectedItemsAtSourceLocationList = selectedItemsAtSourceLocation.stream().map(v -> v.getValue()).collect(Collectors.toList());
                final Ds3GetJob getJob = new Ds3GetJob(selectedItemsAtSourceLocationList, selectedItemsAtDestinationList.get(0), session.getClient(), deepStorageBrowserPresenter);
                getJob.setOnSucceeded(e -> {
                    LOG.info("job completed successfully");
                });

                jobWorkers.execute(getJob);

                getJob.setOnSucceeded(e -> {
                    TreeItem<FileTreeModel> selectedItem = selectedItemsAtDestination.get(0);
                    if (selectedItemsAtDestination.get(0).getValue().getType().equals(FileTreeModel.Type.File)) {
                        selectedItem = selectedItemsAtDestination.get(0).getParent();
                    }
                    final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(provider, selectedItem.getValue());
                    selectedItem.getChildren().remove(0, selectedItem.getChildren().size());
                    selectedItem.getChildren().addAll(newRootTreeItem.getChildren());
                    selectedItem.setExpanded(true);
                    treeTable.getSelectionModel().clearSelection();
                    treeTable.getSelectionModel().select(selectedItem);
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

    private void ds3DeleteObjects() {
        final Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(ds3SessionTabPane.getSelectionModel().getSelectedItem().getText())).findFirst().get();
        if (session != null) {

            ds3TreeTableView = (TreeTableView<Ds3TreeTableValue>) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
            final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView.getSelectionModel().getSelectedItems()
                    .stream().collect(GuavaCollectors.immutableList());

            if (values.isEmpty()) {
                LOG.error("No files selected");
                ALERT.setContentText("No files selected");
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
                String bucketName = ds3TreeTableView.getSelectionModel().getSelectedItem().getValue().getBucketName();
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
     * @param session
     * @param bucketName
     * @param values
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
                } catch (final IOException | SignatureException e) {
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

    }

    /**
     * Delete multiple selected files
     *
     * @param session
     * @param values
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
                } catch (final IOException | SignatureException e) {
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
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = (TreeTableView<Ds3TreeTableValue>) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
        ds3TreeTableView.getSelectionModel().clearSelection();

    }

    private void ds3NewFolder() {
        LOG.info("Create New Folder Prompt");

        final Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(ds3SessionTabPane.getSelectionModel().getSelectedItem().getText())).findFirst().get();
        if (session != null) {
            @SuppressWarnings("unchecked")
            final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = (TreeTableView<Ds3TreeTableValue>) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
            final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView.getSelectionModel().getSelectedItems()
                    .stream().collect(GuavaCollectors.immutableList());

            if (values.isEmpty()) {
                deepStorageBrowserPresenter.logText("Select bucket/folder where you want to create an empty folder.", LogType.ERROR);
                ALERT.setContentText("Location is not selected");
                ALERT.showAndWait();
                return;
            }

            if (values.size() > 1) {
                LOG.error("Only a single location can be selected to create empty folder");
                ALERT.setContentText("Only a single location can be selected to create empty folder");
                ALERT.showAndWait();
                return;
            }

            String location = values.get(0).getValue().getFullName();
            if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.File)) {
                location = values.get(0).getParent().getValue().getFullName();
            }

            final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());

            CreateFolderPopup.show(new CreateFolderModel(session.getClient(), location, buckets.get(0)), deepStorageBrowserPresenter);
            refresh(values.get(0));

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
        final Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(ds3SessionTabPane.getSelectionModel().getSelectedItem().getText())).findFirst().get();
        @SuppressWarnings("unchecked")
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = (TreeTableView<Ds3TreeTableValue>) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
        final Ds3TreeTablePresenter ds3TreeTablePresenter = new Ds3TreeTablePresenter();
        ds3TreeTablePresenter.refreshTreeTableView(ds3TreeTableView, workers, session);
        ds3PathIndicator.setText("");
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
            if (newValue.equals("")) {
                refreshCompleteTreeTableView();
            }
        });

        ds3PanelSearch.setOnKeyPressed(new EventHandler<KeyEvent>() {

            @Override
            public void handle(final KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    filterChanged(ds3PanelSearch.getText());
                }
            }
        });

        if (ds3SessionTabPane.getTabs().size() == 1) {
            disableMenu(true);
        }
    }

    private void filterChanged(final String newValue) {
        ds3PathIndicator.setText("Searching..");
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = (TreeTableView<Ds3TreeTableValue>) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();

        final List<Ds3TreeTableItem> listOfItems = new ArrayList<>();

        if (newValue.isEmpty()) {
            refreshCompleteTreeTableView();
        } else {
            final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
            rootTreeItem.setExpanded(true);
            ds3TreeTableView.setShowRoot(false);
            final Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(ds3SessionTabPane.getSelectionModel().getSelectedItem().getText())).findFirst().get();

            final Ds3JobTask search = new Ds3JobTask() {

                @Override
                public void executeJob() throws Exception {
                    updateTitle("Searching....");
                    final GetObjectsSpectraS3Request request = new GetObjectsSpectraS3Request().withName("%" + newValue + "%");
                    final GetObjectsSpectraS3Response response = session.getClient().getObjectsSpectraS3(request);

                    Platform.runLater(() -> {
                        deepStorageBrowserPresenter.logText("Search response: " + response.getResponse().getStatusCode(), LogType.SUCCESS);
                        final ImmutableList<Ds3TreeTableValue> treeItems = response.getS3ObjectListResult().getS3Objects().stream()
                                .map(f -> new Ds3TreeTableValue(f.getBucketId().toString(), f.getName(), Ds3TreeTableValue.Type.File, "0.00", DateFormat.formatDate(f.getCreationDate()))).collect(GuavaCollectors.immutableList());
                        final ImmutableList<Ds3TreeTableItem> fileItems = treeItems.stream().map(item -> new Ds3TreeTableItem(item.getFullName().toString(), session, item, workers)).collect(GuavaCollectors.immutableList());
                        ds3PathIndicator.setText("Search result(s): " + fileItems.size() + " object(s) found");
                        deepStorageBrowserPresenter.logText("Search result(s): " + fileItems.size() + " object(s) found", LogType.INFO);
                        fileItems.stream().forEach(value -> {
                                    rootTreeItem.getChildren().add(value);
                                }
                        );
                        ds3TreeTableView.setRoot(rootTreeItem);

                    });
                }
            };

            jobWorkers.execute(search);

            search.setOnSucceeded(event -> Platform.runLater(() -> {
                LOG.info("Search completed!");
            }));
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
        ds3Refresh.setDisable(disable);
        ds3NewFolder.setDisable(disable);
        ds3NewBucket.setDisable(disable);
        ds3DeleteButton.setDisable(disable);
        ds3PanelSearch.setDisable(disable);
        ds3TransferLeft.setDisable(disable);
    }
}


