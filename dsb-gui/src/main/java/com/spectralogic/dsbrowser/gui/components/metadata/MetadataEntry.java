package com.spectralogic.dsbrowser.gui.components.metadata;

public class MetadataEntry {
    private final String key;
    private final String value;

    public MetadataEntry(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}

