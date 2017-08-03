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

package com.spectralogic.dsbrowser.integration.services.newSessionService;

import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

public class NewSessionModelValidationTest {
    private boolean successFlag = false;

    @Test
    public void validationNewSession() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final NewSessionModel model = new NewSessionModel();
        model.setSessionName(SessionConstants.SESSION_NAME);
        model.setProxyServer(null);
        model.setEndpoint(SessionConstants.SESSION_PATH);
        model.setPortno(SessionConstants.PORT_NO);
        model.setAccessKey(SessionConstants.ACCESS_ID);
        model.setSecretKey(SessionConstants.SECRET_KEY);
        model.setDefaultSession(true);
        new JFXPanel();
        Platform.runLater(() -> {
            if (!NewSessionModelValidation.validationNewSession(model)) {
                successFlag = true;
                latch.countDown();
            }});
        latch.await();
        Assert.assertTrue(successFlag);
    }

}