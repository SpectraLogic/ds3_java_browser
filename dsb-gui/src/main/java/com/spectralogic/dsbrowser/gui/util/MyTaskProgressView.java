package com.spectralogic.dsbrowser.gui.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.util.Callback;

public class MyTaskProgressView<T extends Task<?>> extends MyControlsFXControl {

    /**
     * Constructs a new task progress view.
     */
    public MyTaskProgressView() {
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

        getTasks().addListener(new ListChangeListener<Task<?>>() {
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
        });
    }

    /** {@inheritDoc} */
    @Override public String getUserAgentStylesheet() {
        return getUserAgentStylesheet(MyTaskProgressView.class, "mytaskprogressview.css");
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new MyTaskProgressViewSkin<>(this);
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
}
