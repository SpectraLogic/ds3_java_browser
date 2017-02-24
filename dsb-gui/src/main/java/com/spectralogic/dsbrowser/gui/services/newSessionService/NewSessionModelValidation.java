package com.spectralogic.dsbrowser.gui.services.newSessionService;

import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.components.validation.SessionValidation;
import com.spectralogic.dsbrowser.gui.util.Ds3Alert;
import com.spectralogic.dsbrowser.gui.util.ResourceBundleProperties;
import javafx.scene.control.Alert;

import java.util.ResourceBundle;

public class NewSessionModelValidation {
    private static final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();

    public static boolean validationNewSession(final NewSessionModel model) {
        if (!SessionValidation.checkStringEmptyNull(model.getSessionName())) {
            Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("enterSession"), Alert.AlertType.ERROR);
            return true;
        } else if (!SessionValidation.checkStringEmptyNull(model.getEndpoint())) {
            Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("enterDataPathAddress"), Alert.AlertType.ERROR);
            return true;
        } else if (!SessionValidation.validatePort(model.getPortNo().trim())) {
            Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("enterValidPort"), Alert.AlertType.ERROR);
            return true;
        } else if (!SessionValidation.checkStringEmptyNull(model.getAccessKey())) {
            Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("enterAccessKey"), Alert.AlertType.ERROR);
            return true;
        } else if (!SessionValidation.checkStringEmptyNull(model.getSecretKey())) {
            Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("enterSecretKey"), Alert.AlertType.ERROR);
            return true;
        }
        return false;
    }
}
