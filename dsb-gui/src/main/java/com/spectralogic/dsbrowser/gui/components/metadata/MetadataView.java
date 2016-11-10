package com.spectralogic.dsbrowser.gui.components.metadata;

import com.airhacks.afterburner.views.FXMLView;

public class MetadataView extends FXMLView {
    public MetadataView(final Ds3Metadata ds3Metadata) {
        super(name -> {
            switch (name) {
                case "ds3Metadata":
                    return ds3Metadata;
                default:
                    return null;
            }
        });
    }
}
