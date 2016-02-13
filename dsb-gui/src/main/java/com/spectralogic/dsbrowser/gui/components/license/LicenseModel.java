package com.spectralogic.dsbrowser.gui.components.license;

public class LicenseModel {
    private final String libraryName;
    private final String license;

    public LicenseModel(final String libraryName, final String license) {
        this.libraryName = libraryName;
        this.license = license;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public String getLicense() {
        return license;
    }
}
