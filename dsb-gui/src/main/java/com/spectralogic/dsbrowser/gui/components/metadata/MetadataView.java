package com.spectralogic.dsbrowser.gui.components.metadata;

import com.airhacks.afterburner.views.FXMLView;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class MetadataView extends FXMLView {
    public MetadataView(final Ds3Metadata ds3Metadata) {
        super(name -> {
            switch (name) {
                case StringConstants.CASE_METADATA:
                    return ds3Metadata;
                default:
                    return null;
            }
        });
    }
}
