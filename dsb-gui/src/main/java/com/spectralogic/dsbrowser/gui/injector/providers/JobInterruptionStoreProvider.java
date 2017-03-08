package com.spectralogic.dsbrowser.gui.injector.providers;

import com.google.inject.Provider;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JobInterruptionStoreProvider implements Provider<JobInterruptionStore> {

    private static final Logger LOG = LoggerFactory.getLogger(JobInterruptionStoreProvider.class);

    @Override
    public JobInterruptionStore get() {
        try {
            return JobInterruptionStore.loadJobIds();
        } catch (final IOException e) {
            LOG.error("Could not load the list of previously interrupted jobs", e);
            return JobInterruptionStore.empty();
        }
    }
}
