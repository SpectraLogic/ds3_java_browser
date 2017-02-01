package com.spectralogic.dsbrowser.gui;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Response;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.RecoverInterruptedJob;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.Ds3GetJob;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.logservice.LogService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.ImageURLs;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.input.DataFormat;
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

public class Main extends Application {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);
    final Alert CLOSECONFIRMATIONALERT = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Are you sure you want to exit?"
    );

    private final Workers workers = new Workers();
    private JobWorkers jobWorkers = null;
    private SavedSessionStore savedSessionStore = null;
    private SavedJobPrioritiesStore savedJobPrioritiesStore = null;
    private SettingsStore settings = null;
    private DataFormat dataFormat = null;
    private ResourceBundle resourceBundle = null;
    private Stage primaryStage = null;
    private JobInterruptionStore jobInterruptionStore = null;


    /*************************/
    private static final String WINDOW_POSITION_X = "Window_Position_X";
    private static final String WINDOW_POSITION_Y = "Window_Position_Y";
    private static final String WINDOW_WIDTH = "Window_Width";
    private static final String WINDOW_HEIGHT = "Window_Height";
    private static final double DEFAULT_X = 10;
    private static final double DEFAULT_Y = 10;
    private static final double DEFAULT_WIDTH = 800;
    private static final double DEFAULT_HEIGHT = 600;
    private static final String NODE_NAME = "ViewSwitcher";
    private static final String BUNDLE = "Bundle";

    /*************************/

    Preferences pref = Preferences.userRoot().node(NODE_NAME);
    double x = pref.getDouble(WINDOW_POSITION_X, DEFAULT_X);
    double y = pref.getDouble(WINDOW_POSITION_Y, DEFAULT_Y);
    double width = pref.getDouble(WINDOW_WIDTH, DEFAULT_WIDTH);
    double height = pref.getDouble(WINDOW_HEIGHT, DEFAULT_HEIGHT);

    private final EventHandler<WindowEvent> confirmCloseEventHandler = event -> {

        //running tasks which are not in cache
        List<Ds3JobTask> notCachedRunningTasks = jobWorkers.getTasks().stream().filter(task -> task.getProgress() != 1).collect(Collectors.toList());
        if (notCachedRunningTasks.isEmpty()) {
            closeApplication(event);
            //event.consume();
        } else {
            final Button exitButton = (Button) CLOSECONFIRMATIONALERT.getDialogPane().lookupButton(
                    ButtonType.OK
            );
            final Button cancelButton = (Button) CLOSECONFIRMATIONALERT.getDialogPane().lookupButton(
                    ButtonType.CANCEL
            );

            exitButton.setText("Exit");
            cancelButton.setText("Cancel");

            if (notCachedRunningTasks.size() == 1) {
                CLOSECONFIRMATIONALERT.setHeaderText(notCachedRunningTasks.size() + " job is still running.");
            } else {
                CLOSECONFIRMATIONALERT.setHeaderText(notCachedRunningTasks.size() + " jobs are still running.");
            }

            final Optional<ButtonType> closeResponse = CLOSECONFIRMATIONALERT.showAndWait();

            if (closeResponse.get().equals(ButtonType.OK)) {
                closeApplication(event);
                /*event.consume();*/
            }

            if (closeResponse.get().equals(ButtonType.CANCEL)) {
                event.consume();
            }
        }
    };

    public static void main(final String[] args) {
        launch(args);
    }


    @Override
    public void start(final Stage primaryStage) throws Exception {

        this.primaryStage = primaryStage;
        this.settings = SettingsStore.loadSettingsStore();
        final Stage stage = (Stage) CLOSECONFIRMATIONALERT.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(ImageURLs.DEEPSTORAGEBROWSER));
        // Create the log service before any logging has started..
        final LogService logService = new LogService(this.settings.getLogSettings());
        this.savedJobPrioritiesStore = SavedJobPrioritiesStore.loadSavedJobPriorties();
        this.jobInterruptionStore = JobInterruptionStore.loadJobIds();
        this.savedSessionStore = SavedSessionStore.loadSavedSessionStore();
        this.dataFormat = new DataFormat("Ds3TreeTableView");
        this.resourceBundle = ResourceBundle.getBundle("lang", new Locale("en"));
        this.jobWorkers = new JobWorkers(settings.getProcessSettings().getMaximumNumberOfParallelThreads());

        final Logger injectorLogger = LoggerFactory.getLogger("Injector");
        LOG.info("Starting Deep Storage Browser v2.0");
        Injector.setLogger(injectorLogger::debug);
        Injector.setModelOrService(LogService.class, logService);
        Injector.setModelOrService(SettingsStore.class, settings);
        Injector.setModelOrService(Workers.class, workers);
        Injector.setModelOrService(JobWorkers.class, jobWorkers);
        Injector.setModelOrService(SavedSessionStore.class, this.savedSessionStore);
        Injector.setModelOrService(SavedJobPrioritiesStore.class, this.savedJobPrioritiesStore);
        Injector.setModelOrService(JobInterruptionStore.class, jobInterruptionStore);
        Injector.setModelOrService(ResourceBundle.class, resourceBundle);
        Injector.setModelOrService(DataFormat.class, dataFormat);
        final DeepStorageBrowserView mainView = new DeepStorageBrowserView();

        final Scene mainScene = new Scene(mainView.getView());
        primaryStage.getIcons().add(new Image(Main.class.getResource("/images/deep_storage_browser.png").toString()));
        primaryStage.setScene(mainScene);
        primaryStage.setX(x);
        primaryStage.setY(y);
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.setTitle(resourceBundle.getString("title"));
        primaryStage.show();
        primaryStage.setOnCloseRequest(confirmCloseEventHandler);

    }



    private void closeApplication(final WindowEvent closeEvent) {
        final Preferences preferences = Preferences.userRoot().node(NODE_NAME);
        preferences.putDouble(WINDOW_POSITION_X, primaryStage.getX());
        preferences.putDouble(WINDOW_POSITION_Y, primaryStage.getY());
        preferences.putDouble(WINDOW_WIDTH, primaryStage.getWidth());
        preferences.putDouble(WINDOW_HEIGHT, primaryStage.getHeight());
        Injector.forgetAll();
        if (savedSessionStore != null) {
            try {
                SavedSessionStore.saveSavedSessionStore(savedSessionStore);
            } catch (final IOException e) {
                LOG.error("Failed to save session information to the local filesystem", e);
            }
        }
        if (savedJobPrioritiesStore != null) {
            try {
                SavedJobPrioritiesStore.saveSavedJobPriorties(savedJobPrioritiesStore);
            } catch (final IOException e) {
                LOG.error("Failed to save job settings information to the local filesystem", e);
            }
        }

        if (jobInterruptionStore != null) {
            try {
                JobInterruptionStore.saveJobInterruptionStore(jobInterruptionStore);
            } catch (final Exception e) {
                LOG.error("Failed to save job ids", e);
            }
        }

        if (settings != null) {
            try {
                SettingsStore.saveSettingsStore(settings);
            } catch (final IOException e) {
                LOG.error("Failed to save settings information to the local filesystem", e);
            }
        }
        if (jobWorkers.getTasks().size() != 0) {
            final ImmutableList<Ds3JobTask> collect = jobWorkers.getTasks().stream().collect(GuavaCollectors.immutableList());
            final Task task = new Task() {
                @Override
                protected Object call() throws Exception {
                    collect.forEach(i -> {
                        try {
                            if (i instanceof Ds3PutJob) {
                                final Ds3PutJob ds3PutJob = (Ds3PutJob) i;
                                ds3PutJob.cancel();
                                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, ds3PutJob.getJobId().toString(), ds3PutJob.getClient().getConnectionDetails().getEndpoint(), null);
                                ds3PutJob.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(ds3PutJob.getJobId()));
                                LOG.info("Cancelled job.");
                            } else if (i instanceof Ds3GetJob) {
                                final Ds3GetJob ds3GetJob = (Ds3GetJob) i;
                                ds3GetJob.cancel();
                                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, ds3GetJob.getJobId().toString(), ds3GetJob.getDs3Client().getConnectionDetails().getEndpoint(), null);
                                ds3GetJob.getDs3Client().cancelJobSpectraS3(new CancelJobSpectraS3Request(ds3GetJob.getJobId()));
                                LOG.info("Cancelled job.");

                            } else if (i instanceof RecoverInterruptedJob) {
                                final RecoverInterruptedJob recoverInterruptedJob = (RecoverInterruptedJob) i;
                                recoverInterruptedJob.cancel();
                                ParseJobInterruptionMap.removeJobID(jobInterruptionStore, recoverInterruptedJob.getUuid().toString(), recoverInterruptedJob.getDs3Client().getConnectionDetails().getEndpoint(), null);
                                recoverInterruptedJob.getDs3Client().cancelJobSpectraS3(new CancelJobSpectraS3Request(recoverInterruptedJob.getUuid()));
                                LOG.info("Cancelled job.");

                            }
                            closeEvent.consume();
                        } catch (final Exception e1) {
                            LOG.error("Failed to cancel job", e1);
                        }
                    });
                    return null;
                }
            };

            workers.execute(task);
            task.setOnSucceeded(event -> {
                workers.shutdown();
                jobWorkers.shutdown();
                jobWorkers.shutdownNow();
                LOG.info("Finished shutting down");
                Platform.exit();
                System.exit(0);
            });
        } else {
            workers.shutdown();
            jobWorkers.shutdown();
            jobWorkers.shutdownNow();
            LOG.info("Finished shutting down");
            Platform.exit();
            System.exit(0);
        }

    }
}
