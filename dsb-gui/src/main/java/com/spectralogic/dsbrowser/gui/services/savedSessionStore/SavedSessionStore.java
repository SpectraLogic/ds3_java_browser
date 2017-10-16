/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.savedSessionStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import com.spectralogic.dsbrowser.api.services.BuildInfoService;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.util.JsonMapping;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SavedSessionStore {
    private final static Logger LOG = LoggerFactory.getLogger(SavedSessionStore.class);
    private final static Path PATH = Paths.get(System.getProperty(StringConstants.SETTING_FILE_PATH), StringConstants.SETTING_FILE_FOLDER_NAME, StringConstants.SESSIONS_STORE);
    private final ObservableList<SavedSession> sessions;
    private final ResourceBundle resourceBundle;
    private final BuildInfoService buildInfoService;
    private boolean dirty = false;

    private SavedSessionStore(final List<SavedSession> sessionList,
                              final ResourceBundle resourceBundle,
                              final BuildInfoService buildInfoService) {
        this.sessions = FXCollections.observableArrayList(sessionList);
        this.sessions.addListener((ListChangeListener<SavedSession>) c -> {
            if (c.next() && (c.wasAdded() || c.wasRemoved())) {
                dirty = true;
            }
        });

        this.resourceBundle = resourceBundle;
        this.buildInfoService = buildInfoService;
    }

    public static SavedSessionStore empty(final ResourceBundle resourceBundle,
                                          final BuildInfoService buildInfoService) {
        return new SavedSessionStore(new ArrayList<>(), resourceBundle, buildInfoService);
    }

    public static SavedSessionStore loadSavedSessionStore(final ResourceBundle resourceBundle,
                                                          final BuildInfoService buildInfoService) throws IOException {
        final List<SavedSession> sessions;
        if (Files.exists(PATH)) {
            try (final InputStream inputStream = Files.newInputStream(PATH)) {
                final SerializedSessionStore store = JsonMapping.fromJson(inputStream, SerializedSessionStore.class);
                sessions = store.getSessions();
            }
        } else {
            LOG.info("Creating new empty saved session store");
            sessions = new ArrayList<>();
        }
        return new SavedSessionStore(sessions, resourceBundle, buildInfoService);
    }

    public static void saveSavedSessionStore(final SavedSessionStore sessionStore) throws IOException {
        if (sessionStore.dirty) {
            LOG.info("Session store was dirty, saving...");
            final SerializedSessionStore store = new SerializedSessionStore(sessionStore.sessions);
            if (!Files.exists(PATH.getParent())) {
                Files.createDirectories(PATH.getParent());
            }
            try (final OutputStream outputStream = Files.newOutputStream(PATH, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                JsonMapping.toJson(outputStream, store);
            }
        }
    }

    public ObservableList<SavedSession> getSessions() {
        return sessions;
    }

    public int addSession(final Session session) {
        final int index;
        if (containsSessionName(sessions, session.getSessionName())) {
            final SavedSession savedSession = sessions.stream().filter(o -> o.getName().equals(session.getSessionName())).findFirst().get();
            if (isNewValuePresent(savedSession, session)) {
                index = sessions.indexOf(savedSession);
                this.sessions.remove(savedSession);
                this.sessions.add(index, new SavedSession(session.getSessionName(), session.getEndpoint(), session.getPortNo(), session.getProxyServer(),
                        SavedCredentials.fromCredentials(session.getClient().getConnectionDetails().getCredentials()), session.getDefaultSession(), session.isUseSSL()));
            } else {
                return -1;
            }
        } else if (!containsSessionName(sessions, session.getSessionName())) {
            this.sessions.add(new SavedSession(session.getSessionName(), session.getEndpoint(), session.getPortNo(), session.getProxyServer(),
                    SavedCredentials.fromCredentials(session.getClient().getConnectionDetails().getCredentials()), session.getDefaultSession(), session.isUseSSL()));
            index = sessions.size() - 1;
        } else {
            index = -2;
        }
        return index;
    }

    private static boolean isNewValuePresent(final SavedSession savedSession, final Session session) {
        if (!savedSession.getName().equals(session.getSessionName()))
            return true;
        if (!savedSession.getCredentials().getAccessId().equals(session.getClient().getConnectionDetails().getCredentials().getClientId()))
            return true;
        if (!savedSession.getCredentials().getSecretKey().equals(session.getClient().getConnectionDetails().getCredentials().getKey()))
            return true;
        if (!savedSession.getEndpoint().equals(session.getEndpoint()))
            return true;
        if (!savedSession.getPortNo().equals(session.getPortNo()))
            return true;
        if (savedSession.getProxyServer() != null) {
            if (!savedSession.getProxyServer().equals(session.getProxyServer())) {
                return true;
            }
        } else if (session.getProxyServer() != null) {
            return true;
        }
        return savedSession.isDefaultSession() != session.getDefaultSession();
    }

    public static boolean containsSessionName(final ObservableList<SavedSession> list, final String name) {
        return list.stream().anyMatch(o -> o.getName().equals(name));
    }

    public static boolean containsNewSessionName(final ObservableList<Session> list, final String name) {
        return list.stream().anyMatch(o -> o.getSessionName().equals(name));
    }

    public void removeSession(final SavedSession sessionName) {
        this.sessions.remove(sessionName);
    }

    //open default session when DSB launched
    public void openDefaultSession(final Ds3SessionStore store) {
        final Optional<SavedSession> savedSessions = getSessions().stream().filter(SavedSession::isDefaultSession).findFirst();
        savedSessions.ifPresent(savedSession ->
                store.addSession(
                        CreateConnectionTask.createConnection(
                                SessionModelService.setSessionModel(savedSession, true), resourceBundle, buildInfoService)));
    }

    public static class SerializedSessionStore {
        @JsonProperty("sessions")
        private final List<SavedSession> sessions;

        @JsonCreator
        private SerializedSessionStore(@JsonProperty("sessions") final List<SavedSession> sessions) {
            this.sessions = sessions;
        }

        public List<SavedSession> getSessions() {
            return sessions;
        }
    }
}
