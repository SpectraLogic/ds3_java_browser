/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteBucketTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteFilesTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteFoldersTask;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.LazyAlert;
import com.spectralogic.dsbrowser.gui.util.RefreshCompleteViewWorker;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public final class DeleteService {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteService.class);

    private static final LazyAlert errorAlert = new LazyAlert(Alert.AlertType.ERROR);

    /**
     * Delete a Single Selected Spectra S3 bucket
     *
     * @param ds3Common ds3Common object
     * @param values    list of objects to be deleted
     */
    public static void deleteBucket(final Ds3Common ds3Common,
                                    final ImmutableList<TreeItem<Ds3TreeTableValue>> values,
                                    final Workers workers,
                                    final LoggingService loggingService,
                                    final DateTimeUtils dateTimeUtils,
                                    final ResourceBundle resourceBundle) {
        LOG.info("Got delete bucket event");

        final Ds3PanelPresenter ds3PanelPresenter = ds3Common.getDs3PanelPresenter();

        final Session currentSession = ds3Common.getCurrentSession();
        if (currentSession != null) {
            final ImmutableList<String> buckets = getBuckets(values);
            if (buckets.size() > 1) {
                loggingService.logMessage(resourceBundle.getString("multiBucketNotAllowed"), LogType.ERROR);
                LOG.info("The user selected objects from multiple buckets.  This is not allowed.");
                errorAlert.showAlert(resourceBundle.getString("multiBucketNotAllowed"), "Error");
                return;
            }
            final Optional<TreeItem<Ds3TreeTableValue>> first = values.stream().findFirst();
            if (first.isPresent()) {
                final TreeItem<Ds3TreeTableValue> value = first.get();
                final String bucketName = value.getValue().getBucketName();
                if (!Ds3PanelService.checkIfBucketEmpty(bucketName, currentSession)) {
                    loggingService.logMessage(resourceBundle.getString("failedToDeleteBucket"), LogType.ERROR);
                    errorAlert.showAlert(resourceBundle.getString("failedToDeleteBucket"), "Error");
                } else {
                    final Ds3DeleteBucketTask ds3DeleteBucketTask = new Ds3DeleteBucketTask(currentSession.getClient(), bucketName);
                    DeleteFilesPopup.show(ds3DeleteBucketTask, ds3Common);
                    ds3Common.getDs3TreeTableView().setRoot(new TreeItem<>());
                    RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers, dateTimeUtils, loggingService);
                    ds3PanelPresenter.getDs3PathIndicator().setText(StringConstants.EMPTY_STRING);
                    ds3PanelPresenter.getDs3PathIndicatorTooltip().setText(StringConstants.EMPTY_STRING);
                }
            }
        } else {
            LOG.error("NULL Session when attempting to deleteBucket");
        }
    }

    /**
     * Delete folder(s)
     *
     * @param ds3Common ds3Common object
     * @param values    list of folders to be deleted
     */
    public static void deleteFolders(final Ds3Common ds3Common,
                                     final ImmutableList<TreeItem<Ds3TreeTableValue>> values) {
        LOG.info("Got delete folder event");

        final Ds3PanelPresenter ds3PanelPresenter = ds3Common.getDs3PanelPresenter();
        final Session currentSession = ds3Common.getCurrentSession();

        if (currentSession != null) {
            final ImmutableMultimap.Builder<String, String> deleteFoldersMap = ImmutableMultimap.builder();
            values.forEach(folder -> {
                deleteFoldersMap.put(folder.getValue().getBucketName(), folder.getValue().getFullName());
            });
            final Ds3DeleteFoldersTask deleteFolderTask = new Ds3DeleteFoldersTask(currentSession.getClient(),
            deleteFoldersMap.build());

            DeleteFilesPopup.show(deleteFolderTask, ds3Common);
            ds3PanelPresenter.getDs3PathIndicator().setText(StringConstants.EMPTY_STRING);
            ds3PanelPresenter.getDs3PathIndicatorTooltip().setText(StringConstants.EMPTY_STRING);
        }
    }

    /**
     * Delete files from BlackPearl bucket/folder
     *
     * @param ds3Common ds3Common object
     * @param values    list of objects to be deleted
     */
    public static void deleteFiles(final Ds3Common ds3Common, final ImmutableList<TreeItem<Ds3TreeTableValue>> values) {
        LOG.info("Got delete file(s) event");

        final ImmutableList<String> buckets = getBuckets(values);

        final ArrayList<Ds3TreeTableValue> filesToDelete = new ArrayList<>(values
                .stream()
                .map(TreeItem::getValue)
                .collect(Collectors.toList())
        );
        final Map<String, List<Ds3TreeTableValue>> bucketObjectsMap = filesToDelete.stream().collect(Collectors.groupingBy(Ds3TreeTableValue::getBucketName));

        final Ds3DeleteFilesTask ds3DeleteFilesTask = new Ds3DeleteFilesTask(
                ds3Common.getCurrentSession().getClient(), buckets, bucketObjectsMap);

        DeleteFilesPopup.show(ds3DeleteFilesTask, ds3Common);
    }

    public static void managePathIndicator(final Ds3Common ds3Common,
                                           final Workers workers,
                                           final DateTimeUtils dateTimeUtils,
                                           final LoggingService loggingService) {
        Platform.runLater(() -> {
            final TreeTableView<Ds3TreeTableValue> ds3TreeTable = ds3Common.getDs3TreeTableView();
            final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems = ds3TreeTable.getSelectionModel().getSelectedItems();

            if (selectedItems.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
                ds3TreeTable.getRoot().getChildren().removeAll(selectedItems);
                ds3TreeTable.getSelectionModel().clearSelection();
            } else {
                final TreeItem<Ds3TreeTableValue> selectedItem = ds3TreeTable.getSelectionModel().getSelectedItems().stream()
                        .findFirst().get().getParent();
                if (ds3TreeTable.getRoot() == null || ds3TreeTable.getRoot().getValue() == null) {
                    ds3TreeTable.setRoot(ds3TreeTable.getRoot().getParent());
                    ds3TreeTable.getSelectionModel().clearSelection();
                    ds3Common.getDs3PanelPresenter().getDs3PathIndicator().setText(StringConstants.EMPTY_STRING);
                    ds3Common.getDs3PanelPresenter().getDs3PathIndicatorTooltip().setText(StringConstants.EMPTY_STRING);
                } else {
                    ds3TreeTable.setRoot(selectedItem);
                }
                ds3TreeTable.getSelectionModel().select(selectedItem);

                ds3TreeTable.getSelectionModel().clearSelection();
                RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers, dateTimeUtils, loggingService);
            }
        });
    }

    private static ImmutableList<String> getBuckets(final ImmutableList<TreeItem<Ds3TreeTableValue>> values) {
        return values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect
                (GuavaCollectors.immutableList());
    }
}
