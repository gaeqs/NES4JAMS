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

package io.github.gaeqs.nes4jams.gui.nes

import io.github.gaeqs.nes4jams.cartridge.CartridgeHeader
import io.github.gaeqs.nes4jams.project.NESProject
import io.github.gaeqs.nes4jams.simulation.NESSimulationData
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.VBox
import net.jamsimulator.jams.gui.editor.FileEditor
import net.jamsimulator.jams.gui.editor.FileEditorTab
import net.jamsimulator.jams.gui.util.AnchorUtils

class NESFileEditor(private val tab: FileEditorTab) : VBox(), FileEditor {

    val header: CartridgeHeader

    init {
        val stream = tab.file.inputStream()
        header = CartridgeHeader(stream)
        stream.close()

        alignment = Pos.CENTER_LEFT

        padding = Insets(5.0)
        children += Label("iNES 2.0: ${header.isINES2}")
        children += Label("Mapper: ${header.mapper}")
        children += Label("Sub-mapper: ${header.subMapper}")
        children += Label("Mirroring: ${header.mirroring}")
        children += Label("TV type: ${header.tvType}")
        children += Label("Console type: ${header.consoleType}")
        children += Label("PRG size: ${header.prgRomSize}")
        children += Label("CHR size: ${header.chrRomSize}")

        children += Button("Run simulation").apply {
            setOnAction {
                val project = tab.workingPane.projectTab.project
                if(project is NESProject) {
                    project.openSimulationForNESFile(tab.file)
                }
            }
        }
    }

    override fun supportsActionRegion(region: String): Boolean {
        return "EDITOR" == region
    }

    override fun getTab() = tab
    override fun onClose() {}
    override fun save() {}

    override fun reload() {}

    override fun addNodesToTab(anchor: AnchorPane) {
        anchor.children.addAll(this)
        AnchorUtils.setAnchor(this, 0.0, 0.0, 0.0, 0.0)
    }
}