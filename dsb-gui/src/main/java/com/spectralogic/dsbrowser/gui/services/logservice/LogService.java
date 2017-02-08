package com.spectralogic.dsbrowser.gui.services.logservice;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import com.spectralogic.dsbrowser.gui.services.settings.LogSettings;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LogService {

    final org.slf4j.Logger LOG = LoggerFactory.getLogger(LogService.class);

    private LogSettings logSettings;
    private final String pattern = "%date %level [%thread] %logger{10} [%file:%line] %msg%n";

    public LogService(final LogSettings logSettings) {
        this.logSettings = logSettings;
        updateLogBackSettings(pattern);
    }

    public void setLogSettings(final LogSettings logSettings) {
        this.logSettings = logSettings;
        updateLogBackSettings(pattern);
    }

    public PatternLayoutEncoder updateLogBackSettings(final String pattern) {
        final Path destPath = Paths.get(logSettings.getLogLocation(), "browser.log");
        final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final LoggerContext context = rootLogger.getLoggerContext();
        // reset the configuration
        context.reset();
        final RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setFile(destPath.toString());
        final PatternLayoutEncoder layout = new PatternLayoutEncoder();
        layout.setPattern(pattern);
        layout.setContext(context);
        layout.start();
        final FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setContext(context);
        rollingPolicy.setFileNamePattern(destPath.toString() + ".%i");
        rollingPolicy.setMinIndex(1);
        rollingPolicy.setMaxIndex(7);
        rollingPolicy.setMinIndex(logSettings.getNumRollovers());
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.start();
        final SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
        triggeringPolicy.setContext(context);
        triggeringPolicy.setMaxFileSize(FileSize.valueOf(String.format("%dMB", logSettings.getLogSize())));
        triggeringPolicy.start();
        fileAppender.setName("FILE");
        fileAppender.setEncoder(layout);
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.setTriggeringPolicy(triggeringPolicy);
        fileAppender.start();
        rootLogger.addAppender(fileAppender);
        if (logSettings.getConsoleLogging()) {
            final ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
            consoleAppender.setName("STDOUT");
            consoleAppender.setContext(context);
            consoleAppender.setEncoder(layout);
            consoleAppender.start();
        }
        if (logSettings.getDebugLogging()) {
            rootLogger.setLevel(Level.DEBUG);
        } else {
            rootLogger.setLevel(Level.INFO);
        }
        LOG.info("Finished configuring logging");
        return layout;
    }
}
