package com.spectralogic.dsbrowser.gui.components.metadata;

import com.spectralogic.ds3client.networking.Metadata;

public class Ds3Metadata {

    private final Metadata metadata;
    private final long size;
    private final String name;

    public  Ds3Metadata() {
        this(null, 0, "");
    }

    public Ds3Metadata(final Metadata metadata, final long size, final String name) {
        this.metadata = metadata;
        this.size = size;
        this.name = name;
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
}
