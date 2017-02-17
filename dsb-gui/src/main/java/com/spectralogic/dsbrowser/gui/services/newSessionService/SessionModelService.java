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
        newModel.setSessionName(model.getSessionName());
        newModel.setEndpoint(model.getEndpoint());
        newModel.setAccessKey(model.getAccessKey());
        newModel.setPortno(model.getPortNo());
        newModel.setSecretKey(model.getSecretKey());
        newModel.setProxyServer(model.getProxyServer());
        newModel.setDefaultSession(model.getDefaultSession());
        return newModel;
    }
    }

