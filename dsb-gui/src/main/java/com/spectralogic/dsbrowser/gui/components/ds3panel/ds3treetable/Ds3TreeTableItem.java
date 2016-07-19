package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.models.common.CommonPrefixes;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;

import java.util.List;
import java.util.stream.Collectors;


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
        final GetBucketTask getBucketTask = new GetBucketTask(observableList);
        workers.execute(getBucketTask);
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

            String nextMarker = null;

            while (true) {

                final GetBucketRequest request = new GetBucketRequest(bucket).withDelimiter("/");

                // Don't include the prefix if the item we are looking up from is the base bucket
                if (ds3Value.getType() != Ds3TreeTableValue.Type.Bucket) {
                    request.withPrefix(ds3Value.getFullName());
                }

                if (nextMarker != null) {
                    request.withMarker(nextMarker);
                }

                final GetBucketResponse bucketResponse = session.getClient().getBucket(request);

                final ImmutableList<Ds3TreeTableValue> directoryValues = bucketResponse.getListBucketResult()
                        .getCommonPrefixes().stream().map(CommonPrefixes::getPrefix)
                        .map(c -> new Ds3TreeTableValue(bucket, c, Ds3TreeTableValue.Type.Directory, FileSizeFormat.getFileSizeType(0), "--"))
                        .collect(GuavaCollectors.immutableList());


                final List<Ds3TreeTableValue> files = bucketResponse.getListBucketResult()
                        .getObjects().stream()
                        .map(f -> new Ds3TreeTableValue(bucket, f.getKey(), Ds3TreeTableValue.Type.File, FileSizeFormat.getFileSizeType(f.getSize()), DateFormat.formatDate(f.getLastModified()))).collect(Collectors.toList());

                final ImmutableList<Ds3TreeTableValue> filteredFiles = files.stream().filter(i -> !i.getName().equals("")).collect(GuavaCollectors.immutableList());

                Platform.runLater(() -> {
                    final ImmutableList<Ds3TreeTableItem> directoryItems = directoryValues.stream().map(item -> new Ds3TreeTableItem(bucket, session, item, workers)).collect(GuavaCollectors.immutableList());
                    final ImmutableList<Ds3TreeTableItem> fileItems = filteredFiles.stream().map(item -> new Ds3TreeTableItem(bucket, session, item, workers)).collect(GuavaCollectors.immutableList());
                    partialResults.get().addAll(directoryItems);
                    partialResults.get().addAll(fileItems);
                });

                nextMarker = bucketResponse.getListBucketResult().getNextMarker();
                if (Guard.isStringNullOrEmpty(nextMarker)) {
                    break;
                }
            }
            return partialResults.get();
        }
    }
}
