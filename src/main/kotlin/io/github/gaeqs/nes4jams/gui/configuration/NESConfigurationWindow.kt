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

package io.github.gaeqs.nes4jams.gui.configuration

import io.github.gaeqs.nes4jams.project.NESProjectData
import io.github.gaeqs.nes4jams.project.configuration.NESSimulationConfiguration
import javafx.scene.control.SplitPane
import javafx.stage.Modality
import javafx.stage.Stage
import net.jamsimulator.jams.Jams
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.gui.image.icon.Icons
import net.jamsimulator.jams.gui.theme.ThemedScene
import net.jamsimulator.jams.language.Messages

class NESConfigurationWindow(val data: NESProjectData) : SplitPane() {

    companion object {
        const val WIDTH = 900.0
        const val HEIGHT = 600.0

        fun open(data: NESProjectData) {
            val window = NESConfigurationWindow(data)
            val scene = ThemedScene(window)
            val stage = Stage()
            val main = JamsApplication.getStage()

            stage.initOwner(main)
            stage.initModality(Modality.APPLICATION_MODAL)
            stage.scene = scene
            stage.width = WIDTH
            stage.height = HEIGHT
            stage.minWidth = WIDTH / 2.0
            stage.minHeight = 0.0

            stage.x = main.x + main.width / 2.0 - WIDTH / 2.0
            stage.y = main.y + main.height / 2.0 - HEIGHT / 2.0
            stage.title = Jams.getLanguageManager().selected.getOrDefault(Messages.SIMULATION_CONFIGURATION_INFO)
            JamsApplication.getIconManager().getOrLoadSafe(Icons.LOGO).ifPresent { stage.icons.add(it) }
            JamsApplication.getActionManager().addAcceleratorsToScene(scene, true)

            stage.setOnCloseRequest { data.save() }
            stage.show()
        }
    }


    init {
    }


    fun display(configuration: NESSimulationConfiguration?) {

    }


}