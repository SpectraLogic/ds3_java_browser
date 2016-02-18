package com.spectralogic.dsbrowser.gui.services.savedSessionStore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class SavedSessionStore {
    private final static Logger LOG = LoggerFactory.getLogger(SavedSessionStore.class);
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static Path PATH = Paths.get(System.getProperty("user.home"), ".dsbrowser", "sessions.json");

    private final Map<String, SavedSession> sessions;

    public static SavedSessionStore loadSavedSessionStore() throws IOException {
        final Map<String, SavedSession> sessions;
        if (Files.exists(PATH)) {
            try (final InputStream inputStream = Files.newInputStream(PATH)) {
                final SerializedSessionStore store = MAPPER.readValue(inputStream, SerializedSessionStore.class);
                sessions = store.getSessions();
            }
        } else {
            LOG.info("Creating new empty saved session store");
            sessions = new HashMap<>();
        }
        return new SavedSessionStore(sessions);
    }

    public static void saveSavedSessionStore(final SavedSessionStore sessionStore) throws IOException {
        final SerializedSessionStore store = new SerializedSessionStore(sessionStore.sessions);
        try (final OutputStream outputStream = Files.newOutputStream(PATH, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            MAPPER.writeValue(outputStream, store);
        }
    }

    private SavedSessionStore(final Map<String, SavedSession> savedSessions) {
        this.sessions = savedSessions;
    }

    public Stream<SavedSession> getSessions() {
        return this.sessions.values().stream();
    }

    public void saveSession(final Session session) {
        this.sessions.put(session.getSessionName(),
                new SavedSession(session.getSessionName(),
                        session.getEndpoint(),
                        session.getClient().getConnectionDetails().getCredentials()));
    }

    public void removeSession(final String sessionName) {
        this.sessions.remove(sessionName);
    }

    private static class SerializedSessionStore {
        @JsonProperty("sessions")
        private final Map<String, SavedSession> sessions;

        private SerializedSessionStore(final Map<String, SavedSession> sessions) {
            this.sessions = sessions;
        }

        public Map<String, SavedSession> getSessions() {
            return sessions;
        }
    }
}
