package com.spectralogic.dsbrowser.gui.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spectralogic.ds3client.commands.GetServiceRequest;
import com.spectralogic.ds3client.commands.GetServiceResponse;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.RecoverInterruptedJob;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.Ds3GetJob;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.TaskProgressView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public final class ParseJobInterruptionMap {

    private final static Logger LOG = LoggerFactory.getLogger(ParseJobInterruptionMap.class);

    public static Map<String, FilesAndFolderMap> getJobIDMap(final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints, final String endpoint, final MyTaskProgressView<Ds3JobTask> jobWorkers, final UUID jobId) {
        if (endpoints.stream().anyMatch(i -> i.containsKey(endpoint))) {
            final Map<String, Map<String, FilesAndFolderMap>> endpointMap = endpoints.stream().filter(i -> i.containsKey(endpoint)).findFirst().get();
            final Map<String, FilesAndFolderMap> jobIDMap = endpointMap.get(endpoint);
            final HashMap<String, FilesAndFolderMap> collect = jobIDMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
            final ObservableList<Ds3JobTask> tasks = jobWorkers.getTasks();
            tasks.forEach(i -> {
                UUID uuid = null;
                if (i instanceof Ds3PutJob) {
                    uuid = ((Ds3PutJob) i).getJobId();
                } else if (i instanceof Ds3GetJob) {
                    uuid = ((Ds3GetJob) i).getJobId();
                } else if (i instanceof RecoverInterruptedJob) {
                    uuid = ((RecoverInterruptedJob) i).getUuid();
                }
                if (uuid != null && uuid != jobId) {
                    if (collect.containsKey(uuid.toString())) {
                        collect.remove(uuid.toString());
                    }
                }
            });
            return collect;
        }
        return null;
    }

    public static Map<String, FilesAndFolderMap> removeJobID(final JobInterruptionStore jobInterruptionStore, final String uuid, final String endpoint, final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> completeArrayList = jobInterruptionStore.getJobIdsModel().getEndpoints();
        final Map<String, FilesAndFolderMap> jobIdMap;
        if (completeArrayList.stream().anyMatch(i -> i.containsKey(endpoint))) {
            final Map<String, Map<String, FilesAndFolderMap>> endpointsMapImmutableMap = completeArrayList.stream().filter(i -> i.containsKey(endpoint)).findFirst().get();
            final HashMap<String, Map<String, FilesAndFolderMap>> endpointHashMap = endpointsMapImmutableMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
            completeArrayList.remove(endpointsMapImmutableMap);
            if (endpointHashMap.containsKey(endpoint)) {
                jobIdMap = endpointHashMap.get(endpoint)
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
                if (jobIdMap.containsKey(uuid)) {
                    jobIdMap.remove(uuid);
                }
                endpointHashMap.put(endpoint, jobIdMap);
            }
            try {
                completeArrayList.add(endpointHashMap);
                jobInterruptionStore.getJobIdsModel().setEndpoints(completeArrayList);
                JobInterruptionStore.saveJobInterruptionStore(jobInterruptionStore);
            } catch (final Exception e) {
                LOG.error("Encountered an exception when trying to remove a job", e);
                if (deepStorageBrowserPresenter != null) {
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to remove job id " + e, LogType.ERROR));
                }
            }
        }
        if(deepStorageBrowserPresenter != null) {
            return getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpoint, deepStorageBrowserPresenter.getJobProgressView(), null);
        }
        return null;
    }

    public static void setButtonAndCountNumber(final Map<String, FilesAndFolderMap> jobIDMap, final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        if (jobIDMap != null) {
            if (jobIDMap.size() == 0) {
                Platform.runLater(() -> {
                    deepStorageBrowserPresenter.getCircle().setVisible(false);
                    deepStorageBrowserPresenter.getJobButton().setDisable(true);
                    deepStorageBrowserPresenter.getCount().setText("");
                });
            } else {
                Platform.runLater(() -> {
                    deepStorageBrowserPresenter.getCircle().setVisible(true);
                    deepStorageBrowserPresenter.getJobButton().setDisable(false);
                    deepStorageBrowserPresenter.getCount().setText(jobIDMap.size() + "");
                });
            }

        } else {
            Platform.runLater(() -> {
                deepStorageBrowserPresenter.getCircle().setVisible(false);
                deepStorageBrowserPresenter.getJobButton().setDisable(true);
                deepStorageBrowserPresenter.getCount().setText("");
            });
        }
    }

    public static void saveValuesToFiles(final JobInterruptionStore jobInterruptionStore, final ImmutableMap<String, Path> filesMap, final ImmutableMap<String, Path> foldersMap, final String endpoint, final UUID jobId, final long totalJobSize, final String targetLocation, final String jobType, final String bucket) {
        final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> completeArrayList = jobInterruptionStore.getJobIdsModel().getEndpoints();
        boolean isNonAdjacent = false;
        if (filesMap.size() != 0 && foldersMap.size() != 0) {
            isNonAdjacent = true;
        }
        final FilesAndFolderMap filesAndFolderMap = new FilesAndFolderMap(filesMap, foldersMap, jobType, DateFormat.formatDate(new Date()), isNonAdjacent, targetLocation, totalJobSize, bucket);
        if (completeArrayList != null && completeArrayList.stream().anyMatch(i -> i.containsKey(endpoint))) {
            final Map<String, Map<String, FilesAndFolderMap>> endpointsImmutableMap = completeArrayList.stream().filter(i -> i.containsKey(endpoint)).findFirst().get();
            if (endpointsImmutableMap != null && endpointsImmutableMap.containsKey(endpoint)) {
                final Map<String, FilesAndFolderMap> jobIdImmutableMap = endpointsImmutableMap.get(endpoint);
                jobIdImmutableMap.put(jobId.toString(), filesAndFolderMap);
            } else {
                final Map<String, FilesAndFolderMap> jobIdHashMap = new HashMap<>();
                jobIdHashMap.put(jobId.toString(), filesAndFolderMap);
                endpointsImmutableMap.put(endpoint, jobIdHashMap);
            }
        } else {
            final Map<String, Map<String, FilesAndFolderMap>> endpointsHashMap = new HashMap<>();
            final Map<String, FilesAndFolderMap> jobIdHashMap = new HashMap<>();
            jobIdHashMap.put(jobId.toString(), filesAndFolderMap);
            endpointsHashMap.put(endpoint, jobIdHashMap);
            completeArrayList.add(endpointsHashMap);
        }
        try {
            jobInterruptionStore.getJobIdsModel().setEndpoints(completeArrayList);
            JobInterruptionStore.saveJobInterruptionStore(jobInterruptionStore);
        } catch (final Exception e) {
            LOG.error("Failed to save job ids", e);
        }
    }

    public static void cancelAllRunningJobs(final JobWorkers jobWorkers, final JobInterruptionStore jobInterruptionStore, final Logger LOG, final Workers workers, final Ds3Common ds3Common) {
        if (jobWorkers.getTasks().size() != 0) {
            final CancelRunningJobsTask cancelRunningJobsTask = new CancelRunningJobsTask(jobWorkers, jobInterruptionStore);
            workers.execute(cancelRunningJobsTask);
            cancelRunningJobsTask.setOnSucceeded(event -> {
                refreshCompleteTreeTableView(ds3Common, workers);
                if (cancelRunningJobsTask.getValue() != null) {
                    LOG.info("Cancelled job. {}", cancelRunningJobsTask.getValue());
                }
            });

        }
    }


    public static void cancelAllRunningJobsBySession(final JobWorkers jobWorkers, final JobInterruptionStore jobInterruptionStore, final Logger LOG, final Workers workers, final Session session) {
        final ImmutableList<Ds3JobTask> tasks = jobWorkers.getTasks().stream().collect(GuavaCollectors.immutableList());
        if (tasks.size() != 0) {
            final Task cancelAllRunningJobs = new Task() {
                @Override
                protected Object call() throws Exception {
                    tasks.forEach(i -> {
                        try {
                            //Ds3Client ds3Client = null;
                            if (i instanceof Ds3PutJob) {
                                final Ds3PutJob ds3PutJob = (Ds3PutJob) i;
                                //  ds3Client =  ds3PutJob.getClient();
                                if (ds3PutJob.getClient().getConnectionDetails().getCredentials().getClientId().equals(session.getClient().getConnectionDetails().getCredentials().getClientId()) && ds3PutJob.getClient().getConnectionDetails().getCredentials().getKey().equals(session.getClient().getConnectionDetails().getCredentials().getKey())) {
                                    ds3PutJob.cancel();
                                    ParseJobInterruptionMap.removeJobID(jobInterruptionStore, ds3PutJob.getJobId().toString(), ds3PutJob.getClient().getConnectionDetails().getEndpoint(), null);
                                }
                            } else if (i instanceof Ds3GetJob) {
                                final Ds3GetJob ds3GetJob = (Ds3GetJob) i;
                                //ds3Client = ds3GetJob.getDs3Client();
                                if (ds3GetJob.getDs3Client().getConnectionDetails().getCredentials().getClientId().equals(session.getClient().getConnectionDetails().getCredentials().getClientId()) && ds3GetJob.getDs3Client().getConnectionDetails().getCredentials().getKey().equals(session.getClient().getConnectionDetails().getCredentials().getKey())) {
                                    ds3GetJob.cancel();
                                    ParseJobInterruptionMap.removeJobID(jobInterruptionStore, ds3GetJob.getJobId().toString(), ds3GetJob.getDs3Client().getConnectionDetails().getEndpoint(), null);
                                }
                            } else if (i instanceof RecoverInterruptedJob) {
                                final RecoverInterruptedJob recoverInterruptedJob = (RecoverInterruptedJob) i;
                                // ds3Client = recoverInterruptedJob.getDs3Client();
                                if (recoverInterruptedJob.getDs3Client().getConnectionDetails().getCredentials().getClientId().equals(session.getClient().getConnectionDetails().getCredentials().getClientId()) && recoverInterruptedJob.getDs3Client().getConnectionDetails().getCredentials().getKey().equals(session.getClient().getConnectionDetails().getCredentials().getKey())) {
                                    recoverInterruptedJob.cancel();
                                    ParseJobInterruptionMap.removeJobID(jobInterruptionStore, recoverInterruptedJob.getUuid().toString(), recoverInterruptedJob.getDs3Client().getConnectionDetails().getEndpoint(), null);
                                }
                            }
                        } catch (final Exception e1) {
                            LOG.error("Failed to cancel job", e1);
                        }
                    });
                    return null;
                }
            };
            workers.execute(cancelAllRunningJobs);
            cancelAllRunningJobs.setOnSucceeded(event -> {
                if (cancelAllRunningJobs.getValue() != null) {
                    LOG.info("Cancelled job. {}", cancelAllRunningJobs.getValue());
                }
            });
        }
    }

    //Refresh blackpearl side
    public static void refreshCompleteTreeTableView(final Ds3Common ds3Common, final Workers workers) {
        if (ds3Common.getCurrentSession() != null && ds3Common.getCurrentTabPane() != null) {
            final Session session = ds3Common.getCurrentSession().stream().findFirst().orElse(null);
            ds3Common.getDeepStorageBrowserPresenter().logText("Refreshing session " + session.getSessionName() + "-" + session.getEndpoint(), LogType.INFO);
            @SuppressWarnings("unchecked")
            final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = getTreeTableView(ds3Common);
            final TreeItem<Ds3TreeTableValue> selectedRoot = ds3TreeTableView.getRoot();
            //invisible column of full path
            if (ds3TreeTableView != null && ds3TreeTableView.getColumns() != null) {
                final TreeTableColumn<Ds3TreeTableValue, ?> ds3TreeTableValueTreeTableColumn = ds3TreeTableView.getColumns().get(1);
                if (ds3TreeTableValueTreeTableColumn != null) {
                    ds3TreeTableValueTreeTableColumn.setVisible(false);
                }
            }
            if (selectedRoot != null && selectedRoot.getValue() != null) {
                ds3TreeTableView.getSelectionModel().clearSelection();
                ds3TreeTableView.setRoot(selectedRoot);
                ds3TreeTableView.getSelectionModel().select(selectedRoot);
                ((Ds3TreeTableItem) selectedRoot).refresh(ds3Common);
                ds3Common.getDs3PanelPresenter().calculateFiles(ds3TreeTableView);
                // ds3Common.getDs3PanelPresenter().getDs3PathIndicator().setText(selectedRoot.getValue().getFullName());
            } else {
                final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
                final Ds3Task getBucketTask = new Ds3Task(session.getClient()) {

                    @Override
                    protected Object call() throws Exception {
                        try {
                            final GetServiceResponse response = session.getClient().getService(new GetServiceRequest());
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
                            final ImmutableList<Ds3TreeTableItem> treeItems = buckets.stream().map(value -> new Ds3TreeTableItem(value.getName(), session, value, workers)).collect(GuavaCollectors.immutableList());
                            rootTreeItem.getChildren().addAll(treeItems);
                            Platform.runLater(() -> ds3TreeTableView.setRoot(rootTreeItem));
                        } catch (final FailedRequestException ex) {
                            LOG.error("Request failed", ex);
                            Platform.runLater(() -> ds3Common.getDeepStorageBrowserPresenter().logTextForParagraph(ex.getError().getMessage(), LogType.ERROR));
                        } catch (final Exception e) {
                            LOG.error("Request failed", e);
                            Platform.runLater(() -> ds3Common.getDeepStorageBrowserPresenter().logText("Failed to delete files" + e, LogType.ERROR));
                        }
                        return null;
                    }
                };
                workers.execute(getBucketTask);
                getBucketTask.setOnSucceeded(event -> {
                    final ObservableList<TreeItem<Ds3TreeTableValue>> children = ds3TreeTableView.getRoot().getChildren();
                    children.forEach(i -> i.expandedProperty().addListener((observable, oldValue, newValue) -> {
                        final BooleanProperty bb = (BooleanProperty) observable;
                        final TreeItem<Ds3TreeTableValue> bean = (TreeItem<Ds3TreeTableValue>) bb.getBean();
                        ds3Common.getExpandedNodesInfo().put(session.getSessionName() + "-" + session.getEndpoint(), bean);
                    }));
                    if (ds3Common.getExpandedNodesInfo().containsKey(session.getSessionName() + "-" + session.getEndpoint())) {
                        final TreeItem<Ds3TreeTableValue> item = ds3Common.getExpandedNodesInfo().get(session.getSessionName() + "-" + session.getEndpoint());
                        if (children.stream().anyMatch(i -> i.getValue().getBucketName().equals(item.getValue().getBucketName()))) {
                            final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = children.stream().filter(i -> i.getValue().getBucketName().equals(item.getValue().getBucketName())).findFirst().get();
                            ds3TreeTableValueTreeItem.setExpanded(false);
                            if (!ds3TreeTableValueTreeItem.isLeaf() && !ds3TreeTableValueTreeItem.isExpanded()) {
                                ds3TreeTableView.getSelectionModel().select(ds3TreeTableValueTreeItem);
                                ds3TreeTableValueTreeItem.setExpanded(true);
                            }
                        }
                    }
                });
            }
        }
    }

    private static TreeTableView<Ds3TreeTableValue> getTreeTableView(final Ds3Common ds3Common) {
        final TabPane ds3SessionTabPane = ds3Common.getCurrentTabPane().stream().findFirst().get();
        final VBox vbox = (VBox) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
        return (TreeTableView<Ds3TreeTableValue>) vbox.getChildren().stream().filter(i -> i instanceof TreeTableView).findFirst().get();
    }

}
