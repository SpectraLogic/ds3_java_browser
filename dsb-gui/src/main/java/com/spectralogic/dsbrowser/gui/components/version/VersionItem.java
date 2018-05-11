package com.spectralogic.dsbrowser.gui.components.version;

import java.util.Date;
import java.util.UUID;

import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormatKt;

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
        final int lastSlash = key.lastIndexOf("/");
        return key.substring(lastSlash+1);
    }

    public String getKey() {
        return key;
    }

    public String getCreated() {
        return new DateTimeUtils().format(lastModified);
    }

    public String getVersionId() {
        return versionId.toString();
    }

    public String getSize() {
        return FileSizeFormatKt.toByteRepresentation(size);
    }


}
