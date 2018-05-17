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

package com.spectralogic.dsbrowser.gui.services.jobinterruption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.spectralogic.dsbrowser.gui.util.JsonMapping;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class JobInterruptionStore {
    private final static Logger LOG = LoggerFactory.getLogger(JobInterruptionStore.class);

    private final static Path PATH = Paths.get(System.getProperty(StringConstants.USER_HOME), StringConstants.SETTING_FILE_FOLDER_NAME, StringConstants.JOB_INTERRUPTION_STORE);

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
            } catch (final IOException e) {
                Files.delete(PATH);
                LOG.info("Creating new empty job ids store", e);
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
        } catch (final IOException e) {
            LOG.error("Unable to persist to InterruptedJobsStore", e);
        }
    }

    public JobInterruptionStore(@JsonProperty("jobIdsModel") final JobIdsModel jobIdsModel) {
        this.jobIdsModel = jobIdsModel;
    }

    public JobIdsModel getJobIdsModel() {
        return jobIdsModel;
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
