package io.github.gaeqs.nes4jams.project

import net.jamsimulator.jams.project.FilesToAssemble
import net.jamsimulator.jams.project.FilesToAssemblerHolder
import net.jamsimulator.jams.project.ProjectData

class NESProjectData(project: NESProject) :
    ProjectData(NESProjectType.INSTANCE, project.folder), FilesToAssemblerHolder {

    val filesToAssemble = NESFilesToAssemble(project)

    override fun getFilesToAssemble() : FilesToAssemble = filesToAssemble

    override fun load() {
        if (loaded) return
        super.load()
        filesToAssemble.load(metadataFolder)
    }

    override fun save() {
        filesToAssemble.save(metadataFolder)
        super.save()
    }
}