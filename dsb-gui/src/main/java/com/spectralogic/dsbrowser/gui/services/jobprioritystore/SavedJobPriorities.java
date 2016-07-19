package com.spectralogic.dsbrowser.gui.services.jobprioritystore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SavedJobPriorities {
    @JsonProperty("sessionName")
    private final String sessionName;
    @JsonProperty("endpoint")
    private final String endpoint;
    @JsonProperty("putJobPriority")
    private final String putJobPriority;
    @JsonProperty("getJobPriority")
    private final String getJobPriority;

    @JsonCreator
    public SavedJobPriorities(@JsonProperty("sessionName") final String sessionName, @JsonProperty("endpoint") final String endpoint, @JsonProperty("putJobPriority") final String putJobPriority, @JsonProperty("getJobPriority") final String getJobPriority) {
        this.sessionName = sessionName;
        this.endpoint = endpoint;
        this.putJobPriority = putJobPriority;
        this.getJobPriority = getJobPriority;

    }

    public String getSessionName() {
        return sessionName;
    }

    public String getPutJobPriority() {
        return putJobPriority;
    }

    public String getGetJobPriority() {
        return getJobPriority;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
