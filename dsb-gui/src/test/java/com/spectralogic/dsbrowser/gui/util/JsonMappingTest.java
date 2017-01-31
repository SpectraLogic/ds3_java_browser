package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by linchpinub4 on 30/1/17.
 */
public class JsonMappingTest {
    @Test
    public void fromJson() throws Exception {
        final Path PATH = Paths.get(System.getProperty("user.home"), ".dsbrowser", "sessions.json");
        final InputStream inputStream = Files.newInputStream(PATH);
        final SavedSessionStore.SerializedSessionStore serializedSessionStore = JsonMapping.fromJson(inputStream, SavedSessionStore.SerializedSessionStore.class);
        final SavedCredentials savedCredentials = new SavedCredentials("c3VsYWJo", "vFSV6Zf5");
        final SavedSession savedSession = new SavedSession("Sulabh", "192.168.11.193","8080", null, savedCredentials);
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
        final SavedCredentials savedCredentials = new SavedCredentials("c3VsYWJo", "vFSV6Zf5");
        final SavedSession savedSession = new SavedSession("Sulabh", "192.168.11.193",
                "8080", null, savedCredentials);
        assertThat(serializedSessionStore.getSessions().get(0).getEndpoint(), is(savedSession.getEndpoint()));
        assertThat(serializedSessionStore.getSessions().get(0).getName(), is(savedSession.getName()));
        assertThat(serializedSessionStore.getSessions().get(0).getCredentials().getAccessId(), is(savedSession.getCredentials().getAccessId()));
        assertThat(serializedSessionStore.getSessions().get(0).getCredentials().getSecretKey(), is(savedSession.getCredentials().getSecretKey()));
        assertThat(serializedSessionStore.getSessions().get(0).getPortNo(), is(savedSession.getPortNo()));
    }

}