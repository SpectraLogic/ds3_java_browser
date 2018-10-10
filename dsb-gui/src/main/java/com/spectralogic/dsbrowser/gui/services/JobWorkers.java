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

package com.spectralogic.dsbrowser.gui.services;

import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class JobWorkers {
    private final static Logger LOG = LoggerFactory.getLogger(JobWorkers.class);

    private ExecutorService workers;
    private final ObservableList<Ds3JobTask> tasks;

    @Inject
    public JobWorkers() {
        workers = Executors.newCachedThreadPool();
        this.tasks = FXCollections.observableArrayList();
        this.tasks.addListener((ListChangeListener<Ds3JobTask>) c -> {
            if (c.next()) {
                c.getAddedSubList().forEach(workers::execute);
            }
        });
    }

    public void setWorkers(final ExecutorService workers) {
        this.workers = workers;
    }

    public ExecutorService getWorkers() {
        return workers;
    }

    public void execute(final Ds3JobTask run) {
        final EventHandler<WorkerStateEvent> onCancelled = run.getOnCancelled();
        run.setOnCancelled(SafeHandler.logHandle(event -> {
            onCancelled.handle(event);
            handleStop(event);
        }));
        final EventHandler<WorkerStateEvent> onFailed = run.getOnFailed();
        run.setOnFailed(SafeHandler.logHandle(event -> {
            onFailed.handle(event);
            handleStop(event);
        }));
        final EventHandler<WorkerStateEvent> onSucceeded = run.getOnSucceeded();
        run.setOnSucceeded(SafeHandler.logHandle(event -> {
            onSucceeded.handle(event);
            handleStop(event);
        }));
        LOG.info("Adding to task list");
        tasks.add(0,run);
    }

    public ObservableList<Ds3JobTask> getTasks() {
        return tasks;
    }

    private void handleStop(final WorkerStateEvent event) {
        if (event.getSource() instanceof Ds3JobTask) {
            final Ds3JobTask task = (Ds3JobTask) event.getSource();
            this.tasks.remove(task);
        } else {
            LOG.info("Unknown worker");
        }
    }

    public void shutdown() {
        workers.shutdown();
    }

    public void shutdownNow() {
        workers.shutdownNow();
    }
}
