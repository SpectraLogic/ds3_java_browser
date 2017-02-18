package com.spectralogic.dsbrowser.gui.services.newSessionService;

import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class NewSessionModelValidationTest {
    private boolean successFlag = false;

    @Test
    public void validationNewSession() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final NewSessionModel model = new NewSessionModel();
        model.setSessionName("SessionName");
        model.setProxyServer(null);
        model.setEndpoint("192.168.6.164");
        model.setPortno("8080");
        model.setAccessKey("aaaaa");
        model.setSecretKey("ggggg");
        model.setDefaultSession(true);
        new JFXPanel();
        Platform.runLater(() -> {
            if (!NewSessionModelValidation.validationNewSession(model)) {
                successFlag = true;
                latch.countDown();
            }});
        latch.await();
        assertTrue(successFlag);
    }

}