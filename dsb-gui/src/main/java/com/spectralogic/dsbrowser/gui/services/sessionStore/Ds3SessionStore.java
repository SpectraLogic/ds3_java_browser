package com.spectralogic.dsbrowser.gui.services.sessionStore;

import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Ds3SessionStore implements SessionStore {
    private final ObservableList<Session> sessions = FXCollections.observableArrayList();

    @Override
    public void addSession(final Session session) {
        sessions.add(session);
    }

    @Override
    public Stream<Session> getSessions() {
        return sessions.stream();
    }

    @Override
    public ObservableList<Session> getObservableList() {
        return this.sessions;
    }

    @Override
    public void removeSession(final Session session) {
        sessions.remove(session);
    }

    @Override
    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    @Override
    public int size() {
        return sessions.size();
    }
}
