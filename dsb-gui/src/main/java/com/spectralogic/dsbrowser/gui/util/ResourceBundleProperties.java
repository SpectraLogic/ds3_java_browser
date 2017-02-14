package com.spectralogic.dsbrowser.gui.util;

import java.util.Locale;
import java.util.ResourceBundle;

public class ResourceBundleProperties {
    private static ResourceBundle resourceBundle;

    public static ResourceBundle getResourceBundle() {
        if(resourceBundle == null) {
            resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));
        }
        return resourceBundle;
    }
}
