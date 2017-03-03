package com.spectralogic.dsbrowser.gui.services.newSessionService;

import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
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
        model.setSessionName(SessionConstants.SESSION_NAME);
        model.setProxyServer(null);
        model.setEndpoint(SessionConstants.SESSION_PATH);
        model.setPortno(SessionConstants.PORT_NO);
        model.setAccessKey(SessionConstants.ACCESS_ID);
        model.setSecretKey(SessionConstants.SECRET_KEY);
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