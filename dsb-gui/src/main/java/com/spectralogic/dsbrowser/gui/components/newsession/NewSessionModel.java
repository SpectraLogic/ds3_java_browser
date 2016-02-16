package com.spectralogic.dsbrowser.gui.components.newsession;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class NewSessionModel {

    private final StringProperty endpoint = new SimpleStringProperty();
    private final StringProperty accessKey = new SimpleStringProperty();
    private final StringProperty secretKey = new SimpleStringProperty();

    public String getEndpoint() {
        return endpoint.get();
    }

    public StringProperty endpointProperty() {
        return endpoint;
    }

    public void setEndpoint(final String endpoint) {
        this.endpoint.set(endpoint);
    }

    public String getAccessKey() {
        return accessKey.get();
    }

    public StringProperty accessKeyProperty() {
        return accessKey;
    }

    public void setAccessKey(final String accessKey) {
        this.accessKey.set(accessKey);
    }

    public String getSecretKey() {
        return secretKey.get();
    }

    public StringProperty secretKeyProperty() {
        return secretKey;
    }

    public void setSecretKey(final String secretKey) {
        this.secretKey.set(secretKey);
    }
}
