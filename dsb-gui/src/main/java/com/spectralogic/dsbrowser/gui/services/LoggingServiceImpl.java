package com.spectralogic.dsbrowser.gui.services;

import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;

import java.util.ArrayList;
import java.util.List;

public class LoggingServiceImpl implements LoggingService {

    private final List<LoggingService.LoggingListener> listeners = new ArrayList<>();

    @Override
    public void registerLogListener(final LoggingService.LoggingListener loggerListener) {
        listeners.add(loggerListener);
    }

    @Override
    public void logMessage(final String message, final LogType logType) {
        listeners.forEach(loggingListener -> loggingListener.log(message, logType));
    }
}
