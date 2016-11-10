package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;


public class JobInfoModel {
    private final String name;
    private final String jobId;
    private final String date;
    private final long size;
    private final String fullPath;
    private final String jobType;
    private final String status;
    private final Type type;
    private final String targetLocation;
    private final String bucket;


    public JobInfoModel(final String name, final String jobId, final String date, final long size, final String fullPath, final String jobType, final String status, final Type type, final String targetLocation, final String bucket) {
        this.fullPath = fullPath;
        this.name = name;
        this.jobType = jobType;
        this.status = status;
        this.type = type;
        this.date = date;
        this.size = size;
        this.jobId = jobId;
        this.targetLocation = targetLocation;
        this.bucket = bucket;
    }

    public String getBucket() {
        return bucket;
    }

    public String getTargetLocation() {
        return targetLocation;
    }

    public String getJobId() {
        return jobId;
    }

    public String getDate() {
        return date;
    }

    public long getSize() {
        return size;
    }

    public String getFullPath() {
        return fullPath;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getJobType() {
        return jobType;
    }

    public String getStatus() {
        return status;
    }

    public enum Type {
        File, Directory, JOBID
    }
}
