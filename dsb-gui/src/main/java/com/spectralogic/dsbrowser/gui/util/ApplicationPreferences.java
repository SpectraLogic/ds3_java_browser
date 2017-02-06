package com.spectralogic.dsbrowser.gui.util;

import java.util.prefs.Preferences;

import static com.spectralogic.dsbrowser.gui.util.StringConstants.*;

public class ApplicationPreferences {

    private static final double DEFAULT_X = 10;
    private static final double DEFAULT_Y = 10;
    private static final double DEFAULT_WIDTH = 800;
    private static final double DEFAULT_HEIGHT = 600;
    private static final Preferences pref = Preferences.userRoot().node(NODE_NAME);
    private static ApplicationPreferences prefInstance;

    private ApplicationPreferences() {
    }

    public static ApplicationPreferences getInstance() {
        if (prefInstance == null) {
            prefInstance = new ApplicationPreferences();
        }
        return prefInstance;
    }

    public double getX() {
        return pref.getDouble(WINDOW_POSITION_X, DEFAULT_X);
    }

    public double getY() {
        return pref.getDouble(WINDOW_POSITION_Y, DEFAULT_Y);
    }

    public double getWidth() {
        return pref.getDouble(WINDOW_WIDTH, DEFAULT_WIDTH);
    }

    public double getHeight() {
        return pref.getDouble(WINDOW_HEIGHT, DEFAULT_HEIGHT);
    }


}
