package com.spectralogic.dsbrowser.gui.services.jobprioritystore;

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

public class SavedJobPrioritiesStore {

    private final static Logger LOG = LoggerFactory.getLogger(SavedJobPrioritiesStore.class);

    private final static Path PATH = Paths.get(System.getProperty("user.home"), ".dsbrowser", "jobsettings.json");

    private boolean dirty = false;

    @JsonProperty("jobSettings")
    private final JobSettings jobSettings;

    public static SavedJobPrioritiesStore loadSavedJobPriorties() throws IOException {
        if (Files.exists(PATH)) {
            try (final InputStream inputStream = Files.newInputStream(PATH)) {
                return JsonMapping.fromJson(inputStream, SavedJobPrioritiesStore.class);
            } catch (final Exception e) {
                Files.delete(PATH);
                LOG.info("Creating new empty saved job setting store");
                final SavedJobPrioritiesStore savedJobPrioritiesStore = new SavedJobPrioritiesStore(JobSettings.DEFAULT);
                savedJobPrioritiesStore.dirty = true;
                return savedJobPrioritiesStore;
            }
        } else {
            LOG.info("Creating new empty saved job setting store");
            final SavedJobPrioritiesStore savedJobPrioritiesStore = new SavedJobPrioritiesStore(JobSettings.DEFAULT);
            savedJobPrioritiesStore.dirty = true;
            return savedJobPrioritiesStore;
        }
    }

    public static void saveSavedJobPriorties(final SavedJobPrioritiesStore jobPrioritiesStore) throws IOException {
        LOG.info("Session store was dirty, saving...");
        final SerializedJobPriorityStore store = new SerializedJobPriorityStore(jobPrioritiesStore.jobSettings);
        if (!Files.exists(PATH.getParent())) {
            Files.createDirectories(PATH.getParent());
        }
        try (final OutputStream outputStream = Files.newOutputStream(PATH, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            JsonMapping.toJson(outputStream, store);
        }

    }

    public SavedJobPrioritiesStore(@JsonProperty("jobSettings") final JobSettings jobSettings) {
        this.jobSettings = jobSettings;
    }

    public JobSettings getJobSettings() {
        return jobSettings;
    }

    private static class SerializedJobPriorityStore {
        @JsonProperty("jobSettings")
        private final JobSettings jobSettings;

        @JsonCreator
        private SerializedJobPriorityStore(@JsonProperty("jobSettings") final JobSettings jobSettings) {
            this.jobSettings = jobSettings;
        }

        public JobSettings getJobSettings() {
            return jobSettings;
        }
    }
}
