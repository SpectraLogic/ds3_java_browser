package com.spectralogic.dsbrowser.gui.services.tasks

import com.spectralogic.dsbrowser.gui.util.Ds3Task

class Ds3GetVersionsTask: Ds3Task<Ds3FileVersions>() {
    override fun call(): Ds3FileVersions {
        return Ds3FileVersions("test", listOf())
    }
}

data class Ds3FileVersions(val name: String, val versions: List<String>)

