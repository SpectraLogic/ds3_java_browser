package com.spectralogic.dsbrowser.gui;

import com.airhacks.afterburner.injection.Injector;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.LoggingServiceImpl;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.logservice.ApplicationLoggerSettings;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.DataFormat;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

public class Main extends Application {
    private final static Logger LOG = LoggerFactory.getLogger(Main.class);
    private final Workers workers = new Workers();

    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        PrimaryStageModel.getInstance().setPrimaryStage(primaryStage);
        primaryStage.setMinHeight(Constants.MIN_HEIGHT);
        primaryStage.setMinWidth(Constants.MIN_WIDTH);
        final SettingsStore settings = SettingsStore.loadSettingsStore();
        // Create the log service before any logging has started..
        final ApplicationLoggerSettings applicationLoggerSettings = new ApplicationLoggerSettings(settings.getLogSettings());
        applicationLoggerSettings.restoreLoggingSettings();
        final SavedJobPrioritiesStore savedJobPrioritiesStore = SavedJobPrioritiesStore.loadSavedJobPriorties();
        final JobInterruptionStore jobInterruptionStore = JobInterruptionStore.loadJobIds();
        final SavedSessionStore savedSessionStore = SavedSessionStore.loadSavedSessionStore();
        final ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
        final DataFormat dataFormat = new DataFormat(resourceBundle.getString("dataFormat"));
        final JobWorkers jobWorkers = new JobWorkers(settings.getProcessSettings().getMaximumNumberOfParallelThreads());
        final Logger injectorLogger = LoggerFactory.getLogger("Injector");
        final LoggingServiceImpl loggingService = new LoggingServiceImpl();
        LOG.info("Starting Deep Storage Browser v2.1");
        Injector.setLogger(injectorLogger::debug);
        Injector.setModelOrService(ApplicationLoggerSettings.class, applicationLoggerSettings);
        Injector.setModelOrService(LoggingService.class, loggingService);
        Injector.setModelOrService(SettingsStore.class, settings);
        Injector.setModelOrService(Workers.class, workers);
        Injector.setModelOrService(JobWorkers.class, jobWorkers);
        Injector.setModelOrService(SavedSessionStore.class, savedSessionStore);
        Injector.setModelOrService(SavedJobPrioritiesStore.class, savedJobPrioritiesStore);
        Injector.setModelOrService(JobInterruptionStore.class, jobInterruptionStore);
        Injector.setModelOrService(ResourceBundle.class, resourceBundle);
        Injector.setModelOrService(DataFormat.class, dataFormat);

        final DeepStorageBrowserView mainView = new DeepStorageBrowserView();
        final Scene mainScene = new Scene(mainView.getView());
        primaryStage.getIcons().add(new Image(ImageURLs.DEEP_STORAGE_BROWSER));
        primaryStage.setScene(mainScene);

        if (ApplicationPreferences.getInstance().isWindowMaximized()) {
            primaryStage.setMaximized(true);
        } else {
            primaryStage.setX(ApplicationPreferences.getInstance().getX());
            primaryStage.setY(ApplicationPreferences.getInstance().getY());
            primaryStage.setWidth(ApplicationPreferences.getInstance().getWidth());
            primaryStage.setHeight(ApplicationPreferences.getInstance().getHeight());
        }
        primaryStage.setTitle(resourceBundle.getString("title"));
        primaryStage.show();
        final CloseConfirmationHandler closeConfirmationHandler = new CloseConfirmationHandler(primaryStage, savedSessionStore, savedJobPrioritiesStore, jobInterruptionStore, settings, jobWorkers, workers, loggingService);
        primaryStage.setOnCloseRequest(closeConfirmationHandler.confirmCloseEventHandler);
    }
}
