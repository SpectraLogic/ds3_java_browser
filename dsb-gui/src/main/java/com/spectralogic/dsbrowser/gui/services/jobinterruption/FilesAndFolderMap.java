package com.spectralogic.dsbrowser.gui.services.jobinterruption;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.util.Map;

public class FilesAndFolderMap {

    @JsonProperty("files")
    private Map<String, Path> files;
    @JsonProperty("folders")
    private Map<String, Path> folders;
    @JsonProperty("type")
    private String type;
    @JsonProperty("date")
    private String date;
    @JsonProperty("nonAdjacent")
    private boolean nonAdjacent;
    @JsonProperty("targetLocation")
    private String targetLocation;
    @JsonProperty("totalJobSize")
    private long totalJobSize;
    @JsonProperty("bucket")
    private String bucket;

    @JsonCreator
    public FilesAndFolderMap(@JsonProperty("files") final Map<String, Path> files, @JsonProperty("folders") final Map<String, Path> folders, @JsonProperty("type") final String type, @JsonProperty("date") final String date, @JsonProperty("nonAdjecent")
    final boolean nonAdjacent, @JsonProperty("targetLocation") final String targetLocation, @JsonProperty("totalJobSize") final long totalJobSize, @JsonProperty("bucket") final String bucket) {
        this.files = files;
        this.folders = folders;
        this.type = type;
        this.date = date;
        this.nonAdjacent = nonAdjacent;
        this.targetLocation = targetLocation;
        this.totalJobSize = totalJobSize;
        this.bucket = bucket;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(final String bucket) {
        this.bucket = bucket;
    }

    public long getTotalJobSize() {
        return totalJobSize;
    }

    public void setTotalJobSize(final long totalJobSize) {
        this.totalJobSize = totalJobSize;
    }

    public boolean isNonAdjacent() {
        return nonAdjacent;
    }

    public void setNonAdjacent(final boolean nonAdjacent) {
        this.nonAdjacent = nonAdjacent;
    }

    public String getTargetLocation() {
        return targetLocation;
    }

    public void setTargetLocation(final String targetLocation) {
        this.targetLocation = targetLocation;
    }

    public String getDate() {
        return date;
    }

    public void setDate(final String date) {
        this.date = date;
    }

    public Map<String, Path> getFiles() {
        return files;
    }

    public Map<String, Path> getFolders() {
        return folders;
    }

    public void setFiles(final Map<String, Path> files) {
        this.files = files;
    }

    public void setFolders(final Map<String, Path> folders) {
        this.folders = folders;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

}
