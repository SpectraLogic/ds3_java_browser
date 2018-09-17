/*
 * Copyright (c) 2013, 2014, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.services.jobService.JobTask;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.controlsfx.control.TaskProgressView;

import java.util.Optional;
import java.util.ResourceBundle;

public class DeepStorageTaskProgressViewSkin<T extends Task<?>> extends
        SkinBase<TaskProgressView<T>> {

    private final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();
    private final Ds3Alert ds3Alert;

    public DeepStorageTaskProgressViewSkin(final TaskProgressView<T> monitor, final BooleanProperty showCache, final Ds3Alert ds3Alert) {
        super(monitor);

        this.ds3Alert = ds3Alert;

        final BorderPane borderPane = new BorderPane();
        borderPane.getStyleClass().add("box");

        // list view
        final ListView<T> listView = new ListView<>();
        listView.setPrefSize(500, 400);
        listView.setPlaceholder(new Label(resourceBundle.getString("noTaskRunning")));
        listView.setCellFactory(param -> new TaskCell(listView, showCache));
        listView.setFocusTraversable(false);

        Bindings.bindContent(listView.getItems(), monitor.getTasks());
        borderPane.setCenter(listView);

        getChildren().add(listView);
    }

    class TaskCell extends ListCell<T> {
        private final ProgressBar progressBar;
        private final Label titleText;
        private final Label messageText;
        private final Button cancelButton;

        private T task;
        private final BorderPane borderPane;

        public TaskCell(final ListView<T> listView, final BooleanProperty showCache) {
            final VBox vbox = new VBox();

            titleText = new Label();
            titleText.getStyleClass().add("task-title");

            messageText = new Label();
            messageText.getStyleClass().add("task-message");
            messageText.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);

            progressBar = new ProgressBar();
            progressBar.setMaxWidth(Double.MAX_VALUE);
            progressBar.setMaxHeight(8);
            progressBar.getStyleClass().add("task-progress-bar");

            cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().add("task-cancel-button");
            cancelButton.setTooltip(new Tooltip(resourceBundle.getString("cancelTask")));
            cancelButton.setOnAction(evt -> {
                popupCancelTask(task, evt);
            });

            showCache.addListener((observable, oldValue, newValue) -> {
                hideTask(task, this);
            });

            vbox.setSpacing(4);
            vbox.getChildren().add(titleText);
            vbox.getChildren().add(progressBar);
            vbox.getChildren().add(messageText);

            BorderPane.setAlignment(cancelButton, Pos.CENTER);
            BorderPane.setMargin(cancelButton, new Insets(0, 0, 0, 4));

            borderPane = new BorderPane();
            borderPane.setCenter(vbox);
            borderPane.setRight(cancelButton);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            prefWidthProperty().bind(listView.widthProperty().subtract(4));
            setMaxWidth(Control.USE_COMPUTED_SIZE);
        }

        @Override
        public void updateIndex(final int index) {
            super.updateIndex(index);

            /*
             * I have no idea why this is necessary but it won't work without
             * it. Shouldn't the updateItem method be enough?
             */
            if (index == -1) {
                setGraphic(null);
                getStyleClass().setAll("task-list-cell-empty");
            }
        }

        @Override
        protected void updateItem(final T task, final boolean empty) {
            super.updateItem(task, empty);

            this.task = task;

            if (empty || task == null) {
                getStyleClass().setAll("task-list-cell-empty");
                setGraphic(null);
            } else {
                getStyleClass().setAll("task-list-cell");
                progressBar.progressProperty().bind(task.progressProperty());
                titleText.textProperty().bind(task.titleProperty());
                messageText.textProperty().bind(task.messageProperty());
                cancelButton.disableProperty().bind(
                        Bindings.not(task.runningProperty()));

                final Callback<T, Node> factory = getSkinnable().getGraphicFactory();
                if (factory != null) {
                    final Node graphic = factory.call(task);
                    if (graphic != null) {
                        BorderPane.setAlignment(graphic, Pos.CENTER);
                        BorderPane.setMargin(graphic, new Insets(0, 4, 0, 0));
                        borderPane.setLeft(graphic);
                    }
                } else {
                    /*
                     * Really needed. The application might have used a graphic
                     * factory before and then disabled it. In this case the border
                     * pane might still have an old graphic in the left position.
                     */
                    borderPane.setLeft(null);
                }

                setGraphic(borderPane);
            }
        }
    }

    private void hideTask(final T task, final TaskCell tc) {
        if (task instanceof JobTask && task != null) {
            tc.visibleProperty().set(((JobTask) task).isVisible().getValue());
        }
    }

    private void popupCancelTask(final T task, final ActionEvent evt) {
        final Optional<ButtonType> closeResponse = ds3Alert.showConfirmationAlert(resourceBundle.getString("confirmation"), resourceBundle.getString("aJobWillBeCancelled"), Alert.AlertType.CONFIRMATION, resourceBundle.getString("reallyWantToCancelSingleJob"), resourceBundle.getString("exitBtnJobCancelConfirm"), resourceBundle.getString("cancelBtnJobCancelConfirm"));
        closeResponse.ifPresent(buttonType -> {
            if(buttonType.equals(ButtonType.OK)) {
                if (task != null) {
                    task.cancel();
                } else if (buttonType.equals(ButtonType.CANCEL)) {
                    evt.consume();
                }
            }
        });
    }

}
