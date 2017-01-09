package com.spectralogic.dsbrowser.gui.services.jobprioritystore;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Paths;

public class JobSettings {

    public static final JobSettings DEFAULT = createDefault();

    private static JobSettings createDefault() {
        final String logPath = Paths.get(System.getProperty("user.home"), ".dsbrowser", "log").toString();
        return new JobSettings("Data Policy Default (no change)", "Data Policy Default (no change)", false);
    }

    @JsonProperty("getJobPriority")
    private String getJobPriority;
    @JsonProperty("putJobPriority")
    private String putJobPriority;


    public JobSettings(final String getJobPriority, final String putJobPriority, final boolean isDefaultForAll) {
        this.putJobPriority = putJobPriority;
        this.getJobPriority = getJobPriority;
    }

    public JobSettings() {
        this(null, null, false);
    }

    public void setGetJobPriority(final String getJobPriority) {
        this.getJobPriority = getJobPriority;
    }

    public void setPutJobPriority(final String putJobPriority) {
        this.putJobPriority = putJobPriority;
    }

    public String getGetJobPriority() {
        return getJobPriority;
    }

    public String getPutJobPriority() {
        return putJobPriority;
    }

    public void overwrite(final JobSettings settings) {
        this.setPutJobPriority(settings.getPutJobPriority());
        this.setGetJobPriority(settings.getGetJobPriority());
    }
}
