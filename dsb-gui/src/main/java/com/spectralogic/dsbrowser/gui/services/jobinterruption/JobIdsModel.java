package com.spectralogic.dsbrowser.gui.services.jobinterruption;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

public class JobIdsModel {
    public static final JobIdsModel DEFAULT = createDefault();

    private static JobIdsModel createDefault() {
        final String logPath = Paths.get(System.getProperty("user.home"), ".dsbrowser", "log").toString();
        final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> abc = new ArrayList<>();
        return new JobIdsModel(abc);
    }

    @JsonProperty("endpoints")
    private ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints;

    public JobIdsModel(final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints) {
        this.endpoints = endpoints;
    }

    public JobIdsModel() {
        this(null);
    }

    public void setEndpoints(final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints) {
        this.endpoints = endpoints;
    }

    public ArrayList<Map<String, Map<String, FilesAndFolderMap>>> getEndpoints() {
        return endpoints;
    }

    public void overwrite(final JobIdsModel jobIdsModel) {
        this.endpoints = jobIdsModel.getEndpoints();
    }
}
