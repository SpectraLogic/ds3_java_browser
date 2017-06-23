/*
 * ****************************************************************************
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
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.newSessionService;

import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;

public class SessionModelService {

    public static NewSessionModel setSessionModel(final SavedSession savedSession, final boolean defaultSession) {
        final NewSessionModel newModel = new NewSessionModel();
        newModel.setSessionName(savedSession.getName());
        newModel.setDefaultSession(defaultSession);
        newModel.setAccessKey(savedSession.getCredentials().getAccessId());
        newModel.setSecretKey(savedSession.getCredentials().getSecretKey());
        newModel.setEndpoint(savedSession.getEndpoint());
        newModel.setPortno(savedSession.getPortNo());
        newModel.setProxyServer(savedSession.getProxyServer());
        return newModel;
    }

    public static NewSessionModel copy(final NewSessionModel model) {
        final NewSessionModel newModel = new NewSessionModel();
        if (model.getSessionName() != null) {
            newModel.setSessionName(model.getSessionName().trim());
        }
        if (model.getEndpoint() != null) {
            newModel.setEndpoint(model.getEndpoint().trim());
        }
        if (model.getAccessKey() != null) {
            newModel.setAccessKey(model.getAccessKey().trim());
        }
        if (model.getPortNo() != null) {
            newModel.setPortno(model.getPortNo().trim());
        }
        if (model.getSecretKey() != null) {
            newModel.setSecretKey(model.getSecretKey().trim());
        }
        if (model.getProxyServer() != null) {
            newModel.setProxyServer(model.getProxyServer().trim());
        }
        newModel.setDefaultSession(model.getDefaultSession());
        return newModel;
    }
}

