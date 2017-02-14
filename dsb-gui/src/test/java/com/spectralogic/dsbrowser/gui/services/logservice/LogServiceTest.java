package com.spectralogic.dsbrowser.gui.services.logservice;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.spectralogic.dsbrowser.gui.services.settings.LogSettings;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class LogServiceTest {

    @Test
    public void updateLogBackSettings() throws Exception {
        final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
        final LogSettings logSettings = settingsStore.getLogSettings();
        final LogService logService = new LogService(logSettings);
        final String pattern = "[%thread] %logger{10} [%file:%line] %msg%n";
        final PatternLayoutEncoder layout = logService.updateLogBackSettings(pattern);
        assertEquals("[%thread] %logger{10} [%file:%line] %msg%n", layout.getPattern());
    }

}