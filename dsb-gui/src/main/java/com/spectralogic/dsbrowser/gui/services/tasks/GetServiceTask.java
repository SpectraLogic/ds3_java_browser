package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.GetServiceRequest;
import com.spectralogic.ds3client.commands.GetServiceResponse;
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
import javafx.scene.layout.HBox;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class GetServiceTask extends Task<ObservableList<TreeItem<Ds3TreeTableValue>>> {


    private final ReadOnlyObjectWrapper<ObservableList<TreeItem<Ds3TreeTableValue>>> partialResults;
    private final Session session;
    private final Workers workers;
    private final Ds3Common ds3Common;

    public GetServiceTask(final ObservableList<TreeItem<Ds3TreeTableValue>> observableList, final Session session,
                          final Workers workers, final Ds3Common ds3Common) {
        partialResults = new ReadOnlyObjectWrapper<>(this, "partialResults", observableList);
        this.session = session;
        this.workers = workers;
        this.ds3Common = ds3Common;
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
        if (response.getListAllMyBucketsResult().getBuckets() != null) {
            final List<Ds3TreeTableValue> buckets = response.getListAllMyBucketsResult()
                    .getBuckets().stream()
                    .map(bucket -> {
                        final HBox hbox = new HBox();
                        hbox.getChildren().add(new Label("----"));
                        hbox.setAlignment(Pos.CENTER);
                        return new Ds3TreeTableValue(bucket.getName(), bucket.getName(), Ds3TreeTableValue.Type.Bucket, 0, DateFormat.formatDate(bucket.getCreationDate()), "--", false, hbox);
                    })
                    .collect(Collectors.toList());
            buckets.sort(Comparator.comparing(t -> t.getName().toLowerCase()));
            Platform.runLater(() -> {
                if (ds3Common.getDeepStorageBrowserPresenter() != null)
                    ds3Common.getDeepStorageBrowserPresenter().logText("Received bucket list", LogType.SUCCESS);
                ds3Common.getDs3PanelPresenter().disableSearch(false);
                final ImmutableList<Ds3TreeTableItem> treeItems = buckets.stream().map(value -> new
                        Ds3TreeTableItem(value.getName(), session, value, workers, ds3Common)).collect(GuavaCollectors
                        .immutableList());
                partialResults.get().addAll(treeItems);
            });
        } else {
            ds3Common.getDs3PanelPresenter().disableSearch(true);
        }
        return this.partialResults.get();
    }
}