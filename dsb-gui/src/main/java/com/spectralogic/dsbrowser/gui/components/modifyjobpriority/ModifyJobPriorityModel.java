package com.spectralogic.dsbrowser.gui.components.modifyjobpriority;

import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;

import java.util.UUID;

public class ModifyJobPriorityModel {

    private final UUID jobID;

    private final String currentPriority;

    private final Session session;

    public ModifyJobPriorityModel() {
        this(null, "", null);
    }

    public ModifyJobPriorityModel(final UUID jobID, final String currentPriority, final Session session) {
        this.jobID = jobID;
        this.currentPriority = currentPriority;
        this.session = session;
    }

    public String getCurrentPriority() {
        return currentPriority;
    }

    public UUID getJobID() {
        return jobID;
    }

    public Session getSession() {
        return session;
    }
}
