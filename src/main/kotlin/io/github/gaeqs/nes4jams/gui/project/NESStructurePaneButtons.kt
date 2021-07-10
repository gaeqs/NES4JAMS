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

import io.github.gaeqs.nes4jams.gui.configuration.NESConfigurationWindow
import io.github.gaeqs.nes4jams.project.NESProject
import io.github.gaeqs.nes4jams.project.configuration.event.NESSimulationConfigurationAddEvent
import io.github.gaeqs.nes4jams.project.configuration.event.NESSimulationConfigurationRemoveEvent
import io.github.gaeqs.nes4jams.project.configuration.event.NESSimulationConfigurationSelectEvent
import io.github.gaeqs.nes4jams.utils.extension.orNull
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.gui.action.defaults.general.GeneralActionAssemble
import net.jamsimulator.jams.gui.image.NearestImageView
import net.jamsimulator.jams.gui.image.icon.Icons
import net.jamsimulator.jams.gui.util.FixedButton

class NESStructurePaneButtons(val project: NESProject) {

    companion object {
        const val BUTTON_ICON_SIZE = 18.0
        const val BUTTON_SIZE = 28.0
        const val BUTTON_STYLE_CLASS = "buttons-hbox-button"
    }

    val nodes: List<Node>
    private val configBox: ComboBox<String>

    init {
        val nodes = mutableListOf<Node>()
        this.nodes = nodes
        this.configBox = ComboBox()

        // Assemble button
        nodes += createAssembleButton()

        // Config box
        configBox.maxWidth = 200.0
        project.data.configurations.forEach { configBox.items += it.name }
        project.data.selectedConfiguration?.let { configBox.selectionModel.select(it.name) }
        configBox.setOnAction { project.data.selectConfiguration(configBox.selectionModel.selectedItem) }
        nodes += configBox

        // Settings button
        nodes += createSettingsButton()

        project.data.registerListeners(this, true)
    }

    private fun createAssembleButton () : Button {
        val icon = JamsApplication.getIconManager().getOrLoadSafe(Icons.PROJECT_ASSEMBLE).orNull()
        val assemble =
            FixedButton("", NearestImageView(icon, BUTTON_ICON_SIZE, BUTTON_ICON_SIZE), BUTTON_SIZE, BUTTON_SIZE)
        assemble.styleClass += BUTTON_STYLE_CLASS
        assemble.setOnAction { GeneralActionAssemble.compileAndShow(project) }
        return assemble
    }

    private fun createSettingsButton () : Button {
        val icon = JamsApplication.getIconManager().getOrLoadSafe(Icons.PROJECT_SETTINGS).orNull()
        val settings =
            FixedButton("", NearestImageView(icon, BUTTON_ICON_SIZE, BUTTON_ICON_SIZE), BUTTON_SIZE, BUTTON_SIZE)
        settings.styleClass += BUTTON_STYLE_CLASS
        settings.setOnAction { NESConfigurationWindow.open(project.data) }
        return settings
    }

    @Listener
    private fun onConfigurationAdd(event: NESSimulationConfigurationAddEvent.After) {
        configBox.items.add(event.configuration.name)
    }

    @Listener
    private fun onConfigurationRemove(event: NESSimulationConfigurationRemoveEvent.After) {
        configBox.items.remove(event.configuration.name)
    }

    @Listener
    private fun onConfigurationSelect(event: NESSimulationConfigurationSelectEvent.After) {
        configBox.selectionModel.select(event.new?.name)
    }

}