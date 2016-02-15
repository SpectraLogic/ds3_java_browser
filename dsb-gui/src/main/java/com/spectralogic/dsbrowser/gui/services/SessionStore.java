package com.spectralogic.dsbrowser.gui.services;

import java.util.stream.Stream;

/**
 * This class is responsible for storing active sessions between the various components that depend on them.
 */

public interface SessionStore {

    void addSession(final Session session);
    Stream<Session> getSessions();
    void removeSession(final Session session);
    boolean isEmpty();

}
