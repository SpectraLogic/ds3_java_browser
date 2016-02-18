package com.spectralogic.dsbrowser.gui.services.sessionStore;

import javafx.collections.ObservableList;

import java.util.stream.Stream;

/**
 * This class is responsible for storing active sessions between the various components that depend on them.
 */

public interface SessionStore {

    void addSession(final Session session);
    Stream<Session> getSessions();
    ObservableList<Session> getObservableList();
    void removeSession(final Session session);
    boolean isEmpty();
    int size();
}
