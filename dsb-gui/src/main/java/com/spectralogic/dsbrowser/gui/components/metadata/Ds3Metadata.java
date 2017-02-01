package com.spectralogic.dsbrowser.gui.components.metadata;

import com.spectralogic.ds3client.networking.Metadata;

public class Ds3Metadata {

    private final Metadata metadata;
    private final long size;
    private final String name;
    private final String lastModified;

    public Ds3Metadata() {
        this(null, 0, "","");
    }

    public Ds3Metadata(final Metadata metadata, final long size, final String name,final String lastModified) {
        this.metadata = metadata;
        this.size = size;
        this.name = name;
        this.lastModified = lastModified;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public String getLastModified() {
        return lastModified;
    }
}
