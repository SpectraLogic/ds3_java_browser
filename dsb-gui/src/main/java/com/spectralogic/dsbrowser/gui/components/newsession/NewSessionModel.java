package com.spectralogic.dsbrowser.gui.components.newsession;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.models.common.Credentials;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class NewSessionModel {

    private final StringProperty sessionName = new SimpleStringProperty();
    private final StringProperty endpoint = new SimpleStringProperty();
    private final StringProperty accessKey = new SimpleStringProperty();
    private final StringProperty secretKey = new SimpleStringProperty();
    //Added By VVDN Team
    private final StringProperty portNo = new SimpleStringProperty();

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

    public String getSessionName() {
        return sessionName.get();
    }

    public StringProperty sessionNameProperty() {
        return sessionName;
    }

    public void setSessionName(final String sessionName) {
        this.sessionName.set(sessionName);
    }

    public void setPortno(final String portNo) {
        this.portNo.set(portNo);
    }

    public StringProperty portNoProperty() {
        return portNo;
    }

    public String getPortNo() {
        return portNo.get();
    }

    public Session toSession() {
        final Ds3Client client = Ds3ClientBuilder
                .create(this.getEndpoint() + ":" + this.getPortNo(),
                        new Credentials(this.getAccessKey(),
                                this.getSecretKey()))
                .withHttps(false)
                .build();
        return new Session(this.getSessionName(), this.getEndpoint(), this.getPortNo(), client);
    }

}
