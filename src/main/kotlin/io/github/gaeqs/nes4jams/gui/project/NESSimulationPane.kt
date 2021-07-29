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

package io.github.gaeqs.nes4jams.gui.project

import io.github.gaeqs.nes4jams.gui.simulation.display.NESSimulationDisplay
import io.github.gaeqs.nes4jams.project.NESProject
import io.github.gaeqs.nes4jams.simulation.NESSimulation
import io.github.gaeqs.nes4jams.util.extension.fit
import io.github.gaeqs.nes4jams.util.extension.orNull
import javafx.scene.control.ScrollPane
import javafx.scene.control.Tab
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import net.jamsimulator.jams.gui.ActionRegion
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.gui.action.RegionTags
import net.jamsimulator.jams.gui.bar.BarPosition
import net.jamsimulator.jams.gui.bar.BarSnapshot
import net.jamsimulator.jams.gui.bar.mode.BarSnapshotViewModePane
import net.jamsimulator.jams.gui.image.icon.Icons
import net.jamsimulator.jams.gui.mips.simulator.execution.ExecutionButtons
import net.jamsimulator.jams.gui.mips.simulator.execution.SpeedSlider
import net.jamsimulator.jams.gui.project.ProjectTab
import net.jamsimulator.jams.gui.project.SimulationHolder
import net.jamsimulator.jams.gui.project.WorkingPane
import net.jamsimulator.jams.language.Messages
import net.jamsimulator.jams.mips.simulation.Simulation
import tornadofx.clear

class NESSimulationPane(parent: Tab, projectTab: ProjectTab, val project: NESProject, val simulation: NESSimulation) :
    WorkingPane(parent, projectTab, null, false), SimulationHolder<Short>, ActionRegion {

    private val executionButtons = ExecutionButtons(simulation)

    val display: NESSimulationDisplay

    init {

        display = NESSimulationDisplay(simulation)
        val border = BorderPane(display)
        display.fitWidth = 200.0
        display.fitHeight = 200.0


        // The scroll pane allows to resize the node easily
        val scroll = ScrollPane(border).fit()
        scroll.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        scroll.vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER

        scroll.widthProperty().addListener { _, _, new -> display.fitToSize(new.toDouble(), scroll.height) }
        scroll.heightProperty().addListener { _, _, new -> display.fitToSize(scroll.width, new.toDouble()) }

        center = scroll

        init()
        loadConsole()
    }

    override fun getLanguageNode() = Messages.PROJECT_TAB_SIMULATION
    override fun getSimulation(): Simulation<out Short> = simulation
    override fun supportsActionRegion(region: String?) = RegionTags.MIPS_SIMULATION == region

    override fun populateHBox(buttonsHBox: HBox) {
        buttonsHBox.clear()
        buttonsHBox.children += SpeedSlider(simulation)
        buttonsHBox.children += executionButtons.nodes
    }

    override fun saveAllOpenedFiles() {
    }

    override fun onClose() {
        super.onClose()
        simulation.destroy()
        display.stop()
    }

    private fun loadConsole() {
        val console = simulation.console ?: return
        val icon = JamsApplication.getIconManager().getOrLoadSafe(Icons.SIMULATION_CONSOLE).orNull()
        barMap.registerSnapshot(
            BarSnapshot(
                "console",
                console,
                BarPosition.BOTTOM_LEFT,
                BarSnapshotViewModePane.INSTANCE,
                true,
                icon,
                Messages.BAR_CONSOLE_NAME,
            )
        )
    }

}