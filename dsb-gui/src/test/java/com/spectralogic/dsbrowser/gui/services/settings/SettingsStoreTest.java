package com.spectralogic.dsbrowser.gui.services.settings;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class SettingsStoreTest {

    @Test
    public void loadSettingsStore() throws Exception {
        final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
        assertNotNull(settingsStore);
    }

    @Test
    public void writeNewSettingsToSettingsJsonFile() throws Exception {
        SettingsStore.writeNewSettingsToSettingsJsonFile();
        final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
        final FilePropertiesSettings filePropertiesSettings = settingsStore.getFilePropertiesSettings();
        assertNotNull(filePropertiesSettings);
    }
}