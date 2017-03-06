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
