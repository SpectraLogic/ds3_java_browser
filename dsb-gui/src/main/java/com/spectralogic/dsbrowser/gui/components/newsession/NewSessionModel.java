package com.spectralogic.dsbrowser.gui.components.newsession;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.commands.GetServiceRequest;
import com.spectralogic.ds3client.commands.GetServiceResponse;
import com.spectralogic.ds3client.commands.spectrads3.GetSystemInformationSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetSystemInformationSpectraS3Response;
import com.spectralogic.ds3client.models.common.Credentials;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.ds3client.networking.FailedRequestUsingMgmtPortException;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.net.UnknownHostException;

public class NewSessionModel {
    private final Alert ALERT = new Alert(Alert.AlertType.ERROR);

    private final StringProperty sessionName = new SimpleStringProperty();
    private final StringProperty endpoint = new SimpleStringProperty();
    private final StringProperty accessKey = new SimpleStringProperty();
    private final StringProperty secretKey = new SimpleStringProperty();
    private final StringProperty portNo = new SimpleStringProperty();
    private final StringProperty proxyServer = new SimpleStringProperty();

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

    public StringProperty portNoProperty() {
        return portNo;
    }

    public String getPortNo() {
        return portNo.get();
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

    public Session toSession() {

        ALERT.setHeaderText(null);
        Ds3Client client = null;
        try {
            if (this.getProxyServer() != null && this.getProxyServer().equals("")) {
                this.setProxyServer(null);
            }
            client = Ds3ClientBuilder
                    .create(this.getEndpoint() + ":" + this.getPortNo(),
                            new Credentials(this.getAccessKey(),
                                    this.getSecretKey()))
                    .withHttps(false).withProxy(this.getProxyServer())
                    .build();

            final GetSystemInformationSpectraS3Response sysreponse = client.getSystemInformationSpectraS3(new GetSystemInformationSpectraS3Request());
            final GetServiceResponse response = client.getService(new GetServiceRequest());
            return new Session(this.getSessionName(), this.getEndpoint(), this.getPortNo(), this.getProxyServer(), client);


        } catch (final UnknownHostException e) {
            ALERT.setTitle("Invalid Endpoint");
            ALERT.setContentText("Invalid Endpoint Server Name or IP Address");
            ALERT.showAndWait();

        } catch (final FailedRequestUsingMgmtPortException e) {
            ALERT.setContentText("Attempted data access on management port -- check endpoint");
            ALERT.showAndWait();
        } catch (final FailedRequestException e) {
            if (e.getStatusCode() == 403) {
                if (e.getError().getCode().equals("RequestTimeTooSkewed")) {
                    ALERT.setTitle("Failed To authenticate session");
                    ALERT.setContentText("Failed To authenticate session : Client's clock is not synchronized with server's clock");
                    ALERT.showAndWait();
                }
                ALERT.setTitle("Invalid ID and KEY");
                ALERT.setContentText("Invalid Access ID or Secret Key");
                ALERT.showAndWait();
            } else {
                ALERT.setTitle("Unexpected Status");
                ALERT.setContentText("BlackPearl return an unexpected status code we did not expect");
                ALERT.showAndWait();

            }
        } catch (final Exception e) {

            if (e instanceof IOException) {
                ALERT.setTitle("Networking Error");
                ALERT.setContentText("Encountered a networking error");
                ALERT.showAndWait();
            } else if (e instanceof RuntimeException) {
                ALERT.setTitle("Error");
                ALERT.setContentText("Authentication error. Please check your credentials");
                ALERT.showAndWait();
            }
        }
        return null;
    }
}
