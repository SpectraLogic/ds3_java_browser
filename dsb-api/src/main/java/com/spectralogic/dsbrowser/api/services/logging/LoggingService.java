package com.spectralogic.dsbrowser.api.services.logging;

import io.reactivex.Observable;

public interface LoggingService {
    Observable<LogEvent> getLoggerObservable();
    void logMessage(final String message, final LogType logType);
    void logInternationalMessage(final String messageBundleName, final LogType logType);

    class LogEvent {
        private final String message;
        private final LogType logType;

        public LogEvent(final String message, final LogType logType) {
            this.message = message;
            this.logType = logType;
        }

        public LogType getLogType() {
            return logType;
        }

        public String getMessage() {
            return message;
        }
    }
}
