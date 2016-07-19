package com.spectralogic.dsbrowser.gui.services.jobprioritystore;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Paths;

public class JobSettings {

    public static final JobSettings DEFAULT = createDefault();

    private static JobSettings createDefault() {
        final String logPath = Paths.get(System.getProperty("user.home"), ".dsbrowser", "log").toString();
        return new JobSettings("default", "default", "NORMAL", "NORMAL");
    }


    @JsonProperty("sessionName")
    private String sessionName;
    @JsonProperty("endpoint")
    private String endpoint;
    @JsonProperty("getJobPriority")
    private String getJobPriority;
    @JsonProperty("putJobPriority")
    private String putJobPriority;


    public JobSettings(final String sessionName, final String endpoint, final String getJobPriority, final String putJobPriority) {
        this.endpoint = endpoint;
        this.sessionName = sessionName;
        this.putJobPriority = putJobPriority;
        this.getJobPriority = getJobPriority;
    }

    public JobSettings() {
        this(null, null, null, null);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setGetJobPriority(String getJobPriority) {
        this.getJobPriority = getJobPriority;
    }

    public void setPutJobPriority(String putJobPriority) {
        this.putJobPriority = putJobPriority;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getGetJobPriority() {
        return getJobPriority;
    }

    public String getPutJobPriority() {
        return putJobPriority;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void overwrite(final JobSettings settings) {
        this.setEndpoint(settings.getEndpoint());
        this.setSessionName(settings.getSessionName());
        this.setPutJobPriority(settings.getPutJobPriority());
        this.setGetJobPriority(settings.getGetJobPriority());
    }

}
