/*
 * ****************************************************************************
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
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.settings;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spectralogic.dsbrowser.gui.util.JsonMapping;
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


    public static SettingsStore getDefaults() {
        return new SettingsStore(LogSettings.DEFAULT, ProcessSettings.DEFAULT, FilePropertiesSettings.DEFAULT);
    }

    public static SettingsStore loadSettingsStore() throws IOException {
        // Do not log when loading the settings store since the logger has not been configured

        if (Files.exists(PATH)) {
            writeNewSettingsToSettingsJsonFile();
            try (final InputStream inputStream = Files.newInputStream(PATH)) {
                return JsonMapping.fromJson(inputStream, SettingsStore.class);
            } catch (final Exception e) {
                LOG.error("Failed to de-serialize", e);
                Files.delete(PATH);
                LOG.info("Creating new empty saved setting store");
                final SettingsStore settingsStore = new SettingsStore(LogSettings.DEFAULT, ProcessSettings.DEFAULT, FilePropertiesSettings.DEFAULT);
                settingsStore.dirty = true; // set this to true so we will write the settings after the first run
                return settingsStore;
            }
        } else {
            final SettingsStore settingsStore = getDefaults();
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
            stream.forEach(e -> {
                if (!e.contains("filePropertiesSettings")) {
                    StringBuilder newFile = new StringBuilder(e);
                    newFile.deleteCharAt(newFile.length() - 1);
                    newFile = newFile.append(",\"filePropertiesSettings\":{\"filePropertiesEnable\":false}}");//file property is true by default..adding this new setting to the file
                    try (final BufferedWriter writer = Files.newBufferedWriter(PATH)) {
                        writer.write(newFile.toString());
                    } catch (final IOException ex) {
                        LOG.error("Failed to save settings", ex);
                    }
                }
            });
        } catch (final Exception e) {
            LOG.error("Failed to save settings", e);
        }
    }
}
