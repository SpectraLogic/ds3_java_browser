package com.spectralogic.dsbrowser.gui.components.version;

import com.airhacks.afterburner.views.FXMLView;

class VersionView extends FXMLView {
    VersionView(VersionModel versionModel) {
        super(name -> versionModel);
    }
}
