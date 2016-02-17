package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.services.Session;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;


public class Ds3TreeTableItem extends TreeItem<Ds3TreeTableValue> {

    private final String bucket;
    private final Session session;
    private final Ds3TreeTableValue value;
    private final boolean leaf;
    private final Workers workers;

    private boolean accessedChildren = false;

    public Ds3TreeTableItem(final String bucket, final Session session, final Ds3TreeTableValue value, final Workers workers) {
        super(value);
        this.bucket = bucket;
        this.session = session;
        this.value = value;
        this.leaf = isLeaf(value);
        this.workers = workers;
        this.setGraphic(getIcon(value.getType())); // sets the default icon
    }

    private static Node getIcon(final Ds3TreeTableValue.Type type) {
        switch(type) {
            case BUCKET: return Icon.getIcon(FontAwesomeIcon.ARCHIVE);
            case DIRECTORY: return Icon.getIcon(FontAwesomeIcon.FOLDER);
            default: return null;
        }
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
        if (isLeaf()) {
            return;
        }
        final GetBucketTask getBucketTask = new GetBucketTask(observableList);

        workers.execute(getBucketTask);

    }

    @Override
    public boolean isLeaf() {
        return leaf;
    }

    private static boolean isLeaf(final Ds3TreeTableValue value) {
        return value.getType() == Ds3TreeTableValue.Type.FILE;
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
                if (value.getType() != Ds3TreeTableValue.Type.BUCKET) {
                   request.withPrefix(value.getFullName());
                }

                if (nextMarker != null) {
                    request.withMarker(nextMarker);
                }

                final GetBucketResponse bucketResponse = session.getClient().getBucket(request);

                final ImmutableList<Ds3TreeTableValue> directoryValues = bucketResponse.getListBucketResult()
                        .getCommonPrefixes().stream()
                        .map(c -> new Ds3TreeTableValue(c, Ds3TreeTableValue.Type.DIRECTORY, 0, ""))
                        .collect(GuavaCollectors.immutableList());

                final ImmutableList<Ds3TreeTableValue> files = bucketResponse.getListBucketResult()
                        .getObjects().stream()
                        .map(f -> new Ds3TreeTableValue(f.getKey(), Ds3TreeTableValue.Type.FILE, f.getSize(), f.getLastModified().toString()))
                        .collect(GuavaCollectors.immutableList());

                Platform.runLater(() -> directoryValues.forEach(item -> partialResults.get().add(new Ds3TreeTableItem(bucket, session, item, workers))));
                Platform.runLater(() -> files.forEach(item -> partialResults.get().add(new Ds3TreeTableItem(bucket, session, item, workers))));

                nextMarker = bucketResponse.getListBucketResult().getNextMarker();
                if (Guard.isStringNullOrEmpty(nextMarker)) {
                    break;
                }
            }
            return partialResults.get();
        }
    }
}
