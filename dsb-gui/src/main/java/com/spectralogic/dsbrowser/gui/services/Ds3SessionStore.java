package com.spectralogic.dsbrowser.gui.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Ds3SessionStore implements SessionStore {

    private final List<Session> sessions = new ArrayList<>();

    @Override
    public void addSession(final Session session) {
        sessions.add(session);
    }

    @Override
    public Stream<Session> getSessions() {
        return sessions.stream();
    }

    @Override
    public void removeSession(final Session session) {
        sessions.remove(session);
    }

    @Override
    public boolean isEmpty() {
        return sessions.isEmpty();
    }
}
