package com.spectralogic.dsbrowser.gui.services.ds3Panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.DeleteObjectsRequest;
import com.spectralogic.ds3client.commands.DeleteObjectsResponse;
import com.spectralogic.ds3client.commands.spectrads3.DeleteFolderRecursivelySpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.DeleteFolderRecursivelySpectraS3Response;
import com.spectralogic.ds3client.models.DeleteResult;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.deletefiles.DeleteFilesPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3DeleteBucketTask;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DeleteService {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PanelService.class);

    private static final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();

    /**
     * Delete a Single Selected Spectra S3 bucket     *
     *
     * @param ds3Common ds3Common object
     * @param values    list of objects to be deleted
     */
    public static void deleteBucket(final Ds3Common ds3Common, final ImmutableList<TreeItem<Ds3TreeTableValue>> values, Workers workers) {
        LOG.info("Got delete bucket event");

        final Ds3PanelPresenter ds3PanelPresenter = ds3Common.getDs3PanelPresenter();

        final Session currentSession = ds3Common.getCurrentSession();
        if (currentSession != null) {
            final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue)
                    .map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());
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
    public static void deleteFolder(final Ds3Common ds3Common, final ImmutableList<TreeItem<Ds3TreeTableValue>> values, Workers workers) {
        LOG.info("Got delete bucket event");

        final Ds3PanelPresenter ds3PanelPresenter = ds3Common.getDs3PanelPresenter();
        final Session currentSession = ds3Common.getCurrentSession();

        if (currentSession != null) {
            final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue)
                    .map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());

            if (buckets.size() > 1) {
                ds3Common.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("multiBucketNotAllowed"), LogType.ERROR);
                LOG.info("The user selected objects from multiple buckets.  This is not allowed.");
                Ds3Alert.show(null, resourceBundle.getString("multiBucketNotAllowed"), Alert.AlertType.INFORMATION);
                return;
            }
            final TreeItem<Ds3TreeTableValue> value = values.stream().findFirst().orElse(null);

            if (value != null) {
                final String bucketName = value.getValue().getBucketName();

                final Ds3Task task = new Ds3Task(ds3Common.getCurrentSession().getClient()) {
                    @Override
                    protected Object call() throws Exception {

                        try {
                            final DeleteFolderRecursivelySpectraS3Response deleteFolderRecursivelySpectraS3Response = getClient().deleteFolderRecursivelySpectraS3(new DeleteFolderRecursivelySpectraS3Request(value.getValue().getBucketName(), value.getValue().getFullName()));
                            Platform.runLater(() -> {
                                //  deepStorageBrowserPresenter.logText("Delete response code: " + deleteFolderRecursivelySpectraS3Response.getStatusCode(), LogType.SUCCESS);
                                ds3Common.getDeepStorageBrowserPresenter().logText("Successfully deleted folder", LogType.SUCCESS);
                                final TreeTableView<Ds3TreeTableValue> ds3TreeTable = ds3Common.getDs3TreeTableView();
                                final TreeItem<Ds3TreeTableValue> selectedItem = ds3TreeTable.getSelectionModel().getSelectedItems().stream().findFirst().get().getParent();


                                if (ds3TreeTable.getRoot() == null || ds3TreeTable.getRoot().getValue() == null) {
                                    ds3TreeTable.setRoot(ds3TreeTable.getRoot().getParent());
                                    Platform.runLater(() -> {
                                        ds3TreeTable.getSelectionModel().clearSelection();
                                        ds3PanelPresenter.getDs3PathIndicator().setText(StringConstants.EMPTY_STRING);
                                        ds3PanelPresenter.getDs3PathIndicatorTooltip().setText(StringConstants.EMPTY_STRING);

                                    });
                                } else {
                                    ds3TreeTable.setRoot(selectedItem);
                                }
                                ds3TreeTable.getSelectionModel().select(selectedItem);

                                RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers);
                            });
                        } catch (final FailedRequestException fre) {
                            LOG.error("Failed to delete folder", fre);
                            ds3Common.getDeepStorageBrowserPresenter().logText("Failed to delete folder : " + fre, LogType.ERROR);
                            Ds3Alert.show(null, "Failed to delete a folder", Alert.AlertType.INFORMATION);
                        } catch (final IOException ioe) {
                            LOG.error("Failed to delete folder", ioe);
                            ds3Common.getDeepStorageBrowserPresenter().logText("Failed to delete folder" + ioe, LogType.ERROR);
                            Ds3Alert.show(null, "Failed to delete a folder", Alert.AlertType.INFORMATION);
                        }
                        return null;
                    }
                };
                DeleteFilesPopup.show(task, ds3Common);
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
    public static void deleteFiles(final Ds3Common ds3Common, final ImmutableList<TreeItem<Ds3TreeTableValue>> values, Workers workers) {
        LOG.info("Got delete bucket event");

        final DeepStorageBrowserPresenter deepStorageBrowserPresenter = ds3Common.getDeepStorageBrowserPresenter();

        final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());

        final TreeTableView<Ds3TreeTableValue> ds3TreeTable = ds3Common.getDs3TreeTableView();

        final ArrayList<Ds3TreeTableValue> filesToDelete = new ArrayList<>(values
                .stream()
                .map(TreeItem::getValue)
                .collect(Collectors.toList())
        );
        final Map<String, List<Ds3TreeTableValue>> bucketObjectsMap = filesToDelete.stream().collect(Collectors.groupingBy(Ds3TreeTableValue::getBucketName));
        final Set<String> bukcets = bucketObjectsMap.keySet();
        final TreeItem<Ds3TreeTableValue> selectedItem = ds3TreeTable.getSelectionModel().getSelectedItems().stream().findFirst().get().getParent();
        final Ds3Task task = new Ds3Task(ds3Common.getCurrentSession().getClient()) {
            @Override
            protected Object call() throws Exception {
                int deleteSize = 0;
                try {
                    for (final String bucket : buckets) {
                        final DeleteObjectsResponse deleteObjectsResponse = getClient().deleteObjects(new DeleteObjectsRequest(bucket, bucketObjectsMap.get(bucket).stream().map(Ds3TreeTableValue::getFullName).collect(Collectors.toList())));
                        final DeleteResult deleteResult = deleteObjectsResponse.getDeleteResult();
                        deleteSize++;
                        if (deleteSize == bukcets.size()) {
                            Platform.runLater(() -> {
                                if (values.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
                                    Ds3PanelService.filterChanged(ds3Common, workers);
                                }
                                // deepStorageBrmainowserPresenter.logText("Delete response code: " + deleteObjectsResponse.getStatusCode(), LogType.SUCCESS);
                                if (ds3TreeTable.getRoot() == null || ds3TreeTable.getRoot().getValue() == null) {
                                    ds3TreeTable.setRoot(ds3TreeTable.getRoot().getParent());
                                    Platform.runLater(() -> {
                                        ds3TreeTable.getSelectionModel().clearSelection();
                                        ds3Common.getDs3PanelPresenter().getDs3PathIndicator().setText("");
                                        ds3Common.getDs3PanelPresenter().getDs3PathIndicatorTooltip().setText("");

                                    });

                                } else {
                                    ds3TreeTable.setRoot(selectedItem);
                                }
                                ds3TreeTable.getSelectionModel().select(selectedItem);
                                //Ds3PanelService.refresh(selectedItem);
                                RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers);
                                ds3Common.getDeepStorageBrowserPresenter().logText("Successfully deleted file(s)", LogType.SUCCESS);

                            });
                        }
                    }

                } catch (final FailedRequestException fre) {
                    LOG.error("Failed to delete files", fre);
                    deepStorageBrowserPresenter.logText("Failed to delete files : " + fre, LogType.ERROR);
                    Ds3Alert.show(null, "Failed to delete files", Alert.AlertType.ERROR);
                } catch (final IOException ioe) {
                    LOG.error("Failed to delete files", ioe);
                    deepStorageBrowserPresenter.logText("Failed to delete files: " + ioe, LogType.ERROR);
                    Ds3Alert.show(null, "Failed to delete files", Alert.AlertType.ERROR);
                }
                return null;
            }
        };
        DeleteFilesPopup.show(task, ds3Common);
        values.stream().forEach(file -> Ds3PanelService.refresh(file.getParent()));
        ds3TreeTable.getSelectionModel().clearSelection();

    }

}
