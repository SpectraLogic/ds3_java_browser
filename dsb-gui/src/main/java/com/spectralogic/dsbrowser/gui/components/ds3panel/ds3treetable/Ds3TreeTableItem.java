package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.spectrads3.GetObjectsWithFullDetailsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetObjectsWithFullDetailsSpectraS3Response;
import com.spectralogic.ds3client.models.BulkObject;
import com.spectralogic.ds3client.models.DetailedS3Object;
import com.spectralogic.ds3client.models.S3ObjectType;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

import static com.spectralogic.dsbrowser.gui.util.GetStorageLocations.addPlacementIconsandTooltip;


public class Ds3TreeTableItem extends TreeItem<Ds3TreeTableValue> {

    private final String bucket;
    private final Session session;
    private final Ds3TreeTableValue ds3Value;
    private final boolean leaf;
    private final Workers workers;
    private boolean accessedChildren = false;

    public Ds3TreeTableItem(final String bucket, final Session session, final Ds3TreeTableValue value, final Workers workers) {
        super(value);
        this.bucket = bucket;
        this.session = session;
        this.ds3Value = value;
        this.leaf = isLeaf(value);
        this.workers = workers;
        this.setGraphic(getIcon(value.getType())); // sets the default icon
    }

    public boolean isAccessedChildren() {
        return accessedChildren;
    }

    public void setAccessedChildren(boolean accessedChildren) {
        this.accessedChildren = accessedChildren;
    }

    private static Node getIcon(final Ds3TreeTableValue.Type type) {
        switch (type) {
            case Bucket:
                return (new ImageView("/images/bucket.png"));
            case Directory:
                return (new ImageView("/images/folder.png"));
            case File:
                return (new ImageView("/images/file.png"));
            default:
                return null;
        }
    }

    private static boolean isLeaf(final Ds3TreeTableValue value) {
        return value.getType() == Ds3TreeTableValue.Type.File;
    }

    public void refresh() {
        final ObservableList<TreeItem<Ds3TreeTableValue>> list = super.getChildren();
        list.remove(0, list.size());
        buildChildren(list);
    }

    @Override
    public ObservableList<TreeItem<Ds3TreeTableValue>> getChildren() {
        if (!accessedChildren) {
            buildChildren(super.getChildren());
            accessedChildren = true;
        }
        return super.getChildren();
    }

    // query black pearl in the background and then update the main thread after
    private void buildChildren(final ObservableList<TreeItem<Ds3TreeTableValue>> observableList) {
        final Node previousGraphics = super.getGraphic();
        final ImageView processImage = new ImageView("/images/loading.gif");
        processImage.setFitHeight(20);
        processImage.setFitWidth(20);
        super.setGraphic(processImage);

        final GetBucketTask getBucketTask = new GetBucketTask(observableList);
        workers.execute(getBucketTask);
        getBucketTask.setOnSucceeded(event -> super.setGraphic(previousGraphics));
        getBucketTask.setOnCancelled(event -> super.setGraphic(previousGraphics));
        getBucketTask.setOnFailed(event -> super.setGraphic(previousGraphics));
    }

    @Override
    public boolean isLeaf() {
        return leaf;
    }

    private class GetBucketTask extends Task<ObservableList<TreeItem<Ds3TreeTableValue>>> {

        private final ReadOnlyObjectWrapper<ObservableList<TreeItem<Ds3TreeTableValue>>> partialResults;

        public GetBucketTask(final ObservableList<TreeItem<Ds3TreeTableValue>> observableList) {
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

            final GetObjectsWithFullDetailsSpectraS3Request request = new GetObjectsWithFullDetailsSpectraS3Request().withBucketId(bucket).withIncludePhysicalPlacement(true);

            if (ds3Value.getType() == Ds3TreeTableValue.Type.Bucket) {
                request.withName("%").withFolder("/");
            } else {
                request.withName(ds3Value.getFullName() + "%").withFolder(ds3Value.getFullName());
            }

            final GetObjectsWithFullDetailsSpectraS3Response objectsWithFullDetailsSpectraS3 = session.getClient().getObjectsWithFullDetailsSpectraS3(request);

            final List<DetailedS3Object> detailedS3Objects = objectsWithFullDetailsSpectraS3.getDetailedS3ObjectListResult().getDetailedS3Objects();

            final List<Ds3TreeTableValue> files = new ArrayList<>();

            final ImmutableList<Ds3TreeTableValue> directoryValues = detailedS3Objects.stream().map(i -> {

                if (ds3Value.getType() != Ds3TreeTableValue.Type.Bucket) {
                    try {
                        if (i.getName().equals(ds3Value.getFullName()))
                            return null;

                        if (i.getType().equals(S3ObjectType.FOLDER)) {
                            return i.getName();
                        } else {
                            final List<BulkObject> objects = i.getBlobs().getObjects();
                            if (objects.stream().findFirst().isPresent()) {
                                final HBox iconsAndTooltip = addPlacementIconsandTooltip(objects.stream().findFirst().orElse(null));
                                files.add(new Ds3TreeTableValue(bucket, i.getName(), Ds3TreeTableValue.Type.File, i.getSize(), DateFormat.formatDate(i.getCreationDate()), i.getOwner(), false, iconsAndTooltip));
                            }
                            return null;
                        }
                    } catch (final Exception e) {
                        return null;
                    }

                } else {
                    final String splitReg = "/";
                    if (i.getName().contains("/")) {
                        return i.getName().split(splitReg)[0] + "/";
                    } else {
                        final List<BulkObject> objects = i.getBlobs().getObjects();
                        if (objects.stream().findFirst().isPresent()) {
                            final HBox iconsAndTooltip = addPlacementIconsandTooltip(objects.stream().findFirst().orElse(null));
                            files.add(new Ds3TreeTableValue(bucket, i.getName(), Ds3TreeTableValue.Type.File, i.getSize(), DateFormat.formatDate(i.getCreationDate()), i.getOwner(), false, iconsAndTooltip));
                        }
                        return null;
                    }
                }
            }).distinct().filter(i -> i != null).map(c -> {
                final HBox hbox = new HBox();
                hbox.getChildren().add(new Label("----"));
                hbox.setAlignment(Pos.CENTER);
                return new Ds3TreeTableValue(bucket, c, Ds3TreeTableValue.Type.Directory, 0, "--", "--", false, hbox);
            }).collect(GuavaCollectors.immutableList());
            final ImmutableList<Ds3TreeTableValue> filteredFiles = files.stream().filter(i -> !i.getName().equals("")).collect(GuavaCollectors.immutableList());

            Platform.runLater(() -> {
                final ImmutableList<Ds3TreeTableItem> directoryItems = directoryValues.stream().map(item -> new Ds3TreeTableItem(bucket, session, item, workers)).collect(GuavaCollectors.immutableList());
                final ImmutableList<Ds3TreeTableItem> fileItems = filteredFiles.stream().map(item -> new Ds3TreeTableItem(bucket, session, item, workers)).collect(GuavaCollectors.immutableList());
                partialResults.get().addAll(directoryItems);
                partialResults.get().addAll(fileItems);
            });

            return partialResults.get();
        }
    }
}
