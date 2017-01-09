package com.spectralogic.dsbrowser.gui.services.settings;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spectralogic.dsbrowser.gui.util.JsonMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

public class SettingsStore {

    private final static Logger LOG = LoggerFactory.getLogger(SettingsStore.class);

    private final static Path PATH = Paths.get(System.getProperty("user.home"), ".dsbrowser", "settings.json");

    @JsonProperty("logSettings")
    private final LogSettings logSettings;

    @JsonProperty("processSettings")
    private final ProcessSettings processSettings;

    @JsonProperty("filePropertiesSettings")
    private final FilePropertiesSettings filePropertiesSettings;

    private boolean dirty = false;


    @JsonCreator
    public SettingsStore(@JsonProperty("logSettings") final LogSettings logSettings, @JsonProperty("processSettings") final ProcessSettings processSettings, @JsonProperty("filePropertiesSettings") final FilePropertiesSettings filePropertiesSettings) {
        this.logSettings = logSettings;
        this.processSettings = processSettings;
        this.filePropertiesSettings = filePropertiesSettings;
    }

    public static SettingsStore loadSettingsStore() throws IOException {
        // Do not log when loading the settings store since the logger has not been configured

        if (Files.exists(PATH)) {
            writeNewSettingsToSettingsJsonFile();
            try (final InputStream inputStream = Files.newInputStream(PATH)) {
                return JsonMapping.fromJson(inputStream, SettingsStore.class);
            } catch (final Exception e) {
                e.printStackTrace();
                Files.delete(PATH);
                LOG.info("Creating new empty saved setting store");
                final SettingsStore settingsStore = new SettingsStore(LogSettings.DEFAULT, ProcessSettings.DEFAULT, FilePropertiesSettings.DEFAULT);
                settingsStore.dirty = true; // set this to true so we will write the settings after the first run
                return settingsStore;
            }
        } else {
            final SettingsStore settingsStore = new SettingsStore(LogSettings.DEFAULT, ProcessSettings.DEFAULT, FilePropertiesSettings.DEFAULT);
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

    public FilePropertiesSettings getFilePropertiesSettings() {
        return filePropertiesSettings;
    }

    public void setFilePropertiesSettings(final boolean settings) {
        dirty = true;
        filePropertiesSettings.overwrite(settings);
    }

    //method to include new json entry to settings.json file if any new setting property is introduced.
    public static void writeNewSettingsToSettingsJsonFile() {
        try (final Stream<String> stream = Files.lines(PATH)) {
            stream.forEach((e) -> {
                if (!e.contains("filePropertiesSettings")) {
                    StringBuilder newFile = new StringBuilder(e);
                    newFile.deleteCharAt(newFile.length() - 1);
                    newFile = newFile.append(",\"filePropertiesSettings\":{\"filePropertiesEnable\":true}}");//file property is true by default..adding this new setting to the file
                    try (final BufferedWriter writer = Files.newBufferedWriter(PATH)) {
                        writer.write(newFile.toString());
                    } catch (final IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }


}
