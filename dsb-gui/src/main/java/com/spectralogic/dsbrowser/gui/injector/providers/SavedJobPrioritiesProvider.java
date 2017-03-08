package com.spectralogic.dsbrowser.gui.injector.providers;

import com.google.inject.Provider;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SavedJobPrioritiesProvider implements Provider<SavedJobPrioritiesStore> {

    private static final Logger LOG = LoggerFactory.getLogger(SavedJobPrioritiesProvider.class);

    @Override
    public SavedJobPrioritiesStore get() {
        try {
            return SavedJobPrioritiesStore.loadSavedJobPriorities();
        } catch (final IOException e) {
            LOG.error("Failed to load SavedJobPriorities, returning empty list", e);
            return SavedJobPrioritiesStore.empty();
        }
    }
}
