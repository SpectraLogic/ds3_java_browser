package com.spectralogic.dsbrowser.gui.services.ds3Panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteBucketTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteFilesTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteFolderTask;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DeleteService {

    private final static Logger LOG = LoggerFactory.getLogger(DeleteService.class);

    private static final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();

    /**
     * Delete a Single Selected Spectra S3 bucket     *
     *
     * @param ds3Common ds3Common object
     * @param values    list of objects to be deleted
     */
    public static void deleteBucket(final Ds3Common ds3Common, final ImmutableList<TreeItem<Ds3TreeTableValue>> values,
                                    final Workers workers) {
        LOG.info("Got delete bucket event");

        final Ds3PanelPresenter ds3PanelPresenter = ds3Common.getDs3PanelPresenter();

        final Session currentSession = ds3Common.getCurrentSession();
        if (currentSession != null) {
            final ImmutableList<String> buckets = getBuckets(values);
            if (buckets.size() > 1) {
                ds3Common.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("multiBucketNotAllowed"), LogType.ERROR);
                LOG.info("The user selected objects from multiple buckets.  This is not allowed.");
                Ds3Alert.show(null, resourceBundle.getString("multiBucketNotAllowed"), Alert.AlertType.INFORMATION);
                return;
            }
            final TreeItem<Ds3TreeTableValue> value = values.stream().findFirst().orElse(null);
            if (value != null) {
                final String bucketName = value.getValue().getBucketName();
                if (!Ds3PanelService.checkIfBucketEmpty(bucketName, currentSession)) {
                    Platform.runLater(() -> {
                        ds3Common.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("failedToDeleteBucket"), LogType.ERROR);
                        Ds3Alert.show(null, resourceBundle.getString("failedToDeleteBucket"), Alert.AlertType.INFORMATION);
                    });
                } else {
                    final Ds3DeleteBucketTask ds3DeleteBucketTask = new Ds3DeleteBucketTask(currentSession.getClient(), bucketName);
                    DeleteFilesPopup.show(ds3DeleteBucketTask, ds3Common);
                    ds3Common.getDs3TreeTableView().setRoot(new TreeItem<>());
                    RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers);
                    ds3PanelPresenter.getDs3PathIndicator().setText(StringConstants.EMPTY_STRING);
                    ds3PanelPresenter.getDs3PathIndicatorTooltip().setText(StringConstants.EMPTY_STRING);
                }
            }

        }

    }

    /**
     * Delete a Single Selected Spectra S3 folder     *
     *
     * @param ds3Common ds3Common object
     * @param values    list of objects to be deleted
     */
    public static void deleteFolder(final Ds3Common ds3Common, final ImmutableList<TreeItem<Ds3TreeTableValue>> values) {
        LOG.info("Got delete folder event");

        final Ds3PanelPresenter ds3PanelPresenter = ds3Common.getDs3PanelPresenter();
        final Session currentSession = ds3Common.getCurrentSession();

        if (currentSession != null) {
            final ImmutableList<String> buckets = getBuckets(values);

            if (buckets.size() > 1) {
                ds3Common.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("multiBucketNotAllowed"), LogType.ERROR);
                LOG.info("The user selected objects from multiple buckets.  This is not allowed.");
                Ds3Alert.show(null, resourceBundle.getString("multiBucketNotAllowed"), Alert.AlertType.INFORMATION);
                return;
            }
            final TreeItem<Ds3TreeTableValue> value = values.stream().findFirst().orElse(null);

            if (value != null) {

                final Ds3DeleteFolderTask deleteFolderTask = new Ds3DeleteFolderTask(currentSession.getClient(),
                        value.getValue().getBucketName(), value.getValue().getFullName());

                DeleteFilesPopup.show(deleteFolderTask, ds3Common);
                ds3PanelPresenter.getDs3PathIndicator().setText(StringConstants.EMPTY_STRING);
                ds3PanelPresenter.getDs3PathIndicatorTooltip().setText(StringConstants.EMPTY_STRING);
            }
        }
    }

    /**
     * Delete files from BlackPearl bucket/folder
     *
     * @param ds3Common ds3Common object
     * @param values    list of objects to be deleted
     */
    public static void deleteFiles(final Ds3Common ds3Common, final ImmutableList<TreeItem<Ds3TreeTableValue>> values,
                                   final Workers workers) {
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

        if (values.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
            Ds3PanelService.filterChanged(ds3Common, workers);
        }
        DeleteFilesPopup.show(ds3DeleteFilesTask, ds3Common);
        values.forEach(file -> Ds3PanelService.refresh(file.getParent()));
    }

    public static void managePathIndicator(final Ds3Common ds3Common, final Workers workers) {
        Platform.runLater(() -> {
            final TreeTableView<Ds3TreeTableValue> ds3TreeTable = ds3Common.getDs3TreeTableView();
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
            RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers);
        });
    }

    private static ImmutableList<String> getBuckets(final ImmutableList<TreeItem<Ds3TreeTableValue>> values) {
        return values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect
                (GuavaCollectors.immutableList());
    }
}
