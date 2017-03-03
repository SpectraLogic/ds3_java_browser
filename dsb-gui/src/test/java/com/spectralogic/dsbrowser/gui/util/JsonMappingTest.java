package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class JsonMappingTest {
    @Test
    public void fromJson() throws Exception {
        final Path PATH = Paths.get(System.getProperty("user.home"), ".dsbrowser", "sessions.json");
        final InputStream inputStream = Files.newInputStream(PATH);
        final SavedSessionStore.SerializedSessionStore serializedSessionStore = JsonMapping.fromJson(inputStream, SavedSessionStore.SerializedSessionStore.class);

        final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO, null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);

        assertThat(serializedSessionStore.getSessions().get(0).getEndpoint(), is(savedSession.getEndpoint()));
        assertThat(serializedSessionStore.getSessions().get(0).getName(), is(savedSession.getName()));
        assertThat(serializedSessionStore.getSessions().get(0).getCredentials().getAccessId(), is(savedSession.getCredentials().getAccessId()));
        assertThat(serializedSessionStore.getSessions().get(0).getCredentials().getSecretKey(), is(savedSession.getCredentials().getSecretKey()));
        assertThat(serializedSessionStore.getSessions().get(0).getPortNo(), is(savedSession.getPortNo()));

    }

    @Test
    public void toJson() throws Exception {
        final Path PATH = Paths.get(System.getProperty("user.home"), ".dsbrowser", "sessions.json");
        final InputStream inputStream = Files.newInputStream(PATH);
        final SavedSessionStore.SerializedSessionStore serializedSessionStore = JsonMapping.fromJson(inputStream, SavedSessionStore.SerializedSessionStore.class);
        final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO, null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);

        assertThat(serializedSessionStore.getSessions().get(0).getEndpoint(), is(savedSession.getEndpoint()));
        assertThat(serializedSessionStore.getSessions().get(0).getName(), is(savedSession.getName()));
        assertThat(serializedSessionStore.getSessions().get(0).getCredentials().getAccessId(), is(savedSession.getCredentials().getAccessId()));
        assertThat(serializedSessionStore.getSessions().get(0).getCredentials().getSecretKey(), is(savedSession.getCredentials().getSecretKey()));
        assertThat(serializedSessionStore.getSessions().get(0).getPortNo(), is(savedSession.getPortNo()));
    }

}