package com.spectralogic.dsbrowser.api.services.logging;

public interface LoggingService {
    void registerLogListener(final LoggingListener loggerListener);
    void logMessage(final String message, LogType logType);

    @FunctionalInterface
    interface LoggingListener {
        void log(final String message, final LogType logType);
    }
}
