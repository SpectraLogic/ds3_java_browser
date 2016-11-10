package com.spectralogic.dsbrowser.gui.services.jobprioritystore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SavedJobPriorities {

    @JsonProperty("putJobPriority")
    private final String putJobPriority;
    @JsonProperty("getJobPriority")
    private final String getJobPriority;

    @JsonCreator
    public SavedJobPriorities(@JsonProperty("putJobPriority") final String putJobPriority, @JsonProperty("getJobPriority") final String getJobPriority) {

        this.putJobPriority = putJobPriority;
        this.getJobPriority = getJobPriority;

    }

    public String getPutJobPriority() {
        return putJobPriority;
    }

    public String getGetJobPriority() {
        return getJobPriority;
    }


}
