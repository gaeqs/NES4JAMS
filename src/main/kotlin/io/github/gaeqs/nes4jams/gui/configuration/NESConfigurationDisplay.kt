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

import io.github.gaeqs.nes4jams.project.configuration.NESSimulationConfiguration
import io.github.gaeqs.nes4jams.project.configuration.NESSimulationConfigurationNodePreset
import io.github.gaeqs.nes4jams.project.configuration.event.NESSimulationConfigurationRefreshEvent
import javafx.event.Event
import javafx.geometry.Insets
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import net.jamsimulator.jams.gui.configuration.ConfigurationRegionDisplay
import net.jamsimulator.jams.gui.util.AnchorUtils
import net.jamsimulator.jams.gui.util.value.ValueEditor
import net.jamsimulator.jams.gui.util.value.ValueEditors
import net.jamsimulator.jams.language.Messages
import net.jamsimulator.jams.language.wrapper.LanguageLabel
import net.jamsimulator.jams.language.wrapper.LanguageTooltip

class NESConfigurationDisplay(val window: NESConfigurationWindow, val configuration: NESSimulationConfiguration) :
    AnchorPane() {

    init {
        AnchorUtils.setAnchor(this, 5.0, 5.0, 5.0, 5.0)

        // Name field
        val nameField = NESConfigurationDisplayNameField(window, configuration)
        AnchorUtils.setAnchor(nameField, 0.0, -1.0, 0.0, 0.0)
        children += nameField

        // General
        val general = NESConfigurationGeneral(configuration)
        AnchorUtils.setAnchor(general, 40.0, 0.0, 0.0, 0.0)
        children += general
    }

}

class NESConfigurationDisplayNameField(
    private val window: NESConfigurationWindow,
    val configuration: NESSimulationConfiguration
) : HBox() {

    companion object {
        const val STYLE_CLASS = "mips-configurations-window-name-field"
    }

    private val textField = TextField(configuration.name)

    init {
        styleClass += STYLE_CLASS
        val label = LanguageLabel(Messages.SIMULATION_CONFIGURATION_NAME)
        textField.setOnAction { handle(it) }
        textField.focusedProperty().addListener { _, _, new -> if (!new) handle(null) }
        textField.prefWidthProperty().bind(widthProperty().subtract(label.widthProperty()).subtract(30))
        children.addAll(label, textField)
    }

    private fun handle(event: Event?) {
        if (window.data.configurations.any { it.name == textField.text }) {
            textField.text = configuration.name
        } else {
            configuration.name = textField.text
            window.list.contents.refreshName(configuration)
            window.data.callEvent(NESSimulationConfigurationRefreshEvent())
        }
    }

}

class NESConfigurationGeneral(val configuration: NESSimulationConfiguration) : VBox() {

    private val representations = configuration.nodes
        .map { (key, value) -> Representation(key, value) }
        .sortedByDescending { it.preset.priority }

    init {
        padding = Insets(5.0)
        spacing = 5.0
        isFillWidth = true
        children += ConfigurationRegionDisplay(Messages.SIMULATION_CONFIGURATION_GENERAL_REGION)
        representations.filter { it.node.isVisible }.forEach { children += it.node }
        representations.forEach { it.refreshEnabled(representations) }
    }

    private fun update(preset: NESSimulationConfigurationNodePreset, value: Any?) {
        configuration.setNodeValue(preset.name, value)
        representations.forEach { it.refreshEnabled(representations) }
    }

    inner class Representation(val preset: NESSimulationConfigurationNodePreset, value: Any?) {

        private val editor: ValueEditor<*> = ValueEditors.getByTypeUnsafe(preset.type.java).build()
        val node = editor.buildConfigNode(LanguageLabel(preset.languageNode)
            .apply { tooltip = LanguageTooltip(preset.languageNode + "_TOOLTIP") })

        init {
            editor.setCurrentValueUnsafe(value)
            editor.addListener { update(preset, it) }
        }

        fun refreshEnabled(representations: Collection<Representation>) {
            isDisabled = representations.any { !preset.supportsNode(it.preset.name, it.editor.currentValue) }
        }
    }

}