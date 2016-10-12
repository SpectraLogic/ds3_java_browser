package com.spectralogic.dsbrowser.gui.services.jobinterruption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Map;

public class JobInterruption {

    @JsonProperty("endpoints")
    private final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints;

    @JsonCreator
    public JobInterruption(@JsonProperty("endpoints") final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints) {
        this.endpoints = endpoints;
    }

    public ArrayList<Map<String, Map<String, FilesAndFolderMap>>> getEndpoints() {
        return endpoints;
    }
}
