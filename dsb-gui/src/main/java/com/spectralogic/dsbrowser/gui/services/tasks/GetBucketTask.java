package com.spectralogic.dsbrowser.gui.services.tasks;


import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.spectralogic.dsbrowser.gui.util.BucketUtil.distinctByKey;

public class GetBucketTask extends Task<ObservableList<TreeItem<Ds3TreeTableValue>>> {
    private final static Logger LOG = LoggerFactory.getLogger(GetBucketTask.class);

    private final ReadOnlyObjectWrapper<ObservableList<TreeItem<Ds3TreeTableValue>>> partialResults;
    private final String bucket;
    private final Session session;
    private final Ds3TreeTableValue ds3Value;
    private final boolean leaf;
    private final Workers workers;
    private final TreeTableView ds3TreeTable;
    private final Ds3Common ds3Common;
    private final int PAGE_LENGTH = 10;
    private final ResourceBundle resourceBundle;
    private final Ds3TreeTableItem ds3TreeTableItem;

    public GetBucketTask(final ObservableList<TreeItem<Ds3TreeTableValue>> observableList, final String bucket, final
    Session session, final Ds3TreeTableValue ds3Value, final boolean leaf, final Workers workers, final
                         Ds3TreeTableItem ds3TreeTableItem, final TreeTableView ds3TreeTable,
                         final Ds3Common ds3Common) {
        this.partialResults = new ReadOnlyObjectWrapper<>(this, "partialResults", observableList);
        resourceBundle = ResourceBundleProperties.getResourceBundle();
        this.bucket = bucket;
        this.session = session;
        this.ds3Value = ds3Value;
        this.leaf = leaf;
        this.workers = workers;
        this.ds3TreeTableItem = ds3TreeTableItem;
        this.ds3TreeTable = ds3TreeTable;
        this.ds3Common = ds3Common;

    }

    public ObservableList<TreeItem<Ds3TreeTableValue>> getPartialResults() {
        return this.partialResults.get();
    }

    public ReadOnlyObjectProperty<ObservableList<TreeItem<Ds3TreeTableValue>>> partialResultsProperty() {
        return partialResults.getReadOnlyProperty();
    }


    @Override
    protected ObservableList<TreeItem<Ds3TreeTableValue>> call() {
        try {
            final GetBucketRequest request = BucketUtil.createRequest(ds3Value, bucket, ds3TreeTableItem, PAGE_LENGTH);
            //if marker is set blank for a item that means offset is 0 else set the marker
            final GetBucketResponse bucketResponse = session.getClient().getBucket(request);
            //marker for the next request
            final String marker = bucketResponse.getListBucketResult().getNextMarker();
            //use to store list of files
            final List<Ds3TreeTableValue> filteredFiles = new ArrayList<>();
            //get list of objects with condition key should not be null and key name and prefix should not be same
            final List<Ds3Object> ds3ObjectListFiles = bucketResponse.getListBucketResult()
                    .getObjects()
                    .stream()
                    .filter(c -> ((c.getKey() != null) && (!c.getKey().equals(ds3Value.getFullName()))))
                    .map(i -> new Ds3Object(i.getKey(), i.getSize()))
                    .collect(Collectors.toList());
            if (!Guard.isNullOrEmpty(ds3ObjectListFiles)) {
                filteredFiles.addAll(BucketUtil.getFilterFilesList(ds3ObjectListFiles, bucketResponse, bucket, session));
            }
            //directoryValues is used to store directories
            final List<Ds3TreeTableValue> directoryValues = BucketUtil.getDirectoryValues(bucketResponse, bucket);
            //after getting both lists we need to merge in partialResult and need to sort
            Platform.runLater(() -> {
                final ImmutableList<Ds3TreeTableItem> directoryItems = directoryValues
                        .stream()
                        .map(item -> new Ds3TreeTableItem(bucket, session, item, workers, ds3Common))
                        .collect(GuavaCollectors.immutableList());
                final ImmutableList<Ds3TreeTableItem> fileItems = filteredFiles
                        .stream()
                        .map(item -> new Ds3TreeTableItem(bucket, session, item, workers, ds3Common))
                        .filter(distinctByKey(p -> p.getValue().getFullName()))
                        .collect(GuavaCollectors.immutableList());
                //if selected item is not load more then clear partial result list so that items will not appear twice
                if(ds3Value.getType() != Ds3TreeTableValue.Type.Loader) {
                    partialResults.get().clear();
                }

                partialResults.get().addAll(directoryItems);
                partialResults.get().addAll(fileItems);
                Collections.sort(partialResults.get(), new PartialResultComparator());
                ds3Common.getExpandedNodesInfo().put(session.getSessionName() + StringConstants.SESSION_SEPARATOR
                        + session.getEndpoint(), ds3TreeTableItem);
                //if selected item was button then just remove that click more button and add new one
                if (ds3Value.getType() == Ds3TreeTableValue.Type.Loader) {
                    //clear the selection
                    if (null != ds3TreeTable && null != ds3TreeTable.getSelectionModel()
                            && null != ds3TreeTableItem.getParent()) {
                        ds3TreeTable.getSelectionModel().clearSelection();
                        ds3TreeTable.getSelectionModel().select(ds3TreeTableItem.getParent());
                        ds3Common.getExpandedNodesInfo().put(session.getSessionName() + StringConstants.SESSION_SEPARATOR
                                + session.getEndpoint(), ds3TreeTableItem.getParent());
                        ds3Common.getDeepStorageBrowserPresenter().logText(
                                (partialResults.get().size() - 1) + StringConstants.SPACE + resourceBundle.getString
                                        ("filesAndFolders") + StringConstants.SPACE +
                                        ds3TreeTableItem.getParent().getValue().getType().toString() + StringConstants.SPACE
                                        + ds3TreeTableItem.getParent().getValue().getFullName(), LogType.SUCCESS);
                    }
                    partialResults.get().remove(ds3TreeTableItem);
                }
                if (!Guard.isStringNullOrEmpty(marker)) {
                    //add a new click to add more button
                    final HBox hbox = new HBox();
                    hbox.getChildren().add(new Label(StringConstants.EMPTY_STRING));
                    hbox.setAlignment(Pos.CENTER);
                    final Text clickToLoadMore = new Text(resourceBundle.getString("addMoreButton"));
                    clickToLoadMore.setFont(Font.font("Verdana", FontWeight.BOLD, 70));
                    final Ds3TreeTableItem addMoreItem = new Ds3TreeTableItem(bucket, session, new Ds3TreeTableValue
                            (bucket, clickToLoadMore.getText(), Ds3TreeTableValue.Type.Loader, -1,
                                    StringConstants.EMPTY_STRING, StringConstants.EMPTY_STRING, false,
                                    hbox, marker), workers, ds3Common);
                    partialResults.get().add(addMoreItem);

                }
            });
        } catch (final Exception e) {
            LOG.error("Encountered an error trying to get the next list of items", e);
        }
        return partialResults.get();
    }


}
