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

package com.spectralogic.dsbrowser.gui.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public final class ParseJobInterruptionMap {

    private final static Logger LOG = LoggerFactory.getLogger(ParseJobInterruptionMap.class);

    public static Map<String, FilesAndFolderMap> getJobIDMap(
            final List<Map<String, Map<String, FilesAndFolderMap>>> endpoints,
            final String endpoint,
            final DeepStorageBrowserTaskProgressView<Ds3JobTask> jobWorkers,
            final UUID jobId) {
        if (!Guard.isNullOrEmpty(endpoints) && endpoints.stream().anyMatch(i -> i.containsKey(endpoint))
                && !Guard.isStringNullOrEmpty(endpoint)) {

            final Optional<Map<String, Map<String, FilesAndFolderMap>>> first = endpoints.stream()
                    .filter(i -> i.containsKey(endpoint)).findFirst();
            if (first.isPresent()) {
                final Map<String, Map<String, FilesAndFolderMap>> endpointMap = first.get();
                final Map<String, FilesAndFolderMap> jobIDMap = endpointMap.get(endpoint);
                final HashMap<String, FilesAndFolderMap> collect = jobIDMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
                final ObservableList<Ds3JobTask> tasks = jobWorkers.getTasks();
                tasks.forEach(task -> {
                    final UUID uuid = task.getJobId();
                    if (uuid != null && uuid != jobId && collect.containsKey(uuid.toString())) {
                        collect.remove(uuid.toString());
                    }
                });
                return collect;
            }
        } else {
            LOG.info("No endpoint present");
        }
        return null;
    }

    public static Map<String, FilesAndFolderMap> removeJobID(final JobInterruptionStore jobInterruptionStore,
                                                             final String uuid,
                                                             final String endpoint,
                                                             final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
                                                             final LoggingService loggingService) {
        final ImmutableList<Map<String, Map<String, FilesAndFolderMap>>> completeArrayList = jobInterruptionStore.getJobIdsModel().getEndpoints().stream().collect(GuavaCollectors.immutableList());
        if (!Guard.isNullOrEmpty(completeArrayList)
                && completeArrayList.stream().anyMatch(i -> i.containsKey(endpoint))) {
            try {
                removeJobIdFromFile(jobInterruptionStore, uuid, endpoint);
            } catch (final IOException e) {
                LOG.error("Encountered an exception when trying to remove a job", e);
                loggingService.logMessage("Failed to remove job id: " + e, LogType.ERROR);
            }
        }
        if (deepStorageBrowserPresenter != null && jobInterruptionStore.getJobIdsModel().getEndpoints() != null) {
            final ImmutableList<Map<String, Map<String, FilesAndFolderMap>>> endpointsMap = jobInterruptionStore.getJobIdsModel().getEndpoints().stream().collect(GuavaCollectors.immutableList());
            return getJobIDMap(endpointsMap, endpoint, deepStorageBrowserPresenter.getJobProgressView(), null);
        }
        return null;
    }

    public static void removeJobIdFromFile(final JobInterruptionStore jobInterruptionStore,
                                           final String uuid,
                                           final String endpoint) throws IOException {
        final Map<String, FilesAndFolderMap> jobIdMap;
        final ObservableList<Map<String, Map<String, FilesAndFolderMap>>> completeArrayList = FXCollections.observableArrayList(jobInterruptionStore.getJobIdsModel().getEndpoints());
        final Optional<Map<String, Map<String, FilesAndFolderMap>>> first = completeArrayList.stream()
                .filter(i -> i.containsKey(endpoint)).findFirst();
        if (first.isPresent()) {
            final Map<String, Map<String, FilesAndFolderMap>> endpointsMap = new HashMap<>(first.get());
            if (!Guard.isMapNullOrEmpty(endpointsMap)) {
                final Map<String, Map<String, FilesAndFolderMap>> endpointHashMap = endpointsMap.entrySet()
                        .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
                completeArrayList.remove(endpointsMap);
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
    }

    public static void setButtonAndCountNumber(final Map<String, FilesAndFolderMap> jobIDMap,
                                               final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        if (!Guard.isMapNullOrEmpty(jobIDMap)) {
            setInterruptJobStatus(deepStorageBrowserPresenter, true, jobIDMap.size());
        } else {
            setInterruptJobStatus(deepStorageBrowserPresenter, false, 0);
        }
    }

    private static void setInterruptJobStatus(final DeepStorageBrowserPresenter deepStorageBrowserPresenter,
                                              final boolean isJobInterrupted,
                                              final int size) {
        Platform.runLater(() -> {
            if (isJobInterrupted) {
                deepStorageBrowserPresenter.getNumInterruptedJobsCircle().setVisible(true);
                deepStorageBrowserPresenter.getRecoverInterruptedJobsButton().setDisable(false);
                deepStorageBrowserPresenter.getNumInterruptedJobsLabel().setText(String.valueOf(size));
            } else {
                deepStorageBrowserPresenter.getNumInterruptedJobsCircle().setVisible(false);
                deepStorageBrowserPresenter.getRecoverInterruptedJobsButton().setDisable(true);
                deepStorageBrowserPresenter.getNumInterruptedJobsLabel().setText("");
            }
        });
    }

    public static void saveValuesToFiles(final JobInterruptionStore jobInterruptionStore,
                                         final Map<String, Path> filesMap,
                                         final Map<String, Path> foldersMap,
                                         final String endpoint,
                                         final UUID jobId,
                                         final long totalJobSize,
                                         final String targetLocation,
                                         final String jobType,
                                         final String bucket) {
        if (jobInterruptionStore != null && jobInterruptionStore.getJobIdsModel() != null) {
                final ObservableList<Map<String, Map<String, FilesAndFolderMap>>> completeArrayList = FXCollections.observableArrayList(jobInterruptionStore.getJobIdsModel().getEndpoints());
                final boolean isNonAdjacent = !(Guard.isMapNullOrEmpty(filesMap) || Guard.isMapNullOrEmpty(foldersMap));

                final FilesAndFolderMap filesAndFolderMap = new FilesAndFolderMap(filesMap, foldersMap, jobType, DateTimeUtils.now(), isNonAdjacent, targetLocation, totalJobSize, bucket);
                if (!Guard.isNullOrEmpty(completeArrayList) && completeArrayList.stream().anyMatch(i -> i.containsKey(endpoint))) {

                    final Optional<Map<String, Map<String, FilesAndFolderMap>>> first = completeArrayList
                            .stream().filter(i -> i.containsKey(endpoint)).findFirst();

                    if (first.isPresent()) {
                        final Map<String, Map<String, FilesAndFolderMap>> endpointsMap = first.get();

                        if (!Guard.isMapNullOrEmpty(endpointsMap) && endpointsMap.containsKey(endpoint)) {
                            final Map<String, FilesAndFolderMap> jobIdImmutableMap = endpointsMap.get(endpoint);
                            jobIdImmutableMap.put(jobId.toString(), filesAndFolderMap);
                        } else {
                            final ImmutableMap<String, FilesAndFolderMap> jobIdHashMap = ImmutableMap.of(jobId.toString(), filesAndFolderMap);
                            endpointsMap.put(endpoint, jobIdHashMap);
                        }
                        jobInterruptionStore.getJobIdsModel().setEndpoints(completeArrayList);
                    }
                } else {
                    final Map<String, Map<String, FilesAndFolderMap>> endpointsHashMap = new HashMap<>();
                    final Map<String, FilesAndFolderMap> jobIdHashMap = new HashMap<>();
                    jobIdHashMap.put(jobId.toString(), filesAndFolderMap);
                    endpointsHashMap.put(endpoint, jobIdHashMap);
                    completeArrayList.add(endpointsHashMap);
                    jobInterruptionStore.getJobIdsModel().setEndpoints(completeArrayList);
                    LOG.info("No entry found");
                }
            try {
                JobInterruptionStore.saveJobInterruptionStore(jobInterruptionStore);
            } catch (final IOException e) {
                LOG.error("Failed to save job ids", e);
            }
        }
    }
}
