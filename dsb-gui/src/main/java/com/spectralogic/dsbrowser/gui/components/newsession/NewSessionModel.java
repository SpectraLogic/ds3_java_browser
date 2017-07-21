package com.spectralogic.dsbrowser.gui.components.newsession;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class NewSessionModel {

    private final StringProperty sessionName = new SimpleStringProperty();
    private final StringProperty endpoint = new SimpleStringProperty();
    private final StringProperty accessKey = new SimpleStringProperty();
    private final StringProperty secretKey = new SimpleStringProperty();
    private final StringProperty portNo = new SimpleStringProperty();
    private final StringProperty proxyServer = new SimpleStringProperty();
    private final BooleanProperty defaultSession = new SimpleBooleanProperty();
    private final BooleanProperty useSSL = new SimpleBooleanProperty();

    public Boolean getDefaultSession() {
        return defaultSession.get();
    }

    public Boolean getUseSSL() { return useSSL.get(); }

    public void setDefaultSession(final Boolean defaultSession) {
        this.defaultSession.set(defaultSession);
    }

    public void setUseSSL(final Boolean useSSL) { this.useSSL.setValue(useSSL);}

    public BooleanProperty defaultSessionProperty() {
        return defaultSession;
    }

    public BooleanProperty useSSLProperty() { return useSSL; }

    public String getEndpoint() {
        return endpoint.get();
    }

    public void setEndpoint(final String endpoint) {
        this.endpoint.set(endpoint);
    }

    public StringProperty endpointProperty() {
        return endpoint;
    }

    public String getAccessKey() {
        return accessKey.get();
    }

    public void setAccessKey(final String accessKey) {
        this.accessKey.set(accessKey);
    }

    public StringProperty accessKeyProperty() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey.get();
    }

    public void setSecretKey(final String secretKey) {
        this.secretKey.set(secretKey);
    }

    public StringProperty secretKeyProperty() {
        return secretKey;
    }

    public String getSessionName() {
        return sessionName.get();
    }

    public void setSessionName(final String sessionName) {
        this.sessionName.set(sessionName);
    }

    public StringProperty sessionNameProperty() {
        return sessionName;
    }

    public void setPortno(final String portNo) {
        this.portNo.set(portNo);
    }

    public String getPortNo() {
        return portNo.get();
    }

    public StringProperty portNoProperty() {
        return portNo;
    }

    public String getProxyServer() {
        return proxyServer.get();
    }

    public void setProxyServer(final String proxyServer) {
        this.proxyServer.set(proxyServer);
    }

    public StringProperty proxyServerProperty() {
        return proxyServer;
    }

    public String debug() {
        String s = "";
        s += "Session Name: " + sessionName.get() + "\n";
        s += "Endpoint: " + endpoint.get() + "\n";
        s += "Access Key: " + accessKey.get() + "\n";
        s += "Secret Key: " + secretKey.get() + "\n";
        s += "Port: " + portNo.get() + "\n";
        s += "Proxy: " + proxyServer.get() + "\n";
        s += "Default: " + defaultSession.get() + "\n";
        s += "SSL: " + useSSL.get() + "\n";

        return s;
    }

}
