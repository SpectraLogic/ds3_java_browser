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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.util.Callback;

public class DeepStorageBrowserTaskProgressView<T extends Task<?>> extends DeepStorageControlsFXControl {

    /**
     * Constructs a new task progress view.
     */
    public DeepStorageBrowserTaskProgressView() {
        getStyleClass().add("task-progress-view");

        final EventHandler<WorkerStateEvent> taskHandler = evt -> {
            if (evt.getEventType().equals(
                    WorkerStateEvent.WORKER_STATE_SUCCEEDED)
                    || evt.getEventType().equals(
                    WorkerStateEvent.WORKER_STATE_CANCELLED)
                    || evt.getEventType().equals(
                    WorkerStateEvent.WORKER_STATE_FAILED)) {
                getTasks().remove(evt.getSource());
            }
        };

        getTasks().addListener(new ListChangeListener(taskHandler));
    }

    /** {@inheritDoc} */
    @Override public String getUserAgentStylesheet() {
        return getUserAgentStylesheet(DeepStorageBrowserTaskProgressView.class, "mytaskprogressview.css");
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new DeepStorageTaskProgressViewSkin<>(this);
    }

    private final ObservableList<T> tasks = FXCollections
            .observableArrayList();

    /**
     * Returns the list of tasks currently monitored by this view.
     *
     * @return the monitored tasks
     */
    public final ObservableList<T> getTasks() {
        return tasks;
    }

    private ObjectProperty<Callback<T, Node>> graphicFactory;

    /**
     * Returns the property used to store an optional callback for creating
     * custom graphics for each task.
     *
     * @return the graphic factory property
     */
    public final ObjectProperty<Callback<T, Node>> graphicFactoryProperty() {
        if (graphicFactory == null) {
            graphicFactory = new SimpleObjectProperty<Callback<T, Node>>(
                    this, "graphicFactory");
        }

        return graphicFactory;
    }

    /**
     * Returns the value of {@link #graphicFactoryProperty()}.
     *
     * @return the optional graphic factory
     */
    public final Callback<T, Node> getGraphicFactory() {
        return graphicFactory == null ? null : graphicFactory.get();
    }

    /**
     * Sets the value of {@link #graphicFactoryProperty()}.
     *
     * @param factory an optional graphic factory
     */
    public final void setGraphicFactory(final Callback<T, Node> factory) {
        graphicFactoryProperty().set(factory);
    }

    private static class ListChangeListener implements javafx.collections.ListChangeListener<Task<?>> {
        private final EventHandler<WorkerStateEvent> taskHandler;

        public ListChangeListener(final EventHandler<WorkerStateEvent> taskHandler) {
            this.taskHandler = taskHandler;
        }

        @Override
        public void onChanged(final Change<? extends Task<?>> c) {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (final Task<?> task : c.getAddedSubList()) {
                        task.addEventHandler(WorkerStateEvent.ANY,
                                taskHandler);
                    }
                } else if (c.wasRemoved()) {
                    for (final Task<?> task : c.getRemoved()) {
                        task.removeEventHandler(WorkerStateEvent.ANY,
                                taskHandler);
                    }
                }
            }
        }
    }
}
