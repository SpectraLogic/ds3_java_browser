package com.spectralogic.dsbrowser.gui.components.ds3panel;

import java.io.IOException;
import java.net.URL;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.DeleteObjectsRequest;
import com.spectralogic.ds3client.commands.spectrads3.DeleteBucketSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetDataPoliciesSpectraS3Request;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketPopup;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketWithDataPoliciesModel;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTablePresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableView;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPopup;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import com.spectralogic.dsbrowser.util.Icon;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

public class Ds3PanelPresenter implements Initializable {
    private final static Logger LOG = LoggerFactory.getLogger(Ds3PanelPresenter.class);

    @FXML
    Button ds3Refresh, ds3NewFolder, ds3NewBucket, ds3DeleteButton, newSessionButton;

    @FXML
    Tooltip ds3RefreshToolTip, ds3NewFolderToolTip, ds3NewBucketToolTip, ds3DeleteButtonToolTip;

    @FXML
    TextField ds3PanelSearch;

    @FXML
    Tab addNewTab;

    @FXML
    TabPane ds3SessionTabPane;

    @Inject
    private Ds3SessionStore store;

    @Inject
    private Workers workers;

    @Inject
    private ResourceBundle resourceBundle;

    private final Alert alert = new Alert(Alert.AlertType.INFORMATION);

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Loading Ds3PanelPresenter");
            alert.setTitle("Error");
            alert.setHeaderText(null);
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

        ds3DeleteButton.setOnAction((event) -> {
            ds3DeleteObjects();
        });

        ds3Refresh.setOnAction(event -> {
            refreshCompleteTreeTableView();
        });

        ds3NewBucket.setOnAction(event -> {
            LOG.info("Create Bucket Prompt");
            Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(ds3SessionTabPane.getSelectionModel().getSelectedItem().getText())).findFirst().get();

            TreeTableView<Ds3TreeTableValue> ds3TreeTableView = (TreeTableView<Ds3TreeTableValue>) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
            final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView.getSelectionModel().getSelectedItems()
                    .stream().collect(GuavaCollectors.immutableList());

            final Task<CreateBucketWithDataPoliciesModel> getDataPolicies = new Task<CreateBucketWithDataPoliciesModel>() {

                @Override
                protected CreateBucketWithDataPoliciesModel call() throws Exception {
                    final Ds3Client client = session.getClient();
                    //final CreateBucketModel value = values.get(0).getValue();
                    final ImmutableList<CreateBucketModel> buckets = client.getDataPoliciesSpectraS3(new GetDataPoliciesSpectraS3Request()).getDataPolicyListResult().
                            getDataPolicies().stream().map(bucket -> new CreateBucketModel(bucket.getName())).collect(GuavaCollectors.immutableList());
                    final ImmutableList<CreateBucketWithDataPoliciesModel> dataPoliciesList = buckets.stream().map(policies ->
                            new CreateBucketWithDataPoliciesModel(buckets, session, workers)).collect(GuavaCollectors.immutableList());
                    return dataPoliciesList.get(0);
                }
            };

            workers.execute(getDataPolicies);

            getDataPolicies.setOnSucceeded(taskEvent -> Platform.runLater(() -> {
                LOG.info("Launching create bucket popup" + getDataPolicies.getValue().getDataPolicies().size());
                CreateBucketPopup.show(getDataPolicies.getValue());
                refreshCompleteTreeTableView();
            }));
        });

        store.getObservableList().addListener((ListChangeListener<Session>) c -> {
            if (c.next() && c.wasAdded()) {
                final List<? extends Session> newItems = c.getAddedSubList();
                newItems.stream().forEach(newSession -> {
                    final Ds3TreeTableView newTreeView = new Ds3TreeTableView(newSession);
                    final Tab treeTab = new Tab(newSession.getSessionName() + "-" + newSession.getEndpoint(), newTreeView.getView());
                    treeTab.setOnClosed(event -> {
                        store.removeSession(newSession);
                    });
                    treeTab.setTooltip(new Tooltip(newSession.getSessionName() + "-" + newSession.getEndpoint()));
                    final int totalTabs = ds3SessionTabPane.getTabs().size();
                    ds3SessionTabPane.getTabs().add(totalTabs - 1, treeTab);
                    ds3SessionTabPane.getSelectionModel().select(treeTab);
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

    private void ds3DeleteObjects() {
    	Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(ds3SessionTabPane.getSelectionModel().getSelectedItem().getText())).findFirst().get();
        if ((session.getSessionName()+"-"+session.getEndpoint()).equals(ds3SessionTabPane.getSelectionModel().getSelectedItem().getText())) {
            TreeTableView<Ds3TreeTableValue> ds3TreeTableView = (TreeTableView<Ds3TreeTableValue>) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
            final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTableView.getSelectionModel().getSelectedItems()
                    .stream().collect(GuavaCollectors.immutableList());

            if (values.isEmpty()) {
                LOG.error("No files selected");
                alert.setContentText("No files selected");
                alert.showAndWait();
                return;
            }

            if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.DIRECTORY)) {
                LOG.error("You can only recursively delete a folder.  Please select the folder to delete, Right click, and select 'Delete Folder...'");
                alert.setContentText("You can only recursively delete a folder.  Please select the folder to delete, Right click, and select 'Delete Folder...'");
                alert.showAndWait();
                return;
            }

            if (values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.BUCKET)) {
                LOG.error("You delete a bucket");
                String bucketName = ds3TreeTableView.getSelectionModel().getSelectedItem().getValue().getBucketName();
                deleteBucket(session,bucketName,values);
            }

            if(values.stream().map(TreeItem::getValue).anyMatch(value -> value.getType() == Ds3TreeTableValue.Type.FILE))
            {
            	LOG.error("you can delete files.");
            	deleteFiles(session,values);
            }
        }

    }

    /**
     * Delete a Single Selected Spectra S3 bucket
     * @param session
     * @param bucketName
     * @param values
     */
    private void deleteBucket(final Session session,final String bucketName,final ImmutableList<TreeItem<Ds3TreeTableValue>> values) {

    	final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());

        if (buckets.size() > 1) {
            LOG.error("The user selected objects from multiple buckets.  This is not allowed.");
            alert.setContentText("The user selected objects from multiple buckets.  This is not allowed.");
            alert.showAndWait();
            return;
        }

    	final Ds3Task deleteBucketTask = new Ds3Task(session.getClient()) {
            @Override
            protected Object call() throws Exception {
                try {
                    getClient().deleteBucketSpectraS3(new DeleteBucketSpectraS3Request(bucketName));
                } catch (final IOException | SignatureException e) {
                    LOG.error("Failed to delte Bucket " + e);
                    alert.setContentText("Failed to delte Bucket");
                    alert.showAndWait();
                }
                return null;
            }
        };
        DeleteFilesPopup.show(deleteBucketTask);
        values.stream().forEach(file -> refresh(file.getParent()));
        refreshCompleteTreeTableView();

	}

    /**
     * Delete multiple selected files
     * @param session
     * @param values
     */
	private void deleteFiles(final Session session,final ImmutableList<TreeItem<Ds3TreeTableValue>> values) {

		final Ds3Task delteFilesTask = new Ds3Task(session.getClient()) {

			final ArrayList<Ds3TreeTableValue> filesToDelete = new ArrayList<>(values
                    .stream()
                    .map(TreeItem::getValue)
                    .collect(Collectors.toList())
            );

		   final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());

            @Override
            protected Object call() throws Exception {
                try {
                    getClient().deleteObjects(new DeleteObjectsRequest(buckets.get(0), filesToDelete.stream().map(Ds3TreeTableValue::getFullName).collect(Collectors.toList())));
                } catch (final IOException | SignatureException e) {
                    LOG.error("Failed to delete files" + e);
                    alert.setContentText("Failed to delete files");
                    alert.showAndWait();
                }
                return null;
            }
        };
        DeleteFilesPopup.show(delteFilesTask);
        values.stream().forEach(file -> refresh(file.getParent()));
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
        if (modifiedTreeItem instanceof Ds3TreeTableItem) {
            LOG.info("Refresh row");
            final Ds3TreeTableItem ds3TreeTableItem = (Ds3TreeTableItem) modifiedTreeItem;
            ds3TreeTableItem.refresh();
        }
    }

    public void refreshCompleteTreeTableView() {
        LOG.info("session" + ds3SessionTabPane.getSelectionModel().getSelectedItem().getText());
        Session session = store.getSessions().filter(sessions -> (sessions.getSessionName() + "-" + sessions.getEndpoint()).equals(ds3SessionTabPane.getSelectionModel().getSelectedItem().getText())).findFirst().get();
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = (TreeTableView<Ds3TreeTableValue>) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
        final Ds3TreeTablePresenter ds3TreeTablePresenter = new Ds3TreeTablePresenter();
        ds3TreeTablePresenter.refreshTreeTableView(ds3TreeTableView, workers, session);
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
        ds3Refresh.setGraphic(Icon.getIcon(FontAwesomeIcon.REFRESH));

        ds3NewFolderToolTip.setText(resourceBundle.getString("ds3NewFolderToolTip"));
        ds3NewFolder.setGraphic(Icon.getIcon(FontAwesomeIcon.FOLDER));

        ds3NewBucketToolTip.setText(resourceBundle.getString("ds3NewBucketToolTip"));
        ds3NewBucket.setGraphic(Icon.getIcon(FontAwesomeIcon.ARCHIVE));

        ds3DeleteButtonToolTip.setText(resourceBundle.getString("ds3DeleteButtonToolTip"));
        ds3DeleteButton.setGraphic(Icon.getIcon(FontAwesomeIcon.TRASH));
        if (ds3SessionTabPane.getTabs().size() == 1) {
            disableMenu(true);
        }
    }

    private void initButtons() {
        newSessionButton.setText(resourceBundle.getString("newSessionButton"));
    }

    private void disableMenu(final boolean disable) {
        ds3Refresh.setDisable(disable);
        ds3NewFolder.setDisable(disable);
        ds3NewBucket.setDisable(disable);
        ds3DeleteButton.setDisable(disable);
    }
}


