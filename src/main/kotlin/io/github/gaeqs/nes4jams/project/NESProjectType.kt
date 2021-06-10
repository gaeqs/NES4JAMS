package io.github.gaeqs.nes4jams.project

import io.github.gaeqs.nes4jams.data.PLUGIN_ICON
import net.jamsimulator.jams.gui.image.icon.IconManager
import net.jamsimulator.jams.project.ProjectType
import java.io.File

class NESProjectType private constructor() : ProjectType<NESProject>(NAME, ICON) {

    companion object {
        const val NAME = "NES"
        val ICON = IconManager.INSTANCE.getOrLoadSafe(PLUGIN_ICON).orElse(null)
        val INSTANCE = NESProjectType();
    }

    init {
        templateBuilders += EmptyNESProjectTemplateBuilder()
    }

    override fun loadProject(folder: File): NESProject {
        return NESProject(folder)
    }
}