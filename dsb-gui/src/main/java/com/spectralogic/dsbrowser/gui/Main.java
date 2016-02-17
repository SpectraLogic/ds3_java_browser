package com.spectralogic.dsbrowser.gui;

import com.airhacks.afterburner.injection.Injector;
import com.spectralogic.dsbrowser.gui.services.Workers;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {

    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    private final Workers workers = new Workers(5);

    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final Logger injectorLogger = LoggerFactory.getLogger("Injector");
        primaryStage.setTitle("Deep Storage Browser v0.0.1");

        Injector.setLogger(injectorLogger::debug);
        Injector.setModelOrService(Workers.class, workers);

        final DeepStorageBrowserView mainView = new DeepStorageBrowserView();

        final Scene mainScene = new Scene(mainView.getView());
        primaryStage.setScene(mainScene);

        primaryStage.show();
    }

    @Override
    public void stop() {
        LOG.info("Starting shutdown process...");
        Injector.forgetAll();
        workers.shutdown();
        LOG.info("Finished shutting down");
    }
}
