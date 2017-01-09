package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.spectralogic.ds3client.commands.spectrads3.GetObjectsWithFullDetailsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetObjectsWithFullDetailsSpectraS3Response;
import com.spectralogic.ds3client.models.BucketDetails;
import com.spectralogic.ds3client.models.BulkObject;
import com.spectralogic.ds3client.models.DetailedS3Object;
import com.spectralogic.ds3client.models.S3ObjectType;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.GetStorageLocations;
import com.spectralogic.dsbrowser.gui.util.LogType;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SearchJob extends Task<String> {
    private final List<BucketDetails> buckets;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final TreeTableView<Ds3TreeTableValue> ds3TreeTableView;
    private final Label ds3PathIndicator;
    private final String seachText;
    private final Session session;
    private final Workers workers;

    public SearchJob(final List<BucketDetails> buckets, final DeepStorageBrowserPresenter deepStorageBrowserPresenter, final TreeTableView<Ds3TreeTableValue> ds3TreeTableView, final Label ds3PathIndicator, final String seachText, final Session session, final Workers workers) {
        this.buckets = buckets;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.ds3TreeTableView = ds3TreeTableView;
        this.ds3PathIndicator = ds3PathIndicator;
        this.seachText = seachText;
        this.session = session;
        this.workers = workers;
    }

    @Override
    protected String call() throws Exception {
        try {
            final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
            rootTreeItem.setExpanded(true);
            ds3TreeTableView.setShowRoot(false);
            final List<Ds3TreeTableItem> list = new ArrayList<>();

            for (final BucketDetails bucket : buckets) {
                if (bucket.getName().contains(seachText)) {
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Found bucket with name " + seachText,
                            LogType.INFO));
                    final Ds3TreeTableValue value = new Ds3TreeTableValue(bucket.getName(), bucket.getName(), Ds3TreeTableValue.Type.Bucket, 0, "--", "--", false, null);
                    list.add(new Ds3TreeTableItem(value.getName(), session, value, workers));
                } else {
                    final GetObjectsWithFullDetailsSpectraS3Request request = new GetObjectsWithFullDetailsSpectraS3Request().withBucketId(bucket.getName()).withName("%" + seachText + "%").withIncludePhysicalPlacement(true);
                    final GetObjectsWithFullDetailsSpectraS3Response responseFullDetails = session.getClient().getObjectsWithFullDetailsSpectraS3(request);
                    final List<DetailedS3Object> detailedS3Objects = responseFullDetails.getDetailedS3ObjectListResult().getDetailedS3Objects();
                    final List<Ds3TreeTableValue> treeItems = new ArrayList<>();
                    detailedS3Objects.stream()
                            .forEach(f -> {
                                        if (!f.getType().equals(S3ObjectType.FOLDER)) {
                                            if (f.getBlobs() != null) {
                                                final List<BulkObject> objects = f.getBlobs().getObjects();
                                                if (objects != null) {
                                                    if (objects.stream().findFirst().isPresent()) {
                                                        treeItems.add(new Ds3TreeTableValue(bucket.getName(), f.getName(), Ds3TreeTableValue.Type.File, f.getSize(), DateFormat.formatDate(f.getCreationDate()), f.getOwner(), true, GetStorageLocations.addPlacementIconsandTooltip(objects.stream().findFirst().orElse(null))));
                                                    }
                                                } else {
                                                    treeItems.add(new Ds3TreeTableValue(bucket.getName(), f.getName(), Ds3TreeTableValue.Type.File, f.getSize(), DateFormat.formatDate(f.getCreationDate()), f.getOwner(), true, null));
                                                }
                                            } else {
                                                treeItems.add(new Ds3TreeTableValue(bucket.getName(), f.getName(), Ds3TreeTableValue.Type.File, f.getSize(), DateFormat.formatDate(f.getCreationDate()), f.getOwner(), true, null));
                                            }
                                        }
                                    }
                            );
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Searched in " + bucket.getName() + ": found " + treeItems.size() + " item(s)",
                            LogType.INFO));
                    treeItems.stream().forEach(item -> list.add(new Ds3TreeTableItem(item.getFullName(), session, item, workers)));
                }
            }

            Platform.runLater(() -> {
                ds3PathIndicator.setText("Search result(s): " + list.size() + " object(s) found");
                deepStorageBrowserPresenter.logText("Search result(s): " + list.size() + " object(s) found", LogType.INFO);
                list.sort(Comparator.comparing(t -> t.getValue().getType().toString()));
                list.stream().forEach(value -> rootTreeItem.getChildren().add(value)
                );
                if (rootTreeItem.getChildren().size() == 0) {
                    ds3TreeTableView.setPlaceholder(new Label("Search result(s): 0 object(s) found"));
                }
                ds3TreeTableView.setRoot(rootTreeItem);
                final TreeTableColumn<Ds3TreeTableValue, ?> ds3TreeTableValueTreeTableColumn = ds3TreeTableView.getColumns().get(1);
                ds3TreeTableValueTreeTableColumn.setVisible(true);
            });
        } catch (final Exception e) {
            Platform.runLater(() -> deepStorageBrowserPresenter.logText("Search failed: " + e.toString(), LogType.INFO));
        }
        return null;
    }
}