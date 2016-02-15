package com.spectralogic.dsbrowser.gui.services;

import java.util.List;

/**
 * This class is responsible for storing active sessions between the various components that depend on them.
 */

public interface SessionStore {

    void addSession(final Session session);
    List<Session> getSessions();
    void removeSession(final Session session);

}
