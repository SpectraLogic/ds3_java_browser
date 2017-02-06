package com.spectralogic.dsbrowser.gui;


import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.junit.Test;

public class Main_Test {

    @Test
    public void main() throws Exception {
    }

    @Test
    public void start() throws Exception {
        final Thread thread = new Thread(() -> {
            new JFXPanel(); // Initializes the JavaFx Platform
            Platform.runLater(() -> {
                try {
                    new Main().start(new Stage());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
        });
        thread.start();// Initialize the thread
        Thread.sleep(20000); // Time to use the app, without this, the thread will be killed too soon
    }
}
