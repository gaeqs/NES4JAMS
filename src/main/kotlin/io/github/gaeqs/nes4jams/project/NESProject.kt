/*
 *  MIT License
 *
 *  Copyright (c) 2021 Gael Rial Costas
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package io.github.gaeqs.nes4jams.project

import io.github.gaeqs.nes4jams.gui.project.NESStructurePane
import javafx.scene.control.Tab
import net.jamsimulator.jams.gui.project.ProjectTab
import net.jamsimulator.jams.gui.project.WorkingPane
import net.jamsimulator.jams.gui.util.log.Log
import net.jamsimulator.jams.project.BasicProject
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