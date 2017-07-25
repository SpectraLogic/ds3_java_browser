/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

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

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ApplicationLoggerSettings {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ApplicationLoggerSettings.class);
    private static final String DEFAULT_LOG_SETTINGS = "%date %level [%thread] %logger{10} [%file:%line] %msg%n";
    private LogSettings logSettings;

    @Inject
    public ApplicationLoggerSettings(final LogSettings logSettings) {
        this.logSettings = logSettings;
    }

    public void setLogSettings(final LogSettings logSettings) {
        this.logSettings = logSettings;
        updateLogBackSettings();
    }

    private void updateLogBackSettings() {
        final Path destPath = Paths.get(logSettings.getLogLocation(), "browser.log");
        final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final LoggerContext context = rootLogger.getLoggerContext();

        // reset the configuration
        context.reset();

        final RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setFile(destPath.toString());

        final PatternLayoutEncoder layout = new PatternLayoutEncoder();
        layout.setPattern(DEFAULT_LOG_SETTINGS);
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
    }

    public void restoreLoggingSettings() {
        updateLogBackSettings();
    }
}
