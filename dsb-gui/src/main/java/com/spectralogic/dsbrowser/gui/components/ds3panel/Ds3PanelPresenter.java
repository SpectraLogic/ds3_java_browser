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

package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.*;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeTableItem;
import com.spectralogic.dsbrowser.gui.components.modifyjobpriority.ModifyJobPriorityModel;
import com.spectralogic.dsbrowser.gui.components.modifyjobpriority.ModifyJobPriorityPopUp;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPopup;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.CreateService;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.DeleteService;
import com.spectralogic.dsbrowser.gui.services.ds3Panel.Ds3PanelService;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.*;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
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
import java.util.stream.Stream;

@Presenter
public class Ds3PanelPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PanelPresenter.class);

    private final Image LENS_ICON = new Image(ImageURLs.LENS_ICON);
    private final Image CROSS_ICON = new Image(ImageURLs.CROSS_ICON);

    private final LazyAlert alert = new LazyAlert("Error");

    @FXML
    private Label ds3PathIndicator;

    @FXML
    private Label infoLabel;

    @FXML
    private Label capacityLabel;

    @FXML
    private Label paneItems;

    @FXML
    private Tooltip ds3PathIndicatorTooltip;

    @FXML
    private Button ds3ParentDir, ds3Refresh, ds3NewFolder, ds3NewBucket, ds3DeleteButton, newSessionButton, ds3TransferLeft;

    @FXML
    private Tooltip ds3ParentDirToolTip, ds3RefreshToolTip, ds3NewFolderToolTip, ds3NewBucketToolTip, ds3DeleteButtonToolTip, ds3TransferLeftToolTip;

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

    private final ResourceBundle resourceBundle;
    private final Ds3SessionStore ds3SessionStore;
    private final Workers workers;
    private final JobWorkers jobWorkers;
    private final SavedJobPrioritiesStore savedJobPrioritiesStore;
    private final JobInterruptionStore jobInterruptionStore;
    private final SettingsStore settingsStore;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final FileTreeTableProvider fileTreeTableProvider;
    private final DataFormat dataFormat;
    private final Ds3Common ds3Common;
    private final SavedSessionStore savedSessionStore;
    private final LoggingService loggingService;

    private GetNoOfItemsTask itemsTask;

    @Inject
    public Ds3PanelPresenter(final ResourceBundle resourceBundle,
                             final Ds3SessionStore ds3SessionStore,
                             final Workers workers,
                             final JobWorkers jobWorkers,
                             final SavedJobPrioritiesStore savedJobPrioritiesStore,
                             final JobInterruptionStore jobInterruptionStore,
                             final SettingsStore settingsStore,
                             final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
                             final FileTreeTableProvider fileTreeTableProvider,
                             final DataFormat dataFormat,
                             final Ds3Common ds3Common,
                             final SavedSessionStore savedSessionStore,
                             final LoggingService loggingService) {
        this.resourceBundle = resourceBundle;
        this.ds3SessionStore = ds3SessionStore;
        this.workers = workers;
        this.jobWorkers = jobWorkers;
        this.savedJobPrioritiesStore = savedJobPrioritiesStore;
        this.jobInterruptionStore = jobInterruptionStore;
        this.settingsStore = settingsStore;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.fileTreeTableProvider = fileTreeTableProvider;
        this.dataFormat = dataFormat;
        this.ds3Common = ds3Common;
        this.savedSessionStore = savedSessionStore;
        this.loggingService = loggingService;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Loading Ds3PanelPresenter");
            ds3PathIndicator = makeSelectable(ds3PathIndicator);
            ds3PathIndicator.setTooltip(null);
            initMenuItems();
            initButtons();
            initTab();
            initTabPane();
            initListeners();
            ds3Common.setDs3PanelPresenter(this);
            ds3Common.setDeepStorageBrowserPresenter(deepStorageBrowserPresenter);

            //open default session when DSB launched
            savedSessionStore.openDefaultSession(ds3SessionStore);
        } catch (final Throwable t) {
            LOG.error("Encountered error when initializing Ds3PanelPresenter", t);
        }
    }

    private Label makeSelectable(final Label label) {
        final StackPane textStack = new StackPane();
        final TextField textField = new TextField(label.getText());
        textField.setEditable(false);
        textField.getStyleClass().add("selectableClass");

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

    /**
     * To move to parent directory.
     */
    private void goToParentDirectory() {
        //if root is null back button will not work
        if (null != getTreeTableView().getRoot().getValue() &&
                null != getTreeTableView().getRoot().getParent()) {
            if (null == getTreeTableView().getRoot().getParent().getValue()) {
                getDs3PathIndicator().setText(StringConstants.EMPTY_STRING);
                getDs3PathIndicator().setTooltip(null);
                capacityLabel.setText(StringConstants.EMPTY_STRING);
                capacityLabel.setVisible(false);
                infoLabel.setText(StringConstants.EMPTY_STRING);
                infoLabel.setVisible(false);
            } else {
                getDs3PathIndicator().setTooltip(getDs3PathIndicatorTooltip());
            }
            getTreeTableView().setRoot(getTreeTableView().getRoot().getParent());
            getTreeTableView().getRoot().getChildren().forEach(treeItem -> treeItem.setExpanded(false));
            try {
                final ProgressIndicator progress = new ProgressIndicator();
                progress.setMaxSize(90, 90);
                ((Ds3TreeTableItem) ds3Common.getDs3PanelPresenter().getTreeTableView().getRoot()).refresh();
                ds3Common.getDs3PanelPresenter().getTreeTableView().refresh();
            } catch (final Exception e) {
                LOG.error("Unable to change root", e);
            }
        } else {
            getDs3PathIndicator().setText(StringConstants.EMPTY_STRING);
            getDs3PathIndicator().setTooltip(null);

        }
    }

    @SuppressWarnings("unchecked")
    private void initListeners() {
        ds3DeleteButton.setOnAction(event -> ds3DeleteObject());
        ds3Refresh.setOnAction(event -> RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers, loggingService));
        ds3ParentDir.setOnAction(event -> goToParentDirectory());
        ds3NewFolder.setOnAction(event -> CreateService.createFolderPrompt(ds3Common, loggingService, resourceBundle));
        ds3TransferLeft.setOnAction(event -> ds3TransferToLocal());
        ds3NewBucket.setOnAction(event -> {
            LOG.debug("Attempting to create bucket...");
            CreateService.createBucketPrompt(ds3Common, workers, loggingService, resourceBundle);
        });

        ds3SessionStore.getObservableList().addListener((ListChangeListener<Session>) c -> {
            if (c.next() && c.wasAdded()) {
                final List<? extends Session> newItems = c.getAddedSubList();
                newItems.forEach(newSession -> {
                    createTabAndSetBehaviour(newSession);
                    loggingService.logMessage(resourceBundle.getString("starting") + StringConstants.SPACE +
                            newSession.getSessionName() + StringConstants.SESSION_SEPARATOR + newSession.getEndpoint()
                            + StringConstants.SPACE + resourceBundle.getString("session"), LogType.SUCCESS);
                });
            }
        });

        ds3SessionTabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
                    try {
                        if (newTab.getContent() instanceof VBox) {
                            final VBox vbox = (VBox) newTab.getContent();

                            final Optional<Node> first = vbox.getChildren().stream().filter(i -> i instanceof TreeTableView).findFirst();

                            if (first.isPresent()) {
                                final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = (TreeTableView<Ds3TreeTableValue>) first.get();
                                final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView.getSelectionModel().getSelectedItems()
                                        .stream().collect(GuavaCollectors.immutableList());
                                ds3Common.setDs3TreeTableView(ds3TreeTableView);
                                ds3Common.setCurrentTabPane(ds3SessionTabPane);

                                final String info = StringBuilderUtil.getPaneItemsString(ds3TreeTableView.getExpandedItemCount(), ds3TreeTableView.getSelectionModel().getSelectedItems().size()).toString();
                                if (Guard.isNullOrEmpty(values)) {
                                    setBlank(true);
                                } else {
                                    setBlank(false);
                                    final Optional<TreeItem<Ds3TreeTableValue>> ds3TreeTableValueTreeItemElement = values.stream().findFirst();
                                    if (ds3TreeTableValueTreeItemElement.isPresent()) {
                                        final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = ds3TreeTableValueTreeItemElement.get();
                                        final Ds3TreeTableValue value = ds3TreeTableValueTreeItem.getValue();
                                        if (!value.getType().equals(Ds3TreeTableValue.Type.Bucket)) {
                                            ds3PathIndicator.setText(value.getBucketName() + StringConstants.FORWARD_SLASH + value.getFullName());
                                            ds3PathIndicatorTooltip.setText(value.getBucketName() + StringConstants.FORWARD_SLASH + value.getFullName());
                                        } else {
                                            ds3PathIndicator.setText(value.getBucketName());
                                            ds3PathIndicatorTooltip.setText(value.getBucketName());
                                        }
                                        calculateFiles(ds3TreeTableView);
                                    }
                                }
                                getPaneItems().setVisible(true);
                                getPaneItems().setText(info);
                            } else {
                                LOG.info("TreeTableView is null");
                            }
                        } else {
                            ds3Common.setCurrentSession(null);
                            setBlank(true);
                            disableSearch(true);
                        }
                    } catch (final Exception e) {
                        LOG.error("Not able to parse:", e);
                    }
                }
        );

        ds3SessionTabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
            if (c.next() && c.wasRemoved()) {
                if (ds3SessionTabPane.getTabs().size() == 1) {
                    disableMenu(true);
                }
                ds3Common.getDs3PanelPresenter().getInfoLabel().setVisible(false);
            } else if (c.wasAdded()) {
                disableMenu(false);
            }
        });

        deepStorageBrowserPresenter.getJobProgressView().setGraphicFactory(task -> {
            final ImageView imageView = new ImageView();
            imageView.setImage(new Image(ImageURLs.SETTINGS_ICON));
            final Button button = new Button();
            button.setGraphic(imageView);
            button.setTooltip(new Tooltip(resourceBundle.getString("viewOrModifyJobPriority")));
            button.setOnAction(event -> modifyJobPriority(task));
            return button;
        });

    }

    private void createTabAndSetBehaviour(final Session newSession) {
        addNewTab.setTooltip(new Tooltip(resourceBundle.getString("newSessionToolTip")));
        final Ds3TreeTableView newTreeView = new Ds3TreeTableView(newSession, this);
        final Tab treeTab = new Tab(newSession.getSessionName() + StringConstants.SESSION_SEPARATOR
                + newSession.getEndpoint(), newTreeView.getView());
        treeTab.setOnSelectionChanged(event -> {
            ds3Common.setCurrentSession(newSession);
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(
                    jobInterruptionStore.getJobIdsModel().getEndpoints(), newSession.getEndpoint()
                            + StringConstants.COLON + newSession.getPortNo(),
                    deepStorageBrowserPresenter.getJobProgressView(), null);
            ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);

        });
        treeTab.setOnCloseRequest(event -> ds3Common.setSessionOfClosedTab(getSession()));
        treeTab.setOnClosed(event -> closeTab((Tab) event.getSource()));
        treeTab.setTooltip(new Tooltip(newSession.getSessionName() + StringConstants.SESSION_SEPARATOR + newSession.getEndpoint()));
        final int totalTabs = ds3SessionTabPane.getTabs().size();
        ds3SessionTabPane.getTabs().add(totalTabs - 1, treeTab);
        ds3SessionTabPane.getSelectionModel().select(treeTab);
    }

    private void modifyJobPriority(final Ds3JobTask task) {
        {
            final UUID jobId = task.getJobId();
            if (getSession() != null) {
                if (jobId != null) {

                    final GetJobPriorityTask jobPriorityTask = new GetJobPriorityTask(getSession(), jobId);

                    workers.execute(jobPriorityTask);
                    jobPriorityTask.setOnSucceeded(eventPriority -> Platform.runLater(() -> {
                        LOG.info("Launching metadata popup");
                        ModifyJobPriorityPopUp.show((ModifyJobPriorityModel) jobPriorityTask.getValue(), resourceBundle);
                    }));
                } else {
                    LOG.info("Job is not started yet");
                }
            } else {
                LOG.error("Null session.");
            }
        }
    }

    private void closeTab(final Tab closedTab) {
        {
            try {
                if (closedTab != null) {
                    final Session closedSession = ds3Common.getSessionOfClosedTab();
                    if (closedSession != null) {
                        CancelJobsWorker.cancelAllRunningJobsBySession(jobWorkers, jobInterruptionStore, workers, closedSession, loggingService);
                        ds3SessionStore.removeSession(closedSession);
                        ds3Common.getExpandedNodesInfo().remove(closedSession.getSessionName() +
                                StringConstants.SESSION_SEPARATOR + closedSession.getEndpoint());
                        ds3Common.setSessionOfClosedTab(null);
                        loggingService.logMessage(closedSession.getSessionName() +
                                StringConstants.SESSION_SEPARATOR + closedSession.getEndpoint() + StringConstants
                                .SPACE + resourceBundle.getString("closed"), LogType.ERROR);
                    }
                }
                final Session currentSession = getSession();
                if (currentSession != null) {
                    final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(),
                            currentSession.getEndpoint() + StringConstants.COLON + currentSession.getPortNo(),
                            deepStorageBrowserPresenter.getJobProgressView(), null);
                    ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);
                }
            } catch (final Exception e) {
                LOG.error("Failed to remove session:", e);
            }
            if (ds3SessionStore.size() == 0) {
                ds3PathIndicator.setText(StringConstants.EMPTY_STRING);
                addNewTab.setTooltip(null);
            }
        }
    }

    public void setBlank(final boolean isSetBlank) {
        if (isSetBlank) {
            ds3PathIndicator.setText(StringConstants.EMPTY_STRING);
            ds3PathIndicator.setTooltip(null);
            paneItems.setVisible(false);
            capacityLabel.setVisible(false);
            infoLabel.setVisible(false);
        } else {
            ds3PathIndicator.setTooltip(ds3PathIndicatorTooltip);
            paneItems.setVisible(true);
            capacityLabel.setVisible(true);
            infoLabel.setVisible(true);
            capacityLabel.setText(resourceBundle.getString("infoLabel"));
            infoLabel.setText(resourceBundle.getString("infoLabel"));
        }
    }

    private Session getSession() {
        return ds3Common.getCurrentSession();
    }

    private void ds3TransferToLocal() {
        final Session session = getSession();
        if ((session == null ) || (ds3Common == null) ) {
            alert.showAlert(resourceBundle.getString("invalidSession"));
            return;
        }

        // Verify valid remote TreeTableView
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = getTreeTableView();
        if (ds3TreeTableView == null) {
            LOG.info("Files not selected");
            alert.showAlert(resourceBundle.getString("fileSelect"));
            return;
        }

        // Verify remote files to GET selected
        if ((ds3TreeTableView.getSelectionModel() == null) || (ds3TreeTableView.getSelectionModel().getSelectedItems() == null)) {
            LOG.info("Files not selected");
            alert.showAlert(resourceBundle.getString("fileSelect"));
            return;
        }
        final ImmutableList<Ds3TreeTableValue> selectedItemsAtSourceLocationList = ds3TreeTableView.getSelectionModel()
                .getSelectedItems().stream()
                .map(TreeItem::getValue).collect(GuavaCollectors.immutableList());

        // Verify local destination selected
        final TreeTableView<FileTreeModel> localTreeTableView = ds3Common.getLocalTreeTableView();
        if (localTreeTableView == null) {
            return;
        }

        final Label localFilePathIndicator = ds3Common.getLocalFilePathIndicator();
        final String fileRootItem = localFilePathIndicator.getText();
        final ObservableList<TreeItem<FileTreeModel>> selectedItemsAtDestination = localTreeTableView.getSelectionModel().getSelectedItems();
        if (fileRootItem.equals(resourceBundle.getString("myComputer"))) {
            if (Guard.isNullOrEmpty(selectedItemsAtDestination)) {
                LOG.info("Location not selected");
                alert.showAlert(resourceBundle.getString("sourceFileSelectError"));
                return;
            }
        }
        if (selectedItemsAtDestination.size() > 1) {
            alert.showAlert(resourceBundle.getString("multipleDestError"));
            return;
        }
        final ImmutableList<FileTreeModel> selectedItemsAtDestinationList = selectedItemsAtDestination.stream()
                .map(TreeItem::getValue).collect(GuavaCollectors.immutableList());

        //Getting selected item at source location
        final ImmutableList<Ds3TreeTableValueCustom> selectedItemsAtSourceLocationListCustom =
                selectedItemsAtSourceLocationList.stream()
                        .map(v -> new Ds3TreeTableValueCustom(v.getBucketName(),
                                v.getFullName(), v.getType(), v.getSize(), v.getLastModified(),
                                v.getOwner(), v.isSearchOn())).collect(GuavaCollectors.immutableList());
        final Path localPath;

        //Getting selected item at destination location
        final Optional<FileTreeModel> first = selectedItemsAtDestinationList.stream().findFirst();
        if (!first.isPresent()) {
            localPath = Paths.get(fileRootItem);
        } else {
            final FileTreeModel selectedAtDest = first.get();
            if (selectedAtDest.getType().equals(FileTreeModel.Type.File)) {
                localPath = selectedAtDest.getPath().getParent();
            } else {
                localPath = selectedAtDest.getPath();
            }
        }

        final String priority = (!savedJobPrioritiesStore.getJobSettings().getGetJobPriority()
                .equals(resourceBundle.getString("defaultPolicyText"))) ?
                savedJobPrioritiesStore.getJobSettings().getGetJobPriority() : null;
        final Ds3GetJob getJob = new Ds3GetJob(selectedItemsAtSourceLocationListCustom, localPath, session.getClient(),
                priority, settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads(),
                jobInterruptionStore, deepStorageBrowserPresenter, resourceBundle, loggingService);
        jobWorkers.execute(getJob);
        getJob.setOnSucceeded(event -> {
            LOG.info("Get Job {} succeeded.", getJob.getJobId());
            refreshLocalSideView(selectedItemsAtDestination, localTreeTableView, localFilePathIndicator, fileRootItem);
        });
        getJob.setOnFailed(e -> {
            LOG.info("Get Job {} failed.", getJob.getJobId());
            refreshLocalSideView(selectedItemsAtDestination, localTreeTableView, localFilePathIndicator, fileRootItem);
        });
        getJob.setOnCancelled(e -> {
            LOG.info("Get Job {} cancelled.", getJob.getJobId());
            if (getJob.getJobId() != null) {
                try {
                    session.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(getJob.getJobId()));
                    ParseJobInterruptionMap.removeJobID(jobInterruptionStore, getJob.getJobId().toString(), getJob.getDs3Client().getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter, loggingService);
                    loggingService.logMessage(resourceBundle.getString("getJobCancelled"), LogType.ERROR);
                } catch (final IOException e1) {
                    LOG.error("Failed to cancel job " + getJob.getJobId(), e1);
                }
            }
            refreshLocalSideView(selectedItemsAtDestination, localTreeTableView, localFilePathIndicator, fileRootItem);
        });
    }

    private void refreshLocalSideView(final ObservableList<TreeItem<FileTreeModel>> selectedItemsAtDestination,
                                      final TreeTableView<FileTreeModel> treeTable,
                                      final Label fileRootItemLabel,
                                      final String fileRootItem) {
        final Optional<TreeItem<FileTreeModel>> first = selectedItemsAtDestination.stream().findFirst();
        if (first.isPresent()) {
            final TreeItem<FileTreeModel> selectedItem = first.get();
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
            final Stream<FileTreeModel> rootItems = fileTreeTableProvider.getRoot(fileRootItem);
            fileRootItemLabel.setText(fileRootItem);
            rootItems.forEach(ftm -> {
                final TreeItem<FileTreeModel> newRootTreeItem = new FileTreeTableItem(fileTreeTableProvider, ftm, workers);
                rootTreeItem.getChildren().add(newRootTreeItem);
            });

            treeTable.setRoot(rootTreeItem);
        }
    }

    public void ds3DeleteObject() {
        LOG.info("Got delete object event");
        final TreeTableView<Ds3TreeTableValue> ds3TreeTable = ds3Common.getDs3TreeTableView();
        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());
        final TreeItem<Ds3TreeTableValue> root = ds3TreeTable.getRoot();
        if (Guard.isNullOrEmpty(values)) {
            if (root.getValue() == null) {
                LOG.info("No files selected");
                alert.showAlert(resourceBundle.getString("noFiles"));
            }
        } else if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.Directory)) {
            values.stream().map(TreeItem::toString).forEach(itemString -> LOG.info("Delete folder {}", itemString));
            DeleteService.deleteFolders(ds3Common, values);
        } else if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.Bucket)) {
            LOG.info("Going to delete the bucket");
            DeleteService.deleteBucket(ds3Common, values, workers, loggingService, resourceBundle);
        } else if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.File)) {
            LOG.info("Going to delete the file(s)");
            DeleteService.deleteFiles(ds3Common, values);
        }
    }

    private void initTabPane() {
        ds3SessionTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (ds3SessionTabPane.getTabs().size() > 1 && newValue == addNewTab) {
                // popup new session dialog box
                final int sessionCount = ds3SessionStore.size();
                newSessionDialog();
                if (sessionCount == ds3SessionStore.size()) {
                    // Do not select the new value if NewSessionDialog fails
                    ds3SessionTabPane.getSelectionModel().select(oldValue);
                }
            }
        });
    }


    @SuppressWarnings("unchecked")
    private TreeTableView<Ds3TreeTableValue> getTreeTableView() {
        final VBox vbox = (VBox) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();

        final Optional<Node> first = vbox.getChildren().stream().filter(i ->
                i instanceof TreeTableView).findFirst();
        if (first.isPresent()) {
            return (TreeTableView<Ds3TreeTableValue>) first.get();
        }
        return null;
    }

    public void newSessionDialog() {
        NewSessionPopup.show(resourceBundle);
    }

    private void initTab() {
        addNewTab.setGraphic(Icon.getIcon(FontAwesomeIcon.PLUS));
    }

    private void initMenuItems() {
        ds3ParentDirToolTip.setText(resourceBundle.getString("ds3ParentDirToolTip"));
        ds3RefreshToolTip.setText(resourceBundle.getString("ds3RefreshToolTip"));
        ds3NewFolderToolTip.setText(resourceBundle.getString("ds3NewFolderToolTip"));
        ds3NewBucketToolTip.setText(resourceBundle.getString("ds3NewBucketToolTip"));
        ds3DeleteButtonToolTip.setText(resourceBundle.getString("ds3DeleteButtonToolTip"));
        ds3PanelSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            final Image icon = (Guard.isStringNullOrEmpty(newValue)) ? LENS_ICON : CROSS_ICON;
            imageView.setImage(icon);
            imageView.setMouseTransparent(icon == LENS_ICON);
            if (Guard.isStringNullOrEmpty(newValue)) {
                RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers, loggingService);
            }
        });
        imageView.setOnMouseClicked(event -> ds3PanelSearch.setText(StringConstants.EMPTY_STRING));
        ds3PanelSearch.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Ds3PanelService.filterChanged(ds3Common, workers, loggingService, resourceBundle);
            }
        });
        if (ds3SessionTabPane.getTabs().size() == 1) {
            disableMenu(true);
        }
    }

    public void disableSearch(final boolean disable) {
        ds3PanelSearch.setText(StringConstants.EMPTY_STRING);
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
        if (disable) {
            ds3PathIndicator.setTooltip(null);
        }
        imageViewForTooltip.setDisable(disable);
        ds3ParentDir.setDisable(disable);
        ds3Refresh.setDisable(disable);
        ds3NewFolder.setDisable(disable);
        ds3NewBucket.setDisable(disable);
        ds3DeleteButton.setDisable(disable);
        ds3PanelSearch.setDisable(disable);
        ds3TransferLeft.setDisable(disable);
    }

    public String getSearchedText() {
        return ds3PanelSearch.getText();
    }

    //Method for calculating no. of files and capacity of selected tree item
    public void calculateFiles(final TreeTableView<Ds3TreeTableValue> ds3TreeTableView) {
        //if a task for calculating of items is already running and cancel that task
        if (itemsTask != null) {
            itemsTask.cancel(true);
        }
        try {
            ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = ds3TreeTableView.getSelectionModel().getSelectedItems();
            final TreeItem<Ds3TreeTableValue> root = ds3TreeTableView.getRoot();
            if (Guard.isNullOrEmpty(selectedItems) && root != null && root.getValue() != null) {
                selectedItems = FXCollections.observableArrayList();
                selectedItems.add(root);
            }
            //start a new task for calculating
            itemsTask = new GetNoOfItemsTask(ds3Common, selectedItems);
            workers.execute(itemsTask);

            itemsTask.setOnSucceeded(event -> Platform.runLater(() -> {
                final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView.getSelectionModel().getSelectedItems()
                        .stream().collect(GuavaCollectors.immutableList());
                TreeItem<Ds3TreeTableValue> selectedRoot = ds3TreeTableView.getRoot();
                if (!Guard.isNullOrEmpty(values)) {
                    final Optional<TreeItem<Ds3TreeTableValue>> first = values.stream().findFirst();
                    if (first.isPresent()) {
                        selectedRoot = first.get();
                    }
                }
                //for number of files and folders
                final FilesCountModel filesCountModel = itemsTask.getValue();
                if (selectedRoot == null || selectedRoot.getValue() == null || getSession() == null || null == filesCountModel) {
                    setVisibilityOfItemsInfo(false);
                } else {
                    setVisibilityOfItemsInfo(true);
                    setItemCountPanelInfo(filesCountModel, selectedRoot);
                }

            }));

        } catch (final Exception e) {
            LOG.error("Unable to calculate no. of items and capacity", e);
        }
    }

    private void setItemCountPanelInfo(final FilesCountModel filesCountModel, final TreeItem<Ds3TreeTableValue> selectedRoot) {
        //For no. of folder(s) and file(s)
        if (filesCountModel.getNoOfFiles() == 0 && filesCountModel.getNoOfFolders() == 0) {
            getInfoLabel().setText(resourceBundle.getString("containsNoItem"));
        } else {
            getInfoLabel().setText(StringBuilderUtil.getItemsCountInfoMessage(filesCountModel.getNoOfFolders(),
                    filesCountModel.getNoOfFiles()).toString());
        }
        //For capacity of bucket or folder
        getCapacityLabel().setText(StringBuilderUtil.getCapacityMessage(filesCountModel.getTotalCapacity(),
                selectedRoot.getValue().getType()).toString());
    }

    private void setVisibilityOfItemsInfo(final boolean visibility) {
        getInfoLabel().setVisible(visibility);
        getCapacityLabel().setVisible(visibility);
    }

    public Label getCapacityLabel() {
        return capacityLabel;
    }

    public void setDs3TreeTablePresenter(final Ds3TreeTablePresenter ds3TreeTablePresenter) {
        this.ds3TreeTablePresenter = ds3TreeTablePresenter;
    }

    public Label getDs3PathIndicator() {
        return ds3PathIndicator;
    }

    public Tooltip getDs3PathIndicatorTooltip() {
        return ds3PathIndicatorTooltip;
    }

    public Label getInfoLabel() {
        return infoLabel;
    }

    public Label getPaneItems() {
        return paneItems;
    }


}


