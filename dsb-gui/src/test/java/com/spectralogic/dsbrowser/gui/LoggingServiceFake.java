package com.spectralogic.dsbrowser.gui;

import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import io.reactivex.Observable;

public class LoggingServiceFake implements LoggingService {
    @Override
    public Observable<LogEvent> getLoggerObservable() {
        return Observable.fromArray();
    }

    @Override
    public void logMessage(final String message, final LogType logType) {
        //Just do nothing
    }
}
