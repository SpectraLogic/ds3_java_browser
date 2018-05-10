package com.spectralogic.dsbrowser.gui.components.version;

import java.util.List;

public class VersionModel {
    private final String bucket;

    private final List<VersionItem> versionItems;

    public VersionModel(final String bucket, final List<VersionItem> versionItems) {
        this.bucket = bucket;
        this.versionItems = versionItems;
    }

    public String getBucket() {
        return bucket;
    }

    public List<VersionItem> getVersionItems() {
        return versionItems;
    }


}
