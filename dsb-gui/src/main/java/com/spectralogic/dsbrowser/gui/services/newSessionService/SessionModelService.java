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

