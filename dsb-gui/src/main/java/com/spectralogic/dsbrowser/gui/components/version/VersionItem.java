package com.spectralogic.dsbrowser.gui.components.version;

import java.util.Date;
import java.util.UUID;

public class VersionItem {
    private final String key;
    private final Date lastModified;
    private final UUID versionId;
    private final Long size;

    public VersionItem(final String key, final Date lastModified, final Long size, final UUID versionId) {
        this.key = key;
        this.lastModified = lastModified;
        this.versionId = versionId;
        this.size = size;
    }

    public String getName() {
        return key;
    }

    public String getCreated() {
        return lastModified.toString();
    }

    public String getVersionId() {
        return versionId.toString();
    }

    public String getSize() {
        return size.toString();
    }


}
