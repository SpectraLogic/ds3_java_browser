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

package com.spectralogic.dsbrowser.gui.services.newSessionService;

import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.components.validation.SessionValidation;
import com.spectralogic.dsbrowser.gui.util.AlertService;
import javafx.stage.Window;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NewSessionModelValidation {
    private final AlertService alert;

    @Inject
    public NewSessionModelValidation(final AlertService alert) {
        this.alert = alert;
    }

    public static final String ERROR = "Error";

    public boolean validationNewSession(final NewSessionModel model, final Window window) {
        if (!SessionValidation.checkStringEmptyNull(model.getSessionName())) {
            alert.error("enterSession", window);
            return false;
        } else if (!SessionValidation.checkStringEmptyNull(model.getEndpoint())) {
            alert.error("enterDataPathAddress", window);
            return false;
        } else if (!SessionValidation.validatePort(model.getPortNo().trim())) {
            alert.error("enterValidPort", window);
            return false;
        } else if (!SessionValidation.checkStringEmptyNull(model.getAccessKey())) {
            alert.error("enterAccessKey", window);
            return false;
        } else if (!SessionValidation.checkStringEmptyNull(model.getSecretKey())) {
            alert.error("enterSecretKey", window);
            return false;
        }
        return true;
    }
}
