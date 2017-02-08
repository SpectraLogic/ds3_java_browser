package com.spectralogic.dsbrowser.gui.services.tasks;


import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Response;
import com.spectralogic.ds3client.models.Contents;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.LogType;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.spectralogic.dsbrowser.gui.util.GetStorageLocations.addPlacementIconsandTooltip;

public class GetBucketTask extends Task<ObservableList<TreeItem<Ds3TreeTableValue>>> {
    private static final Logger LOG = LoggerFactory.getLogger(GetBucketTask.class);

    private final ReadOnlyObjectWrapper<ObservableList<TreeItem<Ds3TreeTableValue>>> partialResults;
    private final String bucket;
    private final Session session;
    private final Ds3TreeTableValue ds3Value;
    private final boolean leaf;
    private final Workers workers;
    private final TreeTableView ds3TreeTable;
    private final Ds3Common ds3Common;
    private static final int PAGE_LENGTH = 1000;
    private final ResourceBundle myResources =
            ResourceBundle.getBundle("lang", new Locale("en_IN"));
    private final Ds3TreeTableItem ds3TreeTableItem;

    public GetBucketTask(final ObservableList<TreeItem<Ds3TreeTableValue>> observableList, final String bucket, final
    Session session, final Ds3TreeTableValue ds3Value, final boolean leaf, final Workers workers, final
                         Ds3TreeTableItem ds3TreeTableItem, final TreeTableView ds3TreeTable,
                         final Ds3Common ds3Common) {
        partialResults = new ReadOnlyObjectWrapper<>(this, "partialResults", observableList);
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
            final GetBucketRequest request;
            //if marker is set blank for a item that means offset is 0 else set the marker
            if (ds3Value.getMarker().equals("")) {
                request = new GetBucketRequest(bucket).withDelimiter("/").withMaxKeys(PAGE_LENGTH);
            } else {
                request = new GetBucketRequest(bucket).withDelimiter("/").withMaxKeys(PAGE_LENGTH).withMarker(ds3Value.getMarker());
            }
            if (ds3Value.getType() == Ds3TreeTableValue.Type.Bucket) {
            } else if (ds3Value.getType() == Ds3TreeTableValue.Type.Loader) {
                if (ds3TreeTableItem.getParent().getValue().getType() == Ds3TreeTableValue.Type.Bucket) {
                } else {
                    final Ds3TreeTableValue ds3ParentValue = ds3TreeTableItem.getParent().getValue();
                    request.withPrefix(ds3ParentValue.getFullName());
                }
            } else {
                request.withPrefix(ds3Value.getFullName());
            }
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
            if (ds3ObjectListFiles.size() != 0) {
                final GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Request requestPlacement = new GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Request(bucket, ds3ObjectListFiles);
                final GetPhysicalPlacementForObjectsWithFullDetailsSpectraS3Response responsePlacement = session.getClient().getPhysicalPlacementForObjectsWithFullDetailsSpectraS3(requestPlacement);
                final List<Ds3TreeTableValue> filteredFileslist = responsePlacement
                        .getBulkObjectListResult()
                        .getObjects()
                        .stream()
                        .map(i -> {
                            final Contents content = bucketResponse.getListBucketResult()
                                    .getObjects()
                                    .stream()
                                    .filter(j -> j.getKey().equals(i.getName()))
                                    .findFirst()
                                    .get();
                            final HBox iconsAndTooltip = addPlacementIconsandTooltip(i.getPhysicalPlacement(), i.getInCache());
                            return new Ds3TreeTableValue(bucket, i.getName(), Ds3TreeTableValue.Type.File, content.getSize(), DateFormat.formatDate(content.getLastModified()), content.getOwner().getDisplayName(), false, iconsAndTooltip);
                        }).collect(Collectors.toList());
                filteredFiles.addAll(filteredFileslist);
            }
            //directoryValues is used to store directories
            final List<Ds3TreeTableValue> directoryValues = bucketResponse.getListBucketResult().getCommonPrefixes().stream().map(i ->
            {
                String folderName = i.getPrefix();
                final HBox hbox = new HBox();
                hbox.getChildren().add(new Label("----"));
                hbox.setAlignment(Pos.CENTER);
                return new Ds3TreeTableValue(bucket, folderName, Ds3TreeTableValue.Type.Directory, 0, "--", "--", false, hbox);

            }).collect(Collectors.toList());
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
                partialResults.get().clear();
                partialResults.get().addAll(directoryItems);
                partialResults.get().addAll(fileItems);
                Collections.sort(partialResults.get(), new PartialResultComparator());
                //if selected item was button then just remove that click more button and add new one
                if (ds3Value.getType() == Ds3TreeTableValue.Type.Loader) {
                    //clear the selection
                    if (ds3TreeTable != null) {
                        ds3TreeTable.getSelectionModel().clearSelection();
                        ds3TreeTable.getSelectionModel().select(ds3TreeTableItem.getParent());
                        ds3Common.getExpandedNodesInfo().put(session.getSessionName() + "-" + session.getEndpoint(), ds3TreeTableItem.getParent());
                        ds3Common.getDeepStorageBrowserPresenter().logText((partialResults.get().size() - 1) + " files/folders have been loaded " +
                                "inside " +
                                ds3TreeTableItem.getParent().getValue().getType().toString() + " " + ds3TreeTableItem.getParent().getValue().getFullName(), LogType.SUCCESS);
                    }
                    partialResults.get().remove(ds3TreeTableItem);
                }
                if (marker != null) {
                    //add a new click to add more button
                    final HBox hbox = new HBox();
                    hbox.getChildren().add(new Label(""));
                    hbox.setAlignment(Pos.CENTER);
                    final Text clickToLoadMore = new Text(myResources.getString("addMoreButton"));
                    clickToLoadMore.setFont(Font.font("Verdana", FontWeight.BOLD, 70));
                    final Ds3TreeTableItem addMoreItem = new Ds3TreeTableItem(bucket, session, new Ds3TreeTableValue
                            (bucket, clickToLoadMore.getText(), Ds3TreeTableValue.Type.Loader, -1, "", "", false,
                                    hbox, marker), workers, ds3Common);
                    partialResults.get().add(addMoreItem);

                }
            });
        } catch (final Throwable t) {
            LOG.error("Encountered an error trying to get the next list of items", t);
        }
        return partialResults.get();
    }


    //function for distinction on the basis of some property
    private static <T> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor) {
        final Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }


    private class PartialResultComparator implements Comparator<TreeItem<Ds3TreeTableValue>> {

        @Override
        public int compare(final TreeItem<Ds3TreeTableValue> o1, final TreeItem<Ds3TreeTableValue> o2) {
            final String type1 = o1.getValue().getType().toString();
            final String type2 = o2.getValue().getType().toString();
            if (type1.equals(Ds3TreeTableValue.Type.Directory.toString()) && !type2.equals(Ds3TreeTableValue.Type.Directory.toString())) {
                // Directory before non-directory
                return -1;
            } else if (!type1.equals(Ds3TreeTableValue.Type.Directory.toString()) && type2.equals(Ds3TreeTableValue.Type.Directory.toString())) {
                // Non-directory after directory
                return 1;
            } else {
                // Alphabetic order otherwise
                return o1.getValue().getFullName().compareTo(o2.getValue().getFullName());
            }
        }
    }
}



