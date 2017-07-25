/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.commands.GetServiceRequest;
import com.spectralogic.ds3client.models.common.Credentials;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.ds3client.networking.FailedRequestUsingMgmtPortException;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.LazyAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ResourceBundle;

public final class CreateConnectionTask {

    private final static Logger LOG = LoggerFactory.getLogger(CreateConnectionTask.class);

    private static final LazyAlert alert = new LazyAlert("Error");

    public static Session createConnection(final NewSessionModel newSessionModel, final ResourceBundle resourceBundle) {
        try {
            if (newSessionModel.getProxyServer() != null && newSessionModel.getProxyServer().isEmpty()) {
                newSessionModel.setProxyServer(null);
            }
            final Ds3Client client = Ds3ClientBuilder
                    .create(newSessionModel.getEndpoint().trim() + ":" + newSessionModel.getPortNo().trim(),
                            new Credentials(newSessionModel.getAccessKey(), newSessionModel.getSecretKey()))
                    .withHttps(newSessionModel.isUseSSL()).withCertificateVerification(false)
                    .withProxy(newSessionModel.getProxyServer())
                    .withUserAgent(resourceBundle.getString("eonbrowser") + " " + resourceBundle.getString("buildVersion"))
                    .build();
            client.getService(new GetServiceRequest());
            return new Session(newSessionModel.getSessionName(), newSessionModel.getEndpoint(), newSessionModel.getPortNo(), newSessionModel.getProxyServer(), client, newSessionModel.getDefaultSession(),
                    newSessionModel.isUseSSL());
        } catch (final UnknownHostException e) {
            LOG.error("Invalid Endpoint Server Name or IP Address: ", e);
            alert.showAlert(resourceBundle.getString("invalidEndpointMessage"));
        } catch (final FailedRequestUsingMgmtPortException e) {
            LOG.error("Attempted data access on management port -- check endpoint: ", e);
            alert.showAlert(resourceBundle.getString("checkEndpoint"));
        } catch (final FailedRequestException e) {
            if (e.getStatusCode() == 403) {
                if (e.getError().getCode().equals("RequestTimeTooSkewed")) {
                    LOG.error("Failed To authenticate session : Client's clock is not synchronized with server's clock: ", e);
                    alert.showAlert(resourceBundle.getString("failToAuthenticateMessage"));
                } else {
                    LOG.error("Invalid Access ID or Secret Key: ", e);
                    alert.showAlert(resourceBundle.getString("invalidIDKEYMessage"));
                }
            } else {
                LOG.error("BlackPearl return an unexpected status code we did not expect: {}", e);
                alert.showAlert(resourceBundle.getString("unexpectedStatusMessage"));
            }
        } catch (final IOException ioe) {
            LOG.error("Encountered a networking error: ", ioe);
            alert.showAlert(resourceBundle.getString("networkErrorMessage"));
        } catch (final RuntimeException rte) {
            LOG.error("Something went wrong.", rte);
            alert.showAlert(resourceBundle.getString("authenticationAlert"));
        }
        return null;
    }
}
