package com.spectralogic.dsbrowser.gui.components.metadata;


import com.spectralogic.dsbrowser.gui.util.Popup;

public final class MetadataPopup {
    private MetadataPopup() {
        // pass
    }

    public static void show(final Ds3Metadata metadata) {
        final MetadataView view = new MetadataView(metadata);
        Popup.show(view.getView(), "Metadata");
    }
}
