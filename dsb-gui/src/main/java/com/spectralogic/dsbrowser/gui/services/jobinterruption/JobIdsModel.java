package com.spectralogic.dsbrowser.gui.services.jobinterruption;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.util.GuavaCollectors;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobIdsModel {
    public static final JobIdsModel DEFAULT = createDefault();

    private static JobIdsModel createDefault() {
        final String logPath = Paths.get(System.getProperty("user.home"), ".dsbrowser", "log").toString();
        final List<Map<String, Map<String, FilesAndFolderMap>>> abc = new ArrayList<>();
        return new JobIdsModel(abc.stream().collect(GuavaCollectors.immutableList()));
    }

    @JsonProperty("endpoints")
    private ImmutableList<Map<String, Map<String, FilesAndFolderMap>>> endpoints;

    public JobIdsModel(final ImmutableList<Map<String, Map<String, FilesAndFolderMap>>> endpoints) {
        this.endpoints = endpoints;
    }

    public JobIdsModel() {
        this(null);
    }

    public void setEndpoints(final ImmutableList<Map<String, Map<String, FilesAndFolderMap>>> endpoints) {
        this.endpoints = endpoints;
    }

    public ImmutableList<Map<String, Map<String, FilesAndFolderMap>>> getEndpoints() {
        return endpoints;
    }

    public void overwrite(final JobIdsModel jobIdsModel) {
        this.endpoints = jobIdsModel.getEndpoints();
    }
}
