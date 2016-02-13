package com.spectralogic.dsbrowser.gui;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.collect.ImmutableMap;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        primaryStage.setTitle("Deep Storage Browser v0.0.1");

        final DeepStorageBrowserView mainView = new DeepStorageBrowserView();

        final Scene mainScene = new Scene(mainView.getView());
        primaryStage.setScene(mainScene);

        primaryStage.show();
    }

    @Override
    public void stop() {
        Injector.forgetAll();
    }
}
