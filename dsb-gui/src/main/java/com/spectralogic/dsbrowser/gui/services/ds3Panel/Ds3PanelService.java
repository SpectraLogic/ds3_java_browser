/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
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

package com.spectralogic.dsbrowser.gui.services.ds3Panel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.spectrads3.GetBucketsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetBucketsSpectraS3Response;
import com.spectralogic.ds3client.models.Bucket;
import com.spectralogic.ds3client.models.ListBucketResult;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.metadata.MetadataView;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.PhysicalPlacementPopup;
import com.spectralogic.dsbrowser.gui.components.version.VersionPopup;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.MetadataTask;
import com.spectralogic.dsbrowser.gui.services.tasks.PhysicalPlacementTask;
import com.spectralogic.dsbrowser.gui.services.tasks.SearchJobTask;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public final class Ds3PanelService {

    private static final Logger LOG = LoggerFactory.getLogger(Ds3PanelService.class);

    private final Ds3Common ds3Common;
    private final Workers workers;
    private final ResourceBundle resourceBundle;
    private final DateTimeUtils dateTimeUtils;
    private final LoggingService loggingService;
    private final RefreshCompleteViewWorker refreshCompleteViewWorker;
    private final VersionPopup versionPopup;
    private final Popup popup;
    private final PhysicalPlacementPopup physicalPlacementPopup;
    private final PhysicalPlacementTask.PhysicalPlacementTaskFactory physicalPlacementTaskFactory;

    @Inject
    public Ds3PanelService(
            final ResourceBundle resourceBundle,
            final DateTimeUtils dateTimeUtils,
            final Ds3Common ds3Common,
            final Workers workers,
            final LoggingService loggingService,
            final RefreshCompleteViewWorker refreshCompleteViewWorker,
            final VersionPopup versionPopup,
            final Popup popup,
            final PhysicalPlacementPopup physicalPlacementPopup,
            final PhysicalPlacementTask.PhysicalPlacementTaskFactory physicalPlacementTaskFactory
    ) {
        this.versionPopup = versionPopup;
        this.ds3Common = ds3Common;
        this.workers = workers;
        this.refreshCompleteViewWorker = refreshCompleteViewWorker;
        this.resourceBundle = resourceBundle;
        this.dateTimeUtils = dateTimeUtils;
        this.loggingService = loggingService;
        this.popup = popup;
        this.physicalPlacementPopup = physicalPlacementPopup;
        this.physicalPlacementTaskFactory = physicalPlacementTaskFactory;
    }

    /**
     * check if bucket contains or folders
     *
     * @param bucketName bucketName
     * @return true if bucket is empty else return false
     */
    public boolean checkIfBucketEmpty(final String bucketName, final Session session) {
        try {
            final GetBucketRequest request = new GetBucketRequest(bucketName).withMaxKeys(1);
            final GetBucketResponse bucketResponse = session.getClient().getBucket(request);
            final ListBucketResult listBucketResult = bucketResponse.getListBucketResult();
            return Guard.isNullOrEmpty(listBucketResult.getObjects()) && Guard.isNullOrEmpty(listBucketResult.getCommonPrefixes());

        } catch (final Exception e) {
            LOG.error("could not get bucket response", e);
            return false;
        }
    }

    public Optional<ImmutableList<Bucket>> setSearchableBucket(final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItem,
            final Session session,
            final TreeTableView<Ds3TreeTableValue> treeTableView) {
        try {
            if (null != treeTableView) {
                ObservableList<TreeItem<Ds3TreeTableValue>> selectedItemTemp = selectedItem;
                if (null == selectedItemTemp) {
                    selectedItemTemp = FXCollections.observableArrayList();
                    if (null != treeTableView.getRoot() && null != treeTableView.getRoot().getValue()) {
                        selectedItemTemp.add(treeTableView.getRoot());
                    }
                }
                final GetBucketsSpectraS3Request getBucketsSpectraS3Request = new GetBucketsSpectraS3Request();
                final GetBucketsSpectraS3Response response = session.getClient().getBucketsSpectraS3(getBucketsSpectraS3Request);
                final ImmutableList<Bucket> buckets = response.getBucketListResult().getBuckets().stream().collect(GuavaCollectors.immutableList());
                if (!Guard.isNullOrEmpty(selectedItemTemp)) {
                    final ImmutableSet<String> bucketNameSet = selectedItemTemp.stream().map(item -> item.getValue()
                            .getBucketName()).collect(GuavaCollectors.immutableSet());
                    return Optional.ofNullable(buckets.stream().filter(bucket -> bucketNameSet.contains(bucket.getName())).collect
                            (GuavaCollectors.immutableList()));
                } else {
                    return Optional.ofNullable(buckets);
                }
            } else {
                throw new NullPointerException("TreeTableView can't be null");
            }
        } catch (final Exception e) {
            LOG.error("Something went wrong!", e);
            return Optional.empty();
        }
    }

    public void refresh(final TreeItem<Ds3TreeTableValue> modifiedTreeItem) {
        LOG.info("Running refresh of row");
        if (modifiedTreeItem instanceof Ds3TreeTableItem) {
            final Ds3TreeTableItem item;
            if (modifiedTreeItem.getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
                item = (Ds3TreeTableItem) modifiedTreeItem.getParent();
            } else {
                item = (Ds3TreeTableItem) modifiedTreeItem;
            }
            if (item != null) {
                if (item.isExpanded()) {
                    item.refresh();
                } else if (item.isAccessedChildren()) {
                    item.setExpanded(true);
                    item.refresh();
                } else {
                    item.setExpanded(true);
                }
            }
        }
    }

    public void showPhysicalPlacement() {
        getSelectedItems()
                .stream()
                .findFirst()
                .ifPresent(ds3TreeTableValueTreeItem -> {
                    final PhysicalPlacementTask getPhysicalPlacement = physicalPlacementTaskFactory.create(ds3TreeTableValueTreeItem.getValue());
                    workers.execute(getPhysicalPlacement);
                    getPhysicalPlacement.setOnSucceeded(SafeHandler.logHandle(event -> Platform.runLater(() -> {
                        LOG.info("Launching PhysicalPlacement popup");
                        physicalPlacementPopup.show(getPhysicalPlacement.getValue());
                    })));
                });
    }

    public void showMetadata() {
        getSelectedItems()
                .stream()
                .findFirst()
                .ifPresent(ds3TreeTableValueTreeItem -> {
                    final MetadataTask getMetadata = new MetadataTask(ds3Common, ImmutableList.of(ds3TreeTableValueTreeItem));
                    workers.execute(getMetadata);
                    getMetadata.setOnSucceeded(SafeHandler.logHandle(event -> Platform.runLater(() -> {
                        LOG.info("Launching metadata popup");
                        final MetadataView metadataView = new MetadataView(getMetadata.getValue());
                        popup.show(metadataView.getView(), resourceBundle.getString("metaDataContextMenu"), true);
                    })));
                });
    }

    public void filterChanged() {
        final Ds3PanelPresenter ds3PanelPresenter = ds3Common.getDs3PanelPresenter();
        final String newValue = ds3PanelPresenter.getSearchedText();
        ds3PanelPresenter.getDs3PathIndicator().setText(resourceBundle.getString("searching"));
        ds3PanelPresenter.getDs3PathIndicatorTooltip().setText(resourceBundle.getString("searching"));
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = ds3Common.getDs3TreeTableView();
        final Session session = ds3Common.getCurrentSession();
        if (Guard.isStringNullOrEmpty(newValue)) {
            setVisibilityOfItemsInfo(true);
            refreshCompleteViewWorker.refreshCompleteTreeTableView();
        } else {
            try {
                ObservableList<TreeItem<Ds3TreeTableValue>> selectedItem = getSelectedItems();
                final TreeItem<Ds3TreeTableValue> root = ds3TreeTableView.getRoot();
                if (Guard.isNullOrEmpty(selectedItem) && (root != null && root.getValue() != null)) {
                    selectedItem = FXCollections.observableArrayList();
                    selectedItem.add(root);
                }

                final Optional<ImmutableList<Bucket>> searchableBuckets = setSearchableBucket(selectedItem, session, ds3TreeTableView);
                final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
                rootTreeItem.setExpanded(true);
                ds3TreeTableView.setShowRoot(false);
                setVisibilityOfItemsInfo(false);

                final SearchJobTask searchJobTask = new SearchJobTask(searchableBuckets.get(), newValue, session, workers, ds3Common, dateTimeUtils, loggingService);
                workers.execute(searchJobTask);
                searchJobTask.setOnSucceeded(SafeHandler.logHandle(event -> {
                    LOG.info("Search completed!");
                    Platform.runLater(() -> {
                        try {
                            final ObservableList<Ds3TreeTableItem> treeTableItems = FXCollections.observableArrayList(searchJobTask.get().stream().collect(Collectors.toList()));
                            ds3PanelPresenter.getDs3PathIndicator().setText(StringBuilderUtil.nObjectsFoundMessage(treeTableItems.size()).toString());
                            ds3PanelPresenter.getDs3PathIndicatorTooltip().setText(StringBuilderUtil.nObjectsFoundMessage(treeTableItems.size()).toString());
                            loggingService.logMessage(
                                    StringBuilderUtil.nObjectsFoundMessage(treeTableItems.size()).toString(), LogType.INFO);
                            treeTableItems.sort(Comparator.comparing(t -> t.getValue().getType().toString()));
                            treeTableItems.forEach(value -> rootTreeItem.getChildren().add(value));
                            if (rootTreeItem.getChildren().size() == 0) {
                                ds3TreeTableView.setPlaceholder(new Label(resourceBundle.getString("0_SearchResult")));
                            }
                            ds3TreeTableView.setRoot(rootTreeItem);
                            final TreeTableColumn<Ds3TreeTableValue, ?> ds3TreeTableValueTreeTableColumn = ds3TreeTableView
                                    .getColumns().get(1);
                            if (null != ds3TreeTableValueTreeTableColumn) {
                                ds3TreeTableValueTreeTableColumn.setVisible(true);
                            }
                        } catch (final Exception e) {
                            LOG.error("Search failed", e);
                            loggingService.logMessage(StringBuilderUtil.searchFailedMessage().append(e).toString(), LogType.ERROR);
                        }
                    });
                }));
                searchJobTask.setOnCancelled(SafeHandler.logHandle(event -> LOG.info("Search cancelled")));
            } catch (final Exception e) {
                LOG.error("Could not complete search: ", e);
            }
        }
    }

    private void setVisibilityOfItemsInfo(final boolean visibility) {
        ds3Common.getDs3PanelPresenter().getInfoLabel().setVisible(visibility);
        ds3Common.getDs3PanelPresenter().getCapacityLabel().setVisible(visibility);

    }

    public void showVersions() {
        final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = getSelectedItems();
        selectedItems.stream()
                .findFirst()
                .ifPresent(item -> {
                    versionPopup.show(item.getValue());
                });
    }

    private ObservableList<TreeItem<Ds3TreeTableValue>> getSelectedItems() {
        final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = ds3Common.getDs3TreeTableView();
        final TreeTableView.TreeTableViewSelectionModel<Ds3TreeTableValue> selectionModel = ds3TreeTableView.getSelectionModel();
        return selectionModel.getSelectedItems();
    }
}
