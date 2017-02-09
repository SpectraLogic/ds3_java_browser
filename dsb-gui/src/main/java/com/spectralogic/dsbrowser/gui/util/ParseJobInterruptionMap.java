package com.spectralogic.dsbrowser.gui.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.RecoverInterruptedJob;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3GetJob;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CancelAllTaskBySession;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.services.tasks.GetServiceTask;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public final class ParseJobInterruptionMap {

    private final static Logger LOG = LoggerFactory.getLogger(ParseJobInterruptionMap.class);

    public static Map<String, FilesAndFolderMap> getJobIDMap(final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints, final String endpoint, final MyTaskProgressView<Ds3JobTask> jobWorkers, final UUID jobId) {
        if (endpoints.stream().anyMatch(i -> i.containsKey(endpoint))) {
            final Map<String, Map<String, FilesAndFolderMap>> endpointMap = endpoints.stream().filter(i -> i
                    .containsKey(endpoint)).findFirst().orElse(null);
            if (null != endpoint) {
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
            } else {
                LOG.info("No endpoint present");
                return null;
            }
        }
        return null;
    }

    public static Map<String, FilesAndFolderMap> removeJobID(final JobInterruptionStore jobInterruptionStore, final String uuid, final String endpoint, final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> completeArrayList = jobInterruptionStore.getJobIdsModel().getEndpoints();
        if (completeArrayList.stream().anyMatch(i -> i.containsKey(endpoint))) {
            try {
                removeJobIdFromFile(jobInterruptionStore, uuid, endpoint);
            } catch (final Exception e) {
                LOG.error("Encountered an exception when trying to remove a job", e);
                if (deepStorageBrowserPresenter != null) {
                    Platform.runLater(() -> deepStorageBrowserPresenter.logText("Failed to remove job id " + e, LogType.ERROR));
                }
            }
        }
        if (deepStorageBrowserPresenter != null) {
            return getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpoint, deepStorageBrowserPresenter.getJobProgressView(), null);
        }
        return null;
    }

    public static void removeJobIdFromFile(final JobInterruptionStore jobInterruptionStore, final String uuid, final
    String endpoint) throws Exception {
        final Map<String, FilesAndFolderMap> jobIdMap;
        final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> completeArrayList = jobInterruptionStore.getJobIdsModel().getEndpoints();
        final Map<String, Map<String, FilesAndFolderMap>> endpointsImmutableMap = completeArrayList.stream().filter(i
                -> i.containsKey(endpoint)).findFirst().orElse(null);
        if (null != endpointsImmutableMap) {
            final HashMap<String, Map<String, FilesAndFolderMap>> endpointHashMap = endpointsImmutableMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
            completeArrayList.remove(endpointsImmutableMap);
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
            completeArrayList.add(endpointHashMap);
            jobInterruptionStore.getJobIdsModel().setEndpoints(completeArrayList);
        } else {
            LOG.info("No endpoint present");
        }
        JobInterruptionStore.saveJobInterruptionStore(jobInterruptionStore);
    }

    public static void setButtonAndCountNumber(final Map<String, FilesAndFolderMap> jobIDMap, final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        if (jobIDMap != null) {
            if (jobIDMap.size() == 0) {
                Platform.runLater(() -> setInterruptJobStatus(deepStorageBrowserPresenter, false, 0));
            } else {
                Platform.runLater(() -> setInterruptJobStatus(deepStorageBrowserPresenter, true, jobIDMap.size()));
            }

        } else {
            Platform.runLater(() -> setInterruptJobStatus(deepStorageBrowserPresenter, false, 0));
        }
    }

    private static void setInterruptJobStatus(final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
                                              final boolean isJobInterrupted, final int size) {
        if (isJobInterrupted) {
            deepStorageBrowserPresenter.getCircle().setVisible(true);
            deepStorageBrowserPresenter.getJobButton().setDisable(false);
            deepStorageBrowserPresenter.getCount().setText(String.valueOf(size));
        } else {
            deepStorageBrowserPresenter.getCircle().setVisible(false);
            deepStorageBrowserPresenter.getJobButton().setDisable(true);
            deepStorageBrowserPresenter.getCount().setText("");
        }
    }

    public static void saveValuesToFiles(final JobInterruptionStore jobInterruptionStore, final ImmutableMap<String, Path> filesMap, final ImmutableMap<String, Path> foldersMap, final String endpoint, final UUID jobId, final long totalJobSize, final String targetLocation, final String jobType, final String bucket) {
        if (jobInterruptionStore != null && jobInterruptionStore.getJobIdsModel() != null) {
            try {
                final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> completeArrayList = jobInterruptionStore.getJobIdsModel().getEndpoints();
                boolean isNonAdjacent = false;
                if (filesMap.size() != 0 && foldersMap.size() != 0) {
                    isNonAdjacent = true;
                }
                final FilesAndFolderMap filesAndFolderMap = new FilesAndFolderMap(filesMap, foldersMap, jobType, DateFormat.formatDate(new Date()), isNonAdjacent, targetLocation, totalJobSize, bucket);
                if (completeArrayList != null) {
                    if (completeArrayList.stream().anyMatch(i -> i.containsKey(endpoint))) {
                        final Map<String, Map<String, FilesAndFolderMap>> endpointsImmutableMap = completeArrayList
                                .stream().filter(i -> i.containsKey(endpoint)).findFirst().orElse(null);
                        if (endpointsImmutableMap != null) {
                            if (endpointsImmutableMap.containsKey(endpoint)) {
                                final Map<String, FilesAndFolderMap> jobIdImmutableMap = endpointsImmutableMap.get(endpoint);
                                jobIdImmutableMap.put(jobId.toString(), filesAndFolderMap);
                            } else {
                                final Map<String, FilesAndFolderMap> jobIdHashMap = new HashMap<>();
                                jobIdHashMap.put(jobId.toString(), filesAndFolderMap);
                                endpointsImmutableMap.put(endpoint, jobIdHashMap);
                            }
                        }
                    } else {
                        final Map<String, Map<String, FilesAndFolderMap>> endpointsHashMap = new HashMap<>();
                        final Map<String, FilesAndFolderMap> jobIdHashMap = new HashMap<>();
                        jobIdHashMap.put(jobId.toString(), filesAndFolderMap);
                        endpointsHashMap.put(endpoint, jobIdHashMap);
                        completeArrayList.add(endpointsHashMap);
                    }
                    jobInterruptionStore.getJobIdsModel().setEndpoints(completeArrayList);
                } else {
                    LOG.info("No entry found");
                }
                JobInterruptionStore.saveJobInterruptionStore(jobInterruptionStore);
            } catch (final Exception e) {
                LOG.error("Failed to save job ids", e);
            }
        }
    }

    public static void cancelAllRunningJobs(final JobWorkers jobWorkers, final JobInterruptionStore jobInterruptionStore, final Logger LOG, final Workers workers, final Ds3Common ds3Common) {
        if (jobWorkers.getTasks().size() != 0) {
            final CancelRunningJobsTask cancelRunningJobsTask = cancelTasks(jobWorkers, jobInterruptionStore, workers);
            cancelRunningJobsTask.setOnSucceeded(event -> {
                refreshCompleteTreeTableView(ds3Common, workers);
                if (cancelRunningJobsTask.getValue() != null) {
                    LOG.info("Cancelled job. {}", cancelRunningJobsTask.getValue());
                }
            });

        }
    }

    public static CancelRunningJobsTask cancelTasks(final JobWorkers jobWorkers, final JobInterruptionStore
            jobInterruptionStore, final Workers workers) {
        final CancelRunningJobsTask cancelRunningJobsTask = new CancelRunningJobsTask(jobWorkers, jobInterruptionStore);
        workers.execute(cancelRunningJobsTask);
        return cancelRunningJobsTask;
    }

    public static CancelAllTaskBySession cancelAllRunningJobsBySession(final JobWorkers jobWorkers, final JobInterruptionStore
            jobInterruptionStore, final Logger LOG, final Workers workers, final Session session) {
        final ImmutableList<Ds3JobTask> tasks = jobWorkers.getTasks().stream().collect(GuavaCollectors.immutableList());
        if (tasks.size() != 0) {
            final CancelAllTaskBySession cancelAllRunningJobs = new CancelAllTaskBySession(tasks, session,
                    jobInterruptionStore);
            workers.execute(cancelAllRunningJobs);
            cancelAllRunningJobs.setOnSucceeded(event -> {
                if (cancelAllRunningJobs.getValue() != null) {
                    LOG.info("Cancelled job. {}", cancelAllRunningJobs.getValue());
                }
            });
            return cancelAllRunningJobs;
        } else {
            return null;
        }
    }

    //Refresh blackpearl side
    @SuppressWarnings("unchecked")
    public static void refreshCompleteTreeTableView(final Ds3Common ds3Common, final Workers workers) {
        if (ds3Common.getCurrentSession() != null && ds3Common.getCurrentTabPane() != null) {
            final Session session = ds3Common.getCurrentSession().stream().findFirst().orElse(null);
            ds3Common.getDeepStorageBrowserPresenter().logText("Refreshing session " + session.getSessionName() +
                    StringConstants.SESSION_SEPARATOR +
                    session.getEndpoint(), LogType.INFO);
            @SuppressWarnings("unchecked")
            final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = getTreeTableView(ds3Common);
            //invisible column of full path
            if (ds3TreeTableView != null && ds3TreeTableView.getColumns() != null) {
                final TreeTableColumn<Ds3TreeTableValue, ?> ds3TreeTableValueTreeTableColumn = ds3TreeTableView.getColumns().get(1);
                if (ds3TreeTableValueTreeTableColumn != null) {
                    ds3TreeTableValueTreeTableColumn.setVisible(false);
                }
                final TreeItem<Ds3TreeTableValue> selectedRoot = ds3TreeTableView.getRoot();
                if (selectedRoot != null && selectedRoot.getValue() != null) {
                    ds3TreeTableView.getSelectionModel().clearSelection();
                    ds3TreeTableView.setRoot(selectedRoot);
                    ds3TreeTableView.getSelectionModel().select(selectedRoot);
                    ((Ds3TreeTableItem) selectedRoot).refresh();
                    ds3Common.getDs3PanelPresenter().calculateFiles(ds3TreeTableView);
                } else {
                    final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
                    final GetServiceTask getServiceTask = new GetServiceTask(rootTreeItem.getChildren(), session, workers, ds3Common);
                    workers.execute(getServiceTask);
                    getServiceTask.setOnSucceeded(event -> {
                        ds3TreeTableView.setRoot(rootTreeItem);
                        if (ds3Common.getExpandedNodesInfo().containsKey(session.getSessionName() + StringConstants.SESSION_SEPARATOR +
                                session.getEndpoint())) {
                            rootTreeItem.getChildren().forEach(i ->
                                    i.expandedProperty().addListener((observable, oldValue, newValue) -> {
                                        final BooleanProperty bb = (BooleanProperty) observable;
                                        final TreeItem<Ds3TreeTableValue> bean = (TreeItem<Ds3TreeTableValue>) bb.getBean();
                                        ds3Common.getExpandedNodesInfo().put(session.getSessionName() + "-" + session.getEndpoint(), bean);
                                    }));
                            final TreeItem<Ds3TreeTableValue> item = ds3Common.getExpandedNodesInfo().get(session.getSessionName() + "-" + session.getEndpoint());
                            if (rootTreeItem.getChildren().stream().anyMatch(i -> i.getValue().getBucketName().equals(item.getValue().getBucketName()))) {
                                final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = rootTreeItem.getChildren().stream().filter(i -> i.getValue().getBucketName().equals(item.getValue().getBucketName())).findFirst().get();
                                ds3TreeTableValueTreeItem.setExpanded(false);
                                if (!ds3TreeTableValueTreeItem.isLeaf() && !ds3TreeTableValueTreeItem.isExpanded()) {
                                    ds3TreeTableView.getSelectionModel().select(ds3TreeTableValueTreeItem);
                                    ds3TreeTableValueTreeItem.setExpanded(true);
                                }
                            }
                        }
                    });
                }
            } else {
                LOG.info("TreeView is null");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static TreeTableView<Ds3TreeTableValue> getTreeTableView(final Ds3Common ds3Common) {
        final TabPane ds3SessionTabPane = ds3Common.getCurrentTabPane().stream().findFirst().orElse(null);
        if (null != ds3SessionTabPane) {
            final VBox vbox = (VBox) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
            return (TreeTableView<Ds3TreeTableValue>) vbox.getChildren().stream().filter(i -> i instanceof TreeTableView)
                    .findFirst().orElse(null);
        } else {
            LOG.info("TabPane is null");
            return null;
        }
    }

}
