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

package com.spectralogic.dsbrowser.integration;

import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.services.newSessionService.NewSessionModelValidation;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.AlertService;
import javafx.stage.Window;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.Assert.assertTrue;

public class NewSessionModelValidationTest {

    @Test
    public void validationNewSession() throws Exception {
        final ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
        final NewSessionModel model = new NewSessionModel();
        model.setSessionName("TestSession");
        model.setProxyServer(null);
        model.setEndpoint("testBp");
        model.setPortno("80");
        model.setAccessKey("testAccessId");
        model.setSecretKey("testSecretKey");
        model.setDefaultSession(true);
        assertTrue(new NewSessionModelValidation(new AlertService(resourceBundle)).validationNewSession(model, Mockito.mock(Window.class)));
    }

}