package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientImpl;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CheckNetwork_Test {
    private static Session session;

    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession("Test1", "192.168.6.164", "8080", null, new SavedCredentials("c3BlY3RyYQ==", "JNXaNmyt"), false);
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
        new JFXPanel();
        Platform.runLater(() -> {
            assertTrue(CheckNetwork.isReachable(session.getClient()));
        });
        Thread.sleep(100);
    }
}
