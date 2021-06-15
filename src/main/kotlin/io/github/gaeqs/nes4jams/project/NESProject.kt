package io.github.gaeqs.nes4jams.project

import io.github.gaeqs.nes4jams.gui.project.NESStructurePane
import javafx.scene.control.Tab
import net.jamsimulator.jams.gui.project.ProjectTab
import net.jamsimulator.jams.gui.project.WorkingPane
import net.jamsimulator.jams.gui.util.log.Log
import net.jamsimulator.jams.project.BasicProject
import net.jamsimulator.jams.project.ProjectData
import net.jamsimulator.jams.project.ProjectType
import java.io.File

class NESProject(folder: File) : BasicProject(folder, true) {

    override fun generateSimulation(p0: Log?) {
        TODO("Not yet implemented")
    }

    override fun onClose() {
        data.save()
        if (projectTab != null) {
            var pane = projectTab.projectTabPane.workingPane
            if (pane is NESStructurePane) {
            }
        }
    }

    override fun generateMainProjectPane(tab: Tab, projectTab: ProjectTab): WorkingPane =
        NESStructurePane(tab, projectTab, this)

    override fun loadData() {
        data = NESProjectData(this)
        data.load()
    }

    override fun getType(): ProjectType<*> {
        return NESProjectType.INSTANCE
    }

    override fun getData(): NESProjectData {
        return super.getData() as NESProjectData
    }
}