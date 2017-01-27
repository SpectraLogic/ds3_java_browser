package com.spectralogic.dsbrowser.gui.util;

public final class CurrentSession {

    private static String currentSession = null;

    public static String getCurrentSession() {
        return currentSession;
    }

    public static void setCurrentSession(final String currentSession) {
        CurrentSession.currentSession = currentSession;
    }
}
