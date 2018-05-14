package com.spectralogic.dsbrowser.gui.components.version

import javafx.stage.Stage

class VersionModel(val bucket: String, val versionItems: List<VersionItem>, private val popup: Stage) {
    fun closePopup() = popup.close()
}