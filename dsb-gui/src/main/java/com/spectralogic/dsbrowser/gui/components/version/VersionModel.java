package com.spectralogic.dsbrowser.gui.components.version;

import javafx.stage.Stage;

import java.util.List;

public class VersionModel {
    private final String bucket;

    private final List<VersionItem> versionItems;
    private final Stage popup;

    public VersionModel(final String bucket, final List<VersionItem> versionItems, final Stage popup) {
        this.bucket = bucket;
        this.versionItems = versionItems;
        this.popup = popup;
    }

    public String getBucket() {
        return bucket;
    }

    public List<VersionItem> getVersionItems() {
        return versionItems;
    }

    public void closePopup() {
        popup.close();
    }

}
