package com.spectralogic.dsbrowser.gui.injector.providers;

import com.google.inject.Provider;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;

public class Ds3SessionStoreProvider implements Provider<Ds3SessionStore> {
    @Override
    public Ds3SessionStore get() {
        return new Ds3SessionStore();
    }
}
