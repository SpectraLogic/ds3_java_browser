package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CheckNetwork_Test {
    private static Session session;
    private boolean successFlag = false;

    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO, null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);
            session = new NewSessionPresenter().createConnection(savedSession);
        });
    }

    @Test
    public void formatUrlPassThrough() {
        assertThat(CheckNetwork.formatUrl("http://host"), is("http://host"));
    }

    @Test
    public void formatUrlWithHttp() {
        assertThat(CheckNetwork.formatUrl("host"), is("http://host"));
    }

    @Test
    public void formatUrlWithHttps() {
        assertThat(CheckNetwork.formatUrl("https://host"), is("http://host"));
    }

    @Test
    public void isReachable_Test() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                successFlag = CheckNetwork.isReachable(session.getClient());
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}
