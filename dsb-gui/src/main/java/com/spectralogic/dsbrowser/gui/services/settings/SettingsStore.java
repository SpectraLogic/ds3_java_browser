package com.spectralogic.dsbrowser.gui.services.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spectralogic.dsbrowser.gui.util.JsonMapping;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

public class SettingsStore {

    private final static Logger LOG = LoggerFactory.getLogger(SettingsStore.class);

    private final static Path PATH = Paths.get(System.getProperty(StringConstants.SETTING_FILE_PATH),
            StringConstants.SETTING_FILE_FOLDER_NAME, StringConstants.SETTING_FILE_NAME);

    @JsonProperty("logSettings")
    private final LogSettings logSettings;

    @JsonProperty("processSettings")
    private final ProcessSettings processSettings;

    @JsonProperty("filePropertiesSettings")
    private final FilePropertiesSettings filePropertiesSettings;

    @JsonProperty("showCachedJobSettings")
    private final ShowCachedJobSettings showCachedJobSettings;

    private boolean dirty = false;

    @JsonCreator
    public SettingsStore(@JsonProperty("logSettings") final LogSettings logSettings,
                         @JsonProperty("processSettings") final ProcessSettings processSettings,
                         @JsonProperty("filePropertiesSettings") final FilePropertiesSettings filePropertiesSettings,
                         @JsonProperty("showCachedJobSettings")
                         final ShowCachedJobSettings showCachedJobSettings) {
        this.logSettings = logSettings;
        this.processSettings = processSettings;
        this.filePropertiesSettings = filePropertiesSettings;
        this.showCachedJobSettings = showCachedJobSettings;

    }

    /**
     * Load settings. If any problem occur, it will load default settings.
     *
     * @return Setting store
     * @throws IOException
     */
    public static SettingsStore loadSettingsStore() throws IOException {
        // Do not log when loading the settings store since the logger has not been configured
        if (Files.exists(PATH)) {
            writeNewSettingsToSettingsJsonFile();
            try (final InputStream inputStream = Files.newInputStream(PATH)) {
                return JsonMapping.fromJson(inputStream, SettingsStore.class);
            } catch (final Exception e) {
                LOG.error("Failed to de-serialize", e);
                Files.delete(PATH);
                return createDefaultSettingStore();
            }
        }
        return createDefaultSettingStore();
    }

    /**
     * Create default settings.
     *
     * @return Default setting store
     */
    private static SettingsStore createDefaultSettingStore() {
        LOG.info("Creating new empty saved setting store");
        final SettingsStore settingsStore = new SettingsStore(LogSettings.DEFAULT,
                ProcessSettings.DEFAULT, FilePropertiesSettings.DEFAULT,
                ShowCachedJobSettings.DEFAULT);
        // set this to true so we will write the settings after the first run
        settingsStore.dirty = true;
        return settingsStore;
    }

    /**
     * Save setting to setting file.
     *
     * @param settingsStore saved setting store
     * @throws IOException
     */
    public static void saveSettingsStore(final SettingsStore settingsStore) throws IOException {
        if (settingsStore.dirty) {
            LOG.info("Session store was dirty, saving...");
            if (!Files.exists(PATH.getParent())) {
                Files.createDirectories(PATH.getParent());
            }
            try (final OutputStream outputStream = Files.newOutputStream(PATH,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                JsonMapping.toJson(outputStream, settingsStore);
            }
        }
    }

    /**
     * Method to include new json entry to settings.json file if any new setting property is introduced.
     */
    public static void writeNewSettingsToSettingsJsonFile() {
        try (final Stream<String> stream = Files.lines(PATH)) {
            stream.forEach(entry -> {
                if (!entry.contains(StringConstants.FILE_PROPERTIES_SETTING)) {
                    appendNewFieldToSetting(entry, StringConstants.FILE_PROPERTIES_DEFAULT);
                }
                if (!entry.contains(StringConstants.SHOW_CACHED_JOB_SETTING)) {
                    appendNewFieldToSetting(entry, StringConstants.SHOW_CACHED_JOB_DEFAULT);
                }
            });
        } catch (final Exception e) {
            LOG.error("Failed to save settings", e);
        }
    }

    /**
     * Append new setting into existing setting file
     *
     * @param entry          entry
     * @param settingDefault Default setting for the file
     */
    private static void appendNewFieldToSetting(final String entry, final String settingDefault) {
        final StringBuilder newFile = new StringBuilder(entry);
        newFile.deleteCharAt(newFile.length() - 1);
        newFile.append(settingDefault);
        try (final BufferedWriter writer = Files.newBufferedWriter(PATH)) {
            writer.write(newFile.toString());
        } catch (final Exception e) {
            LOG.error("Failed to save setting", e);
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

    public ShowCachedJobSettings getShowCachedJobSettings() {
        return showCachedJobSettings;
    }

    public void setShowCachedJobSettings(final boolean settings) {
        dirty = true;
        showCachedJobSettings.overwrite(settings);
    }
}
