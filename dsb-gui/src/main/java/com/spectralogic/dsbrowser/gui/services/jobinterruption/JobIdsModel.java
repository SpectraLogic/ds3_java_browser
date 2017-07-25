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

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobIdsModel {
    public static final JobIdsModel DEFAULT = createDefault();

    private static JobIdsModel createDefault() {
        final String logPath = Paths.get(System.getProperty("user.home"), ".dsbrowser", "log").toString();
        final List<Map<String, Map<String, FilesAndFolderMap>>> jobIdModels = new ArrayList<>();
        return new JobIdsModel(jobIdModels);
    }

    @JsonProperty("endpoints")
    private List<Map<String, Map<String, FilesAndFolderMap>>> endpoints;

    public JobIdsModel(final List<Map<String, Map<String, FilesAndFolderMap>>> endpoints) {
        this.endpoints = endpoints;
    }

    public JobIdsModel() {
        this(null);
    }

    public void setEndpoints(final List<Map<String, Map<String, FilesAndFolderMap>>> endpoints) {
        this.endpoints = endpoints;
    }

    public List<Map<String, Map<String, FilesAndFolderMap>>> getEndpoints() {
        return endpoints;
    }

    public void overwrite(final JobIdsModel jobIdsModel) {
        this.endpoints = jobIdsModel.getEndpoints();
    }
}
