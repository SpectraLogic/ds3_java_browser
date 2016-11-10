package com.spectralogic.dsbrowser.gui.services.settings;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spectralogic.dsbrowser.gui.util.JsonMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class SettingsStore {

    private final static Logger LOG = LoggerFactory.getLogger(SettingsStore.class);

    private final static Path PATH = Paths.get(System.getProperty("user.home"), ".dsbrowser", "settings.json");

    @JsonProperty("logSettings")
    private final LogSettings logSettings;

    @JsonProperty("processSettings")
    private final ProcessSettings processSettings;

    private boolean dirty = false;


    @JsonCreator
    public SettingsStore(@JsonProperty("logSettings") final LogSettings logSettings, @JsonProperty("processSettings") final ProcessSettings processSettings) {
        this.logSettings = logSettings;
        this.processSettings = processSettings;
    }

    public static SettingsStore loadSettingsStore() throws IOException {
        // Do not log when loading the settings store since the logger has not been configured
        if (Files.exists(PATH)) {
            try (final InputStream inputStream = Files.newInputStream(PATH)) {
                return JsonMapping.fromJson(inputStream, SettingsStore.class);
            } catch (final Exception e) {
                Files.delete(PATH);
                LOG.info("Creating new empty saved setting store");
                final SettingsStore settingsStore = new SettingsStore(LogSettings.DEFAULT, ProcessSettings.DEFAULT);
                settingsStore.dirty = true; // set this to true so we will write the settings after the first run
                return settingsStore;
            }
        } else {
            final SettingsStore settingsStore = new SettingsStore(LogSettings.DEFAULT, ProcessSettings.DEFAULT);
            settingsStore.dirty = true; // set this to true so we will write the settings after the first run
            return settingsStore;
        }
    }

    public static void saveSettingsStore(final SettingsStore settingsStore) throws IOException {
        if (settingsStore.dirty) {
            LOG.info("Session store was dirty, saving...");
            if (!Files.exists(PATH.getParent())) {
                Files.createDirectories(PATH.getParent());
            }
            try (final OutputStream outputStream = Files.newOutputStream(PATH, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                JsonMapping.toJson(outputStream, settingsStore);
            }
        }
    }

    public LogSettings getLogSettings() {
        return logSettings.copy();
    }

    public void setLogSettings(final LogSettings settings) {
        dirty = true;
        logSettings.overwrite(settings);
    }

    public ProcessSettings getProcessSettings() {
        return processSettings;
    }

    public void setProcessSettings(final ProcessSettings settings) {
        dirty = true;
        processSettings.overwrite(settings);
    }

}
