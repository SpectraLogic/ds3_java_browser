package com.spectralogic.dsbrowser.gui.injector.providers;

import com.google.inject.Provider;

import java.util.Locale;
import java.util.ResourceBundle;

public class ResourceBundleProvider implements Provider<ResourceBundle> {
    @Override
    public ResourceBundle get() {
        return ResourceBundle.getBundle("lang", new Locale("en"));
    }
}
