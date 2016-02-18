package com.spectralogic.dsbrowser.gui;

import com.airhacks.afterburner.injection.Injector;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main extends Application {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    private final Workers workers = new Workers(5);
    private SavedSessionStore savedSessionStore = null;

    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final Logger injectorLogger = LoggerFactory.getLogger("Injector");
        primaryStage.setTitle("Deep Storage Browser v0.0.1");

        this.savedSessionStore = SavedSessionStore.loadSavedSessionStore();

        Injector.setLogger(injectorLogger::debug);
        Injector.setModelOrService(Workers.class, workers);
        Injector.setModelOrService(SavedSessionStore.class, this.savedSessionStore);

        final DeepStorageBrowserView mainView = new DeepStorageBrowserView();

        final Scene mainScene = new Scene(mainView.getView());
        primaryStage.setScene(mainScene);

        primaryStage.show();
    }

    @Override
    public void stop() {
        LOG.info("Starting shutdown process...");
        Injector.forgetAll();
        if (savedSessionStore != null) {
            try {
                SavedSessionStore.saveSavedSessionStore(savedSessionStore);
            } catch (final IOException e) {
                LOG.error("Failed to save session information to the local filesystem", e);
            }
        }
        workers.shutdown();
        LOG.info("Finished shutting down");
    }
}
