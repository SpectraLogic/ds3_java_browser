package com.spectralogic.dsbrowser.gui.services.jobinterruption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spectralogic.dsbrowser.gui.util.JsonMapping;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class JobInterruptionStore {
    private final static org.slf4j.Logger LOG = LoggerFactory.getLogger(JobInterruptionStore.class);

    private final static Path PATH = Paths.get(System.getProperty("user.home"), ".dsbrowser", "jobids.json");

    @JsonProperty("jobIdsModel")
    private final JobIdsModel jobIdsModel;

    public static JobInterruptionStore empty() {
        return new JobInterruptionStore(JobIdsModel.DEFAULT);
    }

    public static JobInterruptionStore loadJobIds() throws IOException {
        if (Files.exists(PATH)) {
            try (final InputStream inputStream = Files.newInputStream(PATH)) {
                final SerializedJobInterruptionStore serializedJobInterruptionStore = JsonMapping.fromJson(inputStream, SerializedJobInterruptionStore.class);
                final JobIdsModel jobIdsModel = serializedJobInterruptionStore.getJobIdsModel();
                return new JobInterruptionStore(jobIdsModel);
            } catch (final Exception e) {
                Files.delete(PATH);
                LOG.info("Creating new empty job ids store");
                return empty();
            }
        } else {
            LOG.info("Creating new empty saved job setting store");
            return empty();
        }
    }

    public static void saveJobInterruptionStore(final JobInterruptionStore jobInterruptionStore) throws IOException {
        LOG.info("Session store was dirty, saving...");
        final SerializedJobInterruptionStore store = new SerializedJobInterruptionStore(jobInterruptionStore.jobIdsModel);
        if (!Files.exists(PATH.getParent())) {
            Files.createDirectories(PATH.getParent());
        }
        try (final OutputStream outputStream = Files.newOutputStream(PATH, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            JsonMapping.toJson(outputStream, store);
        }

    }

    public JobInterruptionStore(@JsonProperty("jobIdsModel") final JobIdsModel jobIdsModel) {
        this.jobIdsModel = jobIdsModel;
    }

    public JobIdsModel getJobIdsModel() {
        return jobIdsModel;
    }

    public void setJobIdsModel(final JobIdsModel jobIdsModel) {
        jobIdsModel.overwrite(jobIdsModel);
    }

    private static class SerializedJobInterruptionStore {
        @JsonProperty("jobIdsModel")
        private final JobIdsModel jobIdsModel;

        @JsonCreator
        private SerializedJobInterruptionStore(@JsonProperty("jobIdsModel") final JobIdsModel jobIdsModel) {
            this.jobIdsModel = jobIdsModel;
        }

        public JobIdsModel getJobIdsModel() {
            return jobIdsModel;
        }
    }
}
