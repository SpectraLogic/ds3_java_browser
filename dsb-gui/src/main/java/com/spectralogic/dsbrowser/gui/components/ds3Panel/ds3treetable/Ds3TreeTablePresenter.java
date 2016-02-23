package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.GetServiceRequest;
import com.spectralogic.ds3client.commands.GetServiceResponse;
import com.spectralogic.ds3client.commands.HeadObjectRequest;
import com.spectralogic.ds3client.commands.HeadObjectResponse;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.metadata.Ds3Metadata;
import com.spectralogic.dsbrowser.gui.components.metadata.MetadataPopup;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Ds3TreeTablePresenter implements Initializable {
    private final static Logger LOG = LoggerFactory.getLogger(Ds3TreeTablePresenter.class);

    @FXML
    TreeTableView<Ds3TreeTableValue> ds3TreeTable;

    @Inject
    Workers workers;

    @Inject
    JobWorkers jobWorkers;

    @Inject
    Session session;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Loading Ds3TreeTablePresenter with session " + session.getSessionName());

            initTreeTableView();

        } catch (final Throwable e) {
            LOG.error("Encountered error when creating Ds3TreeTablePresenter", e);
            throw e;
        }
    }

    private void initTreeTableView() {
        ds3TreeTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ds3TreeTable.setRowFactory(view -> {

            final TreeTableRow<Ds3TreeTableValue> row = new TreeTableRow<>();

            row.setOnDragOver(event -> {
                if (event.getGestureSource() != ds3TreeTable && event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY);
                }

                event.consume();
            });

            row.setOnDragEntered(event -> {
                final TreeItem<Ds3TreeTableValue> treeItem = row.getTreeItem();

                if(treeItem != null) {
                    if (!treeItem.isLeaf() && !treeItem.isExpanded()) {
                        LOG.info("Expanding closed row");
                        treeItem.setExpanded(true);
                    }

                    final InnerShadow is = new InnerShadow();
                    is.setOffsetY(1.0f);

                    row.setEffect(is);
                }
                event.consume();
            });

            row.setOnDragExited(event -> {
                row.setEffect(null);
                event.consume();
            });

            row.setOnDragDropped(event -> {
                LOG.info("Got drop event");
                final Dragboard db = event.getDragboard();

                if (db.hasFiles()) {
                    LOG.info("Drop event contains files");
                    // get bucket info and current path
                    final TreeItem<Ds3TreeTableValue> treeItem = row.getTreeItem();
                    final Ds3TreeTableValue value = treeItem.getValue();
                    final String bucket = value.getBucketName();
                    final String targetDir = value.getDirectoryName();
                    LOG.info("Passing new Ds3PutJob to jobWorkers thread pool to be scheduled");

                    final Ds3PutJob putJob = new Ds3PutJob(session.getClient(), db.getFiles(), bucket, targetDir);

                    putJob.setOnSucceeded(e -> {
                        LOG.info("job completed successfully");
                        refresh(row.getTreeItem());
                    });

                    jobWorkers.execute(putJob);
                }
                event.consume();
            });

            final ContextMenu contextMenu = new ContextMenu();
            final MenuItem deleteFile = new MenuItem("Delete...");
            deleteFile.setOnAction(event -> deletePrompt());

            final MenuItem metaData = new MenuItem("Metadata...");
            metaData.setOnAction(event -> showMetadata());

            contextMenu.getItems().addAll(deleteFile, metaData);
            row.setContextMenu(contextMenu);

            return row;
        });


        final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
        rootTreeItem.setExpanded(true);
        ds3TreeTable.setShowRoot(false);

        final GetServiceTask getServiceTask = new GetServiceTask(rootTreeItem.getChildren());
        ds3TreeTable.setRoot(rootTreeItem);
        workers.execute(getServiceTask);
    }

    public void showMetadata() {
        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());
        if (values.isEmpty()) {
            LOG.error("No files selected");
            // TODO display an error
            return;
        }

        if (values.size() > 1) {
            LOG.error("Only a single object can be selected to view metadata ");
            // TODO show error
            return;
        }

        final Task<Ds3Metadata> getMetadata = new Task<Ds3Metadata>() {
            @Override
            protected Ds3Metadata call() throws Exception {
                final Ds3Client client = session.getClient();

                final Ds3TreeTableValue value = values.get(0).getValue();
                final HeadObjectResponse headObjectResponse = client.headObject(new HeadObjectRequest(value.getBucketName(), value.getFullName()));

                return new Ds3Metadata(headObjectResponse.getMetadata(), headObjectResponse.getObjectSize(), value.getFullName());
            }
        };
        workers.execute(getMetadata);
        getMetadata.setOnSucceeded(event -> Platform.runLater( () -> {
            LOG.info("Launching metadata popup");
            MetadataPopup.show(getMetadata.getValue());
        }));
    }

    public void deletePrompt() {
        LOG.info("Got delete event");

        final ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3TreeTable.getSelectionModel().getSelectedItems().stream().collect(GuavaCollectors.immutableList());
        if (values.isEmpty()) {
            LOG.error("No files selected");
            // TODO display an error
            return;
        }
        final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());

        if (buckets.size() > 1) {
            LOG.error("The user selected objects from multiple buckets.  This is not allowed.");
            // TODO show error
            return;
        }

        final ArrayList<Ds3TreeTableValue> filesToDelete = new ArrayList<>(values
                .stream()
                .map(TreeItem::getValue)
                .collect(Collectors.toList())
        );
        DeleteFilesPopup.show(session, buckets.get(0), filesToDelete);
        values.stream().forEach(file -> refresh(file.getParent()));
    }

    private void refresh(final TreeItem<Ds3TreeTableValue> modifiedTreeItem) {
        LOG.info("Running refresh of row");
        if (modifiedTreeItem instanceof Ds3TreeTableItem) {
            LOG.info("Refresh row");
            final Ds3TreeTableItem ds3TreeTableItem = (Ds3TreeTableItem) modifiedTreeItem;
            ds3TreeTableItem.refresh();
        }
    }

    private class GetServiceTask extends Task<ObservableList<TreeItem<Ds3TreeTableValue>>> {

        private final ReadOnlyObjectWrapper<ObservableList<TreeItem<Ds3TreeTableValue>>> partialResults;

        public GetServiceTask(final ObservableList<TreeItem<Ds3TreeTableValue>> observableList) {

            partialResults = new ReadOnlyObjectWrapper<>(this, "partialResults", observableList);
        }

        public ObservableList<TreeItem<Ds3TreeTableValue>> getPartialResults() {
            return this.partialResults.get();
        }

        public ReadOnlyObjectProperty<ObservableList<TreeItem<Ds3TreeTableValue>>> partialResultsProperty() {
            return partialResults.getReadOnlyProperty();
        }

        @Override
        protected ObservableList<TreeItem<Ds3TreeTableValue>> call() throws Exception {
            final GetServiceResponse response = session.getClient().getService(new GetServiceRequest());

            final ImmutableList<Ds3TreeTableValue> buckets = response.getListAllMyBucketsResult()
                    .getBuckets().stream()
                    .map(bucket -> new Ds3TreeTableValue(bucket.getName(), bucket.getName(), Ds3TreeTableValue.Type.BUCKET, 0, bucket.getCreationDate().toString()))
                    .collect(GuavaCollectors.immutableList());

            Platform.runLater(() -> {
                final ImmutableList<Ds3TreeTableItem> treeItems = buckets.stream().map(value -> new Ds3TreeTableItem(value.getName(), session, value, workers)).collect(GuavaCollectors.immutableList());
                partialResults.get().addAll(treeItems);
            });

            return this.partialResults.get();
        }
    }
}
