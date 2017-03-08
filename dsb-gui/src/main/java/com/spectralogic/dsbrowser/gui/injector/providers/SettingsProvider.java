package com.spectralogic.dsbrowser.gui.injector.providers;

import com.google.inject.Provider;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SettingsProvider implements Provider<SettingsStore> {

    private final static Logger LOG = LoggerFactory.getLogger(SettingsProvider.class);

    @Override
    public SettingsStore get() {
        try {
            return SettingsStore.loadSettingsStore();
        } catch (final IOException e) {
            LOG.error("Failed to load SettingsStore, returning defaults", e);
            return SettingsStore.getDefaults();
        }
    }
}
