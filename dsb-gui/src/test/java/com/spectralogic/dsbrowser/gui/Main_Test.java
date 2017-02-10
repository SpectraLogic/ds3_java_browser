package com.spectralogic.dsbrowser.gui;


import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main_Test {

    @Test
    public void main() throws Exception {
    }

    @Test
    public void start() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
            new JFXPanel(); // Initializes the JavaFx Platform

            Platform.runLater(() -> {
                try {
                    new Main().start(new Stage());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
       // Initialize the thread
        latch.await(30, TimeUnit.SECONDS) ;// Time to use the app, without this, the thread will be killed too soon
    }
}
