package com.spectralogic.dsbrowser.gui.util;

import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.Optional;

public class MyTaskProgressViewSkin<T extends Task<?>> extends
        SkinBase<DeepStorageBrowserTaskProgressView<T>> {

    private final Alert CLOSECONFIRMATIONALERT = new Alert(
            Alert.AlertType.CONFIRMATION,
            ""
    );

    public MyTaskProgressViewSkin(final DeepStorageBrowserTaskProgressView<T> monitor) {
        super(monitor);

        final BorderPane borderPane = new BorderPane();
        borderPane.getStyleClass().add("box");

        // list view
        final ListView<T> listView = new ListView<>();
        listView.setPrefSize(500, 400);
        listView.setPlaceholder(new Label("No tasks running"));
        listView.setCellFactory(param -> new TaskCell());
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

        public TaskCell() {
            titleText = new Label();
            titleText.getStyleClass().add("task-title");

            messageText = new Label();
            messageText.getStyleClass().add("task-message");

            progressBar = new ProgressBar();
            progressBar.setMaxWidth(Double.MAX_VALUE);
            progressBar.setMaxHeight(8);
            progressBar.getStyleClass().add("task-progress-bar");

            cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().add("task-cancel-button");
            cancelButton.setTooltip(new Tooltip("Cancel Task"));
            cancelButton.setOnAction(evt -> {
               popupCancelTask(task,evt);
            });

            final VBox vbox = new VBox();
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
            } else if (task != null) {
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

    private void popupCancelTask(final T task, final ActionEvent evt) {
        final Button exitButton = (Button) CLOSECONFIRMATIONALERT.getDialogPane().lookupButton(
                ButtonType.OK
        );
        final Button cancelButton = (Button) CLOSECONFIRMATIONALERT.getDialogPane().lookupButton(
                ButtonType.CANCEL
        );
        exitButton.setText("Yes");
        cancelButton.setText("No! I don't");
        CLOSECONFIRMATIONALERT.setHeaderText("Are you really want to cancel this job");
        CLOSECONFIRMATIONALERT.setContentText("Job will be cancelled. You can not recover this in future.");
        final Optional<ButtonType> closeResponse = CLOSECONFIRMATIONALERT.showAndWait();
        if (closeResponse.get().equals(ButtonType.OK)) {
            if (task != null) {
                task.cancel();
            }
        }else if (closeResponse.get().equals(ButtonType.CANCEL)) {
               evt.consume();
        }
    }

}
