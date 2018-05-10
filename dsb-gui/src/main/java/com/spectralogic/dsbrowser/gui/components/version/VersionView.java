package com.spectralogic.dsbrowser.gui.components.version;

import com.airhacks.afterburner.views.FXMLView;

public class VersionView extends FXMLView {
    public VersionView(VersionModel versionModel) {
        super(name -> versionModel);
    }
}
