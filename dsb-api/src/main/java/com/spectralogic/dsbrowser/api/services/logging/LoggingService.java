package com.spectralogic.dsbrowser.api.services.logging;

import io.reactivex.Observable;
import org.slf4j.Logger;

public interface LoggingService {
    Observable<LogEvent> getLoggerObservable();
    void logMessage(final String message, final LogType logType);

    void showAndLogErrror(final String message, final Logger log, final Exception e);

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
