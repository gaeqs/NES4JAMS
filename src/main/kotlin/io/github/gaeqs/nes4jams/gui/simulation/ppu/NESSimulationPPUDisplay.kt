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

package io.github.gaeqs.nes4jams.gui.simulation.ppu

import io.github.gaeqs.nes4jams.data.NES4JAMS_PPU_NAME_TABLES
import io.github.gaeqs.nes4jams.data.NES4JAMS_PPU_PATTERN_TABLES
import io.github.gaeqs.nes4jams.gui.project.NESSimulationPane
import io.github.gaeqs.nes4jams.gui.simulation.display.BasicDisplay
import io.github.gaeqs.nes4jams.gui.simulation.display.DisplayHolder
import io.github.gaeqs.nes4jams.simulation.event.NESSimulationRenderEvent
import javafx.geometry.Pos
import javafx.scene.control.TabPane
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.gui.util.AnchorUtils
import net.jamsimulator.jams.language.wrapper.LanguageTab
import net.jamsimulator.jams.mips.simulation.event.SimulationStopEvent

class NESSimulationPPUDisplay(val pane: NESSimulationPane) : AnchorPane() {

    companion object {
        const val PATTERN_TABLE_SIZE = 128
        const val MIRRORING_WIDTH = 64 * 8
        const val MIRRORING_HEIGHT = 60 * 8
    }

    private val worker = NESSimulationPPUDisplayWorker(this).apply { isDaemon = true }
    private val array = UByteArray(0x4000)

    val patternTables = listOf(
        BasicDisplay(PATTERN_TABLE_SIZE, PATTERN_TABLE_SIZE),
        BasicDisplay(PATTERN_TABLE_SIZE, PATTERN_TABLE_SIZE)
    )

    val nameTables = BasicDisplay(MIRRORING_WIDTH, MIRRORING_HEIGHT)

    val palettes = listOf(
        NESSimulationPPUDisplayPalette(0),
        NESSimulationPPUDisplayPalette(1),
        NESSimulationPPUDisplayPalette(2),
        NESSimulationPPUDisplayPalette(3),
        NESSimulationPPUDisplayPalette(4),
        NESSimulationPPUDisplayPalette(5),
        NESSimulationPPUDisplayPalette(6),
        NESSimulationPPUDisplayPalette(7)
    )

    var selectedPalette = palettes[0].apply { selected = true }
        set(value) {
            if (field == value) return
            field.selected = false
            field = value
            field.selected = true
        }

    init {
        pane.simulation.registerListeners(this, true)
        worker.start()

        val tabPane = TabPane()

        // PATTERN TABLES

        val patternTablesTab = LanguageTab(NES4JAMS_PPU_PATTERN_TABLES).apply { isClosable = false }
        val patternTablesRoot = VBox().apply {
            alignment = Pos.CENTER
            isFillWidth = true
        }

        patternTablesTab.content = patternTablesRoot
        patternTables.forEach {
            val holder = DisplayHolder(it)
            holder.prefWidthProperty().bind(patternTablesRoot.widthProperty())
            holder.prefHeightProperty().bind(patternTablesRoot.heightProperty().multiply(0.5))
            patternTablesRoot.children += holder
        }

        // FULL SCENE

        val fullSceneTab = LanguageTab(NES4JAMS_PPU_NAME_TABLES).apply { isClosable = false }

        val fullSceneHolder = DisplayHolder(nameTables)
        fullSceneHolder.prefWidthProperty().bind(tabPane.widthProperty())
        fullSceneHolder.prefHeightProperty().bind(tabPane.heightProperty())

        fullSceneTab.content = fullSceneHolder

        // TAB PANE

        tabPane.tabs += patternTablesTab
        tabPane.tabs += fullSceneTab
        children += tabPane

        // PALETTES

        palettes.forEach { it.setOnMouseClicked { _ -> selectedPalette = it } }

        val hBox = HBox()
        hBox.children.addAll(palettes)
        hBox.alignment = Pos.CENTER
        hBox.spacing = 5.0

        AnchorUtils.setAnchor(hBox, -1.0, 0.0, 0.0, 0.0)

        heightProperty().addListener { obs, old, new ->
            hBox.prefHeight = new.toDouble() * 0.1
            AnchorUtils.setAnchor(tabPane, 0.0, new.toDouble() * 0.1, 0.0, 0.0)
        }

        children += hBox
    }

    fun stop() {
        worker.stopWorker()
        patternTables.forEach { it.dispose() }
    }

    @Listener
    private fun onSimulationRender(event: NESSimulationRenderEvent) {
        pushCurrentPPU()
    }

    @Listener
    private fun onSimulationStop(event: SimulationStopEvent) {
        // Render at stop too! This allows us to see changes on mid-render.
        pushCurrentPPU()
    }

    private fun pushCurrentPPU() {
        val ppu = pane.simulation.ppu
        worker.generateAndPushIfPossible {
            repeat(0x4000) { array[it] = ppu.ppuRead(it.toUShort()) }
            array
        }
    }

}