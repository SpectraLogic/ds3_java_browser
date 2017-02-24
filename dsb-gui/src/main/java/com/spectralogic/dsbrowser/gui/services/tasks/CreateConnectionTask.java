package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.commands.GetServiceRequest;
import com.spectralogic.ds3client.models.common.Credentials;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.ds3client.networking.FailedRequestUsingMgmtPortException;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.Ds3Alert;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ResourceBundle;

public class CreateConnectionTask {
    @Inject
    private ResourceBundle resourceBundle;

    private final static Logger LOG = LoggerFactory.getLogger(CreateConnectionTask.class);

    public Session createConnection(final NewSessionModel newSessionModel) {
        Ds3Client client = null;
        try {
            if (newSessionModel.getProxyServer() != null && newSessionModel.getProxyServer().equals("")) {
                newSessionModel.setProxyServer(null);
            }
            client = Ds3ClientBuilder
                    .create(newSessionModel.getEndpoint().trim() + ":" + newSessionModel.getPortNo().trim(),
                            new Credentials(newSessionModel.getAccessKey(),
                                    newSessionModel.getSecretKey()))
                    .withHttps(false).withProxy(newSessionModel.getProxyServer())
                    .build();
            client.getService(new GetServiceRequest());
            return new Session(newSessionModel.getSessionName(), newSessionModel.getEndpoint(), newSessionModel.getPortNo(), newSessionModel.getProxyServer(), client, newSessionModel.getDefaultSession());
        } catch (final UnknownHostException e) {
            LOG.error("Invalid Endpoint Server Name or IP Address: {}", e);
            Ds3Alert.show(resourceBundle.getString("invalidEndpoint"), resourceBundle.getString("invalidEndpointMessage"), Alert.AlertType.ERROR);
        } catch (final FailedRequestUsingMgmtPortException e) {
            LOG.error("Attempted data access on management port -- check endpoint: {}", e);
            Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("checkEndpoint"), Alert.AlertType.ERROR);
        } catch (final FailedRequestException e) {
            if (e.getStatusCode() == 403) {
                if (e.getError().getCode().equals("RequestTimeTooSkewed")) {
                    LOG.error("Failed To authenticate session : Client's clock is not synchronized with server's" +
                            "clock: {}", e);
                    Ds3Alert.show(resourceBundle.getString("failToAuthenticate"), resourceBundle.getString("failToAuthenticateMessage"), Alert.AlertType.ERROR);
                } else {
                    LOG.error("Invalid Access ID or Secret Key: {}", e);
                    Ds3Alert.show(resourceBundle.getString("invalidIDKEY"), resourceBundle.getString("invalidIDKEYMessage"), Alert.AlertType.ERROR);
                }
            } else {
                LOG.error("BlackPearl return an unexpected status code we did not expect: {}", e);
                Ds3Alert.show(resourceBundle.getString("unexpectedStatus"), resourceBundle.getString("unexpectedStatusMessage"), Alert.AlertType.ERROR);
            }
        } catch (final IOException ioe) {
            LOG.error("Encountered a networking error: {}", ioe);
            Ds3Alert.show(resourceBundle.getString("networkError"), resourceBundle.getString("networkErrorMessage"), Alert.AlertType.ERROR);
        } catch (final RuntimeException rte) {
            LOG.error("Something went wrong.", rte);
            Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("authenticationAlert"), Alert.AlertType.ERROR);
        }
        return null;
    }
}
