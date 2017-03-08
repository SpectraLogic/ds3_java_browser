package com.spectralogic.dsbrowser.gui.injector.providers;

import com.google.inject.Provider;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SavedSessionProvider implements Provider<SavedSessionStore> {

    private static final Logger LOG = LoggerFactory.getLogger(SavedSessionProvider.class);

    @Override
    public SavedSessionStore get() {
        try {
            return SavedSessionStore.loadSavedSessionStore();
        } catch (final IOException e) {
            LOG.error("Failed to load any saved sessions, continuing with none", e);
            return SavedSessionStore.empty();
        }
    }
}
