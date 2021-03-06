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

import io.github.gaeqs.nes4jams.data.NES4JAMS_BAR_INFO
import io.github.gaeqs.nes4jams.data.NES4JAMS_BAR_PPU
import io.github.gaeqs.nes4jams.gui.simulation.NESSimulationInfo
import io.github.gaeqs.nes4jams.gui.simulation.display.DisplayHolder
import io.github.gaeqs.nes4jams.gui.simulation.display.NESSimulationDisplay
import io.github.gaeqs.nes4jams.gui.simulation.memory.NESMemoryPane
import io.github.gaeqs.nes4jams.gui.simulation.ppu.NESSimulationPPUDisplay
import io.github.gaeqs.nes4jams.project.NESProject
import io.github.gaeqs.nes4jams.simulation.NESSimulation
import io.github.gaeqs.nes4jams.util.managerOfD
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.layout.HBox
import net.jamsimulator.jams.gui.ActionRegion
import net.jamsimulator.jams.gui.action.RegionTags
import net.jamsimulator.jams.gui.bar.BarPosition
import net.jamsimulator.jams.gui.bar.BarSnapshot
import net.jamsimulator.jams.gui.bar.mode.BarSnapshotViewMode
import net.jamsimulator.jams.gui.image.icon.Icons
import net.jamsimulator.jams.gui.mips.simulator.execution.ExecutionButtons
import net.jamsimulator.jams.gui.mips.simulator.execution.SpeedSlider
import net.jamsimulator.jams.gui.project.ProjectTab
import net.jamsimulator.jams.gui.project.SimulationHolder
import net.jamsimulator.jams.gui.project.WorkingPane
import net.jamsimulator.jams.language.Messages
import net.jamsimulator.jams.mips.simulation.Simulation
import tornadofx.*

class NESSimulationPane(parent: Tab, projectTab: ProjectTab, val project: NESProject, val simulation: NESSimulation) :
    WorkingPane(parent, projectTab, null, false), SimulationHolder<Short>, ActionRegion {

    private val executionButtons = ExecutionButtons(simulation)

    val display = NESSimulationDisplay(this)
    val info = NESSimulationInfo(simulation)
    val memory = NESMemoryPane(simulation)
    val ppuDisplay: NESSimulationPPUDisplay

    init {
        center = DisplayHolder(display)

        init()
        loadConsole()
        loadMemory()
        loadInfo()

        ppuDisplay = NESSimulationPPUDisplay(this)
        loadPatternTableDisplay()
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
        display.dispose()
        ppuDisplay.stop()
        info.dispose()
    }

    private fun loadConsole() {
        val console = simulation.log as? Node ?: return
        barMap.registerSnapshot(
            BarSnapshot(
                "console",
                console,
                BarPosition.BOTTOM_LEFT,
                managerOfD<BarSnapshotViewMode>().default,
                true,
                Icons.SIMULATION_CONSOLE,
                Messages.BAR_CONSOLE_NAME,
            )
        )
    }

    private fun loadPatternTableDisplay() {
        barMap.registerSnapshot(
            BarSnapshot(
                "ppu_display",
                ppuDisplay,
                BarPosition.RIGHT_TOP,
                managerOfD<BarSnapshotViewMode>().default,
                true,
                Icons.FILE_IMAGE,
                NES4JAMS_BAR_PPU
            )
        )
    }

    private fun loadMemory() {
        barMap.registerSnapshot(
            BarSnapshot(
                "memory",
                memory,
                BarPosition.LEFT_TOP,
                managerOfD<BarSnapshotViewMode>().default,
                true,
                Icons.SIMULATION_MEMORY,
                Messages.BAR_MEMORY_NAME
            )
        )
    }

    private fun loadInfo() {
        barMap.registerSnapshot(
            BarSnapshot(
                "info",
                info,
                BarPosition.LEFT_BOTTOM,
                managerOfD<BarSnapshotViewMode>().default,
                true,
                Icons.FILE_TEXT,
                NES4JAMS_BAR_INFO
            )
        )
    }

}