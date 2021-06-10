package io.github.gaeqs.nes4jams.project

import net.jamsimulator.jams.project.ProjectData

class NESProjectData(project: NESProject) : ProjectData(NESProjectType.INSTANCE, project.folder) {
}