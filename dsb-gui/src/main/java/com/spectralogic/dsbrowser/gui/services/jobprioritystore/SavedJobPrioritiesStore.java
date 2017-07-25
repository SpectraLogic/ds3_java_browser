/*
 * ******************************************************************************
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
 * ******************************************************************************
 */

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

    @JsonProperty("jobSettings")
    private final JobSettings jobSettings;

    public static SavedJobPrioritiesStore empty() {
        return new SavedJobPrioritiesStore(JobSettings.DEFAULT);
    }

    public static SavedJobPrioritiesStore loadSavedJobPriorities() throws IOException {
        if (Files.exists(PATH)) {
            try (final InputStream inputStream = Files.newInputStream(PATH)) {
                return JsonMapping.fromJson(inputStream, SavedJobPrioritiesStore.class);
            } catch (final Exception e) {
                LOG.error("Failed to load existing job settings", e);
                Files.delete(PATH);
                LOG.info("Creating new empty saved job setting store");
                return empty();
            }
        } else {
            LOG.info("Creating new empty saved job setting store");
            return empty();
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
