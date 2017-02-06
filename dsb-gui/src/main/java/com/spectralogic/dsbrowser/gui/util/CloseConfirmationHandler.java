package com.spectralogic.dsbrowser.gui.util;

import com.airhacks.afterburner.injection.Injector;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static com.spectralogic.dsbrowser.gui.util.StringConstants.*;

public class CloseConfirmationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CloseConfirmationHandler.class);

    private final Stage primaryStage;
    private final SavedSessionStore savedSessionStore;
    private final SavedJobPrioritiesStore savedJobPrioritiesStore;
    private final JobInterruptionStore jobInterruptionStore;
    private final SettingsStore settings;
    private final Workers workers;
    private final ResourceBundle resourceBundle;
    private final Alert closeConfirmationAlert;
    private final JobWorkers jobWorkers;

    //running tasks which are not in cache
    public final EventHandler<WindowEvent> confirmCloseEventHandler = this::closeConfirmationAlert;

    public CloseConfirmationHandler(final Stage primaryStage, final SavedSessionStore savedSessionStore,
                                    final SavedJobPrioritiesStore savedJobPrioritiesStore, final JobInterruptionStore jobInterruptionStore, final SettingsStore settings, final JobWorkers jobWorkers, final Workers workers) {
        this.primaryStage = primaryStage;
        this.savedSessionStore = savedSessionStore;
        this.savedJobPrioritiesStore = savedJobPrioritiesStore;
        this.jobInterruptionStore = jobInterruptionStore;
        this.settings = settings;
        this.jobWorkers = jobWorkers;
        this.workers = workers;
        this.resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
        this.closeConfirmationAlert = new Alert(
                Alert.AlertType.CONFIRMATION,
                resourceBundle.getString("closeConfirmationAlertMessage")
        );
        final Stage stage = (Stage) closeConfirmationAlert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEPSTORAGEBROWSER));
    }

    /**
     * Showing alert for exiting the application
     *
     * @param event event
     */
    public void closeConfirmationAlert(final Event event) {
        LOG.info("Initiating close event");
        if (jobWorkers != null && !Guard.isNullOrEmpty(jobWorkers.getTasks())) {
            final List<Ds3JobTask> notCachedRunningTasks = jobWorkers.getTasks().stream().filter(task -> task.getProgress() != 1).collect(Collectors.toList());
            if (Guard.isNullOrEmpty(notCachedRunningTasks)) {
                closeApplication(event);
            } else {
                final Button exitButton = (Button) closeConfirmationAlert.getDialogPane().lookupButton(
                        ButtonType.OK
                );
                final Button cancelButton = (Button) closeConfirmationAlert.getDialogPane().lookupButton(
                        ButtonType.CANCEL
                );
                exitButton.setText(resourceBundle.getString("exitButtonText"));
                cancelButton.setText(resourceBundle.getString("cancelButtonText"));
                if (1 == notCachedRunningTasks.size()) {
                    closeConfirmationAlert.setHeaderText(notCachedRunningTasks.size() + StringConstants.SPACE + resourceBundle.getString("jobStillRunningMessage"));
                } else {
                    closeConfirmationAlert.setHeaderText(notCachedRunningTasks.size() + StringConstants.SPACE + resourceBundle.getString("multipleJobStillRunningMessage"));
                }
                final Optional<ButtonType> closeResponse = closeConfirmationAlert.showAndWait();
                if (closeResponse.get().equals(ButtonType.OK)) {
                    closeApplication(event);
                }
                if (closeResponse.get().equals(ButtonType.CANCEL)) {
                    event.consume();
                }
            }
        } else {
            closeApplication(event);
        }
    }

    /**
     * method of closing the application and canceling all running tasks
     *
     * @param closeEvent close event
     */
    private void closeApplication(final Event closeEvent) {
        LOG.info("Closing the application and canceling all running tasks");
        setPreferences(primaryStage.getX(), primaryStage.getY(), primaryStage.getWidth(), primaryStage.getHeight());
        Injector.forgetAll();
        saveSessionStore(savedSessionStore);
        saveJobPriorities(savedJobPrioritiesStore);
        saveInterruptionJobs(jobInterruptionStore);
        saveSettings(settings);
        if (!Guard.isNullOrEmpty(jobWorkers.getTasks())) {
            final Task cancelRunningJobsTask = cancelAllRunningTasks(jobWorkers, workers, jobInterruptionStore);
            cancelRunningJobsTask.setOnSucceeded(event -> {
                closeEvent.consume();
                shutdownWorkers();
                Platform.exit();
                System.exit(0);
            });
        } else {
            shutdownWorkers();
            Platform.exit();
            System.exit(0);
        }
    }

    /**
     * shut down all working threads
     */
    public void shutdownWorkers() {
        LOG.info("Shutting down all working threads");
        workers.shutdown();
        jobWorkers.shutdown();
        jobWorkers.shutdownNow();
        LOG.info("Finished shutting down");
    }

    /**
     * To cancel all running jobs
     *
     * @param jobWorkers           jobWorker object
     * @param workers              worker object
     * @param jobInterruptionStore jobInterruptionStore Object
     * @return task
     */
    public Task cancelAllRunningTasks(final JobWorkers jobWorkers, final Workers workers, final JobInterruptionStore jobInterruptionStore) {
        LOG.info("Cancelling all running jobs");
        final Task cancelRunningJobsTask = new CancelRunningJobsTask(jobWorkers, jobInterruptionStore);
        workers.execute(cancelRunningJobsTask);
        return cancelRunningJobsTask;
    }

    /**
     * set preferences for window resize
     *
     * @param x      x
     * @param y      y
     * @param width  width
     * @param height height
     */
    public void setPreferences(final double x, final double y, final double width, final double height) {
        LOG.info("Setting up windows preferences");
        final Preferences preferences = Preferences.userRoot().node(NODE_NAME);
        preferences.putDouble(WINDOW_POSITION_X, x);
        preferences.putDouble(WINDOW_POSITION_Y, y);
        preferences.putDouble(WINDOW_WIDTH, width);
        preferences.putDouble(WINDOW_HEIGHT, height);
    }

    /**
     * save session into local file system
     *
     * @param savedSessionStore savedSessionStore
     */
    public void saveSessionStore(final SavedSessionStore savedSessionStore) {
        LOG.info("Saving session into local file system");
        if (savedSessionStore != null) {
            try {
                SavedSessionStore.saveSavedSessionStore(savedSessionStore);
            } catch (final IOException e) {
                LOG.error("Failed to save session information to the local filesystem", e);
            } catch (final Exception ex) {
                LOG.error("General Exception while saving session information to the local filesystem", ex);
            }
        }
    }

    /**
     * save job settings to the local file system
     *
     * @param savedJobPrioritiesStore savedJobPrioritiesStore
     */
    public void saveJobPriorities(final SavedJobPrioritiesStore savedJobPrioritiesStore) {
        LOG.info("Saving job settings to the local file system");
        if (savedJobPrioritiesStore != null) {
            try {
                SavedJobPrioritiesStore.saveSavedJobPriorties(savedJobPrioritiesStore);
            } catch (final IOException e) {
                LOG.error("Failed to save job settings information to the local filesystem", e);
            } catch (final Exception ex) {
                LOG.error("General Exception while saving job settings information to the local filesystem", ex);
            }
        }
    }

    /**
     * save Interrupted jobs to the local file system
     *
     * @param jobInterruptionStore jobInterruptionStore
     */
    public void saveInterruptionJobs(final JobInterruptionStore jobInterruptionStore) {
        LOG.info("Saving interrupted jobs to the local file system");
        if (jobInterruptionStore != null) {
            try {
                JobInterruptionStore.saveJobInterruptionStore(jobInterruptionStore);
            } catch (final IOException e) {
                LOG.error("Failed to save job ids", e);
            } catch (final Exception ex) {
                LOG.error("General Exception while saving job Ids to the local filesystem", ex);
            }
        }
    }

    /**
     * To save user setting to local system
     *
     * @param settings settings
     */
    public void saveSettings(final SettingsStore settings) {
        LOG.info("Saving user setting to local system");
        if (settings != null) {
            try {
                SettingsStore.saveSettingsStore(settings);
            } catch (final IOException e) {
                LOG.error("Failed to save settings information to the local filesystem", e);
            } catch (final Exception ex) {
                LOG.error("General Exception while saving settings information to the local filesystem", ex);
            }
        }
    }
}
