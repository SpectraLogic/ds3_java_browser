package com.spectralogic.dsbrowser.gui.components.jobpriority;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class JobSettingsModel {

    private final StringProperty sessionName = new SimpleStringProperty();
    private final SimpleStringProperty endpoint = new SimpleStringProperty();
    private final SimpleStringProperty putJobPriorty = new SimpleStringProperty();
    private final SimpleStringProperty getJobPriorty = new SimpleStringProperty();

    public JobSettingsModel() {
        //pass
    }

    public String getSessionName() {
        return sessionName.get();
    }

    public StringProperty sessionNameProperty() {
        return sessionName;
    }

    public void setSessionName(final String sessionName) {
        this.sessionName.set(sessionName);
    }

    public String getEndpointName() {
        return endpoint.get();
    }

    public StringProperty endpointProperty() {
        return endpoint;
    }

    public void setEndpoint(final String endpoint) {
        this.endpoint.set(endpoint);
    }

    public String getPutJobPriority() {
        return putJobPriorty.get();
    }

    public StringProperty putJobProperty() {
        return putJobPriorty;
    }

    public void setPutJobPriorty(final String putJobPriorty) {
        this.putJobPriorty.set(putJobPriorty);
    }

    public String getGetJobPriorty() {
        return getJobPriorty.get();
    }

    public StringProperty getJobProperty() {
        return getJobPriorty;
    }

    public void setGetJobPriorty(final String getJobPriorty) {
        this.getJobPriorty.set(getJobPriorty);
    }
}
