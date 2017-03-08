package com.spectralogic.dsbrowser.gui;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.ShutdownService;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.util.ImageURLs;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.ResourceBundle;

class DeepStorageBrowser {

    private static final Logger LOG = LoggerFactory.getLogger(DeepStorageBrowser.class);

    private final ResourceBundle resourceBundle;
    private final ShutdownService shutdownService;
    private final JobWorkers jobWorkers;

    @Inject
    DeepStorageBrowser(final ResourceBundle resourceBundle, final ShutdownService shutdownService, final JobWorkers jobWorkers) {
        this.resourceBundle = resourceBundle;
        this.shutdownService = shutdownService;
        this.jobWorkers = jobWorkers;
    }

    void start(final Stage primaryStage) {

        LOG.info("Starting Deep Storage Browser v2.0");
        final DeepStorageBrowserView mainView = new DeepStorageBrowserView();

        final Scene mainScene = new Scene(mainView.getView());
        primaryStage.getIcons().add(new Image(Main.class.getResource("/images/deep_storage_browser.png").toString()));
        primaryStage.setScene(mainScene);
        primaryStage.setMaximized(true);
        primaryStage.setTitle(resourceBundle.getString("title"));
        primaryStage.setOnCloseRequest(this::handleWindowClose);
        primaryStage.show();
    }

    private void handleWindowClose(final WindowEvent event) {
        final ImmutableList<Ds3JobTask> notCachedRunningTasks = jobWorkers.getTasks().stream()
                .filter(task -> task.getProgress() != 1).collect(GuavaCollectors.immutableList());

        if (Guard.isNullOrEmpty(jobWorkers.getTasks()) || Guard.isNullOrEmpty(notCachedRunningTasks)) {
            shutdownService.shutdown();
            event.consume();

        } else {

            final Alert closeConfirmation = createCloseConfirmationAlert(notCachedRunningTasks.size());

            final Optional<ButtonType> closeResponse = closeConfirmation.showAndWait();

            closeResponse.ifPresent(response -> {
                if (response.equals(ButtonType.OK)) {
                    shutdownService.shutdown();
                    event.consume();
                } else if (response.equals(ButtonType.CANCEL)) {
                    event.consume();
                }
            });
        }
    }

    private Alert createCloseConfirmationAlert(final int runningTasks) {
        final Alert closeConfirmation = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Are you sure you want to exit?" //TODO put into a resource bundle
        );

        final Stage stage = (Stage) closeConfirmation.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEPSTORAGEBROWSER));

        final Button exitButton = (Button) closeConfirmation.getDialogPane().lookupButton(
                ButtonType.OK
        );
        final Button cancelButton = (Button) closeConfirmation.getDialogPane().lookupButton(
                ButtonType.CANCEL
        );

        exitButton.setText("Exit");
        cancelButton.setText("Cancel");

        if (jobWorkers.getTasks().size() == 1) {
            closeConfirmation.setHeaderText(runningTasks + " job is still running.");
        } else {
            closeConfirmation.setHeaderText(runningTasks + " jobs are still running.");
        }

        return closeConfirmation;
    }
}
