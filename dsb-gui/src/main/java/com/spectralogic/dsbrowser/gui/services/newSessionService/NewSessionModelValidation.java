package com.spectralogic.dsbrowser.gui.services.newSessionService;

import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.components.validation.SessionValidation;
import com.spectralogic.dsbrowser.gui.util.LazyAlert;
import com.spectralogic.dsbrowser.gui.util.ResourceBundleProperties;

import java.util.ResourceBundle;

public class NewSessionModelValidation {
    private static final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();
    private static final LazyAlert alert = new LazyAlert("Error");

    public static boolean validationNewSession(final NewSessionModel model) {
        if (!SessionValidation.checkStringEmptyNull(model.getSessionName())) {
            alert.showAlert(resourceBundle.getString("enterSession"));
            return true;
        } else if (!SessionValidation.checkStringEmptyNull(model.getEndpoint())) {
            alert.showAlert(resourceBundle.getString("enterDataPathAddress"));
            return true;
        } else if (!SessionValidation.validatePort(model.getPortNo().trim())) {
            alert.showAlert(resourceBundle.getString("enterValidPort"));
            return true;
        } else if (!SessionValidation.checkStringEmptyNull(model.getAccessKey())) {
            alert.showAlert(resourceBundle.getString("enterAccessKey"));
            return true;
        } else if (!SessionValidation.checkStringEmptyNull(model.getSecretKey())) {
            alert.showAlert(resourceBundle.getString("enterSecretKey"));
            return true;
        }
        return false;
    }
}
