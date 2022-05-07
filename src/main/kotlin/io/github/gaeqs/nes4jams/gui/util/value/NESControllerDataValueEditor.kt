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

package io.github.gaeqs.nes4jams.gui.util.value

import io.github.gaeqs.nes4jams.data.NES4JAMS_CONTROLLER_TYPE
import io.github.gaeqs.nes4jams.gui.util.converter.NESControllerDataValueConverter
import io.github.gaeqs.nes4jams.simulation.controller.NESButton
import io.github.gaeqs.nes4jams.simulation.controller.NESControllerData
import io.github.gaeqs.nes4jams.simulation.controller.NESControllerDeviceBuilder
import io.github.gaeqs.nes4jams.simulation.controller.NESKeyboardController
import io.github.gaeqs.nes4jams.util.extension.SELECTED_LANGUAGE
import io.github.gaeqs.nes4jams.util.extension.orNull
import io.github.gaeqs.nes4jams.util.managerOf
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import net.jamsimulator.jams.event.Listener
import net.jamsimulator.jams.gui.util.value.ValueEditor
import net.jamsimulator.jams.gui.util.value.ValueEditors
import net.jamsimulator.jams.language.Language
import net.jamsimulator.jams.language.event.LanguageRefreshEvent
import net.jamsimulator.jams.language.wrapper.LanguageLabel
import tornadofx.*
import java.util.function.Consumer

class NESControllerDataValueEditor : VBox(), ValueEditor<NESControllerData> {

    companion object {
        const val NAME = "nes_controller_data"
        const val HBOX_STYLE_CLASS = "value-editor-hbox"
        const val EXTRA_LANGUAGE_NODE = "NES4JAMS_CONTROLLER_EXTRA_"
    }

    private var listener: Consumer<NESControllerData> = Consumer { }
    private var current = NESControllerData(
        NESKeyboardController.Builder.NAME,
        NESKeyboardController.Builder.DEFAULT_MAPPER,
        NESKeyboardController.Builder.DEFAULT_EXTRA
    )
    private var listenersDisabledCount = 0

    private val typeBox = ComboBox<NESControllerDeviceBuilder>().apply {
        items += managerOf<NESControllerDeviceBuilder>()
        setCellFactory { Cell() }
        buttonCell = Cell()
        selectionModel.select(current.builderInstance)
        selectionModel.selectedItemProperty().addListener { _, _, new ->
            if (listenersDisabledCount > 0) return@addListener
            current = NESControllerData(new.name, new.defaultMapper, new.defaultExtra)
            populateComboBoxes()
            createExtraValues()
            assignComboBoxesData()
            listener.accept(current)
        }
    }

    private val extraEditors = mutableMapOf<String, Node>()
    private val mappingBoxes = mutableMapOf<NESButton, ComboBox<String>>()

    init {
        val typeHBox = HBox()
        typeHBox.styleClass.add(HBOX_STYLE_CLASS)
        typeHBox.children += LanguageLabel(NES4JAMS_CONTROLLER_TYPE)
        typeHBox.children += typeBox
        children += typeHBox

        NESButton.values().forEach { button ->
            val box = HBox()
            box.styleClass.add(HBOX_STYLE_CLASS)
            val label = Label(button.name.lowercase().replaceFirstChar { it.uppercase() } + ":")

            val comboBox = ComboBox<String>()
            mappingBoxes[button] = comboBox

            box.children += label
            box.children += comboBox
            children += box

            comboBox.selectionModel.selectedItemProperty().addListener { _, old, new ->
                if (listenersDisabledCount > 0) return@addListener

                val map = current.mapping.toMutableMap()
                map.remove(old)

                if (new != "-") {
                    map[new] = button
                    current = current.copy(mapping = map)
                    listenersDisabledCount++

                    mappingBoxes.forEach { other ->
                        if (other.value != comboBox && other.value.selectedItem == new) {
                            other.value.selectionModel.select(0)
                        }
                    }

                    listenersDisabledCount--
                }

                current = current.copy(mapping = map)

                listener.accept(current)
            }
        }

        populateComboBoxes()
        createExtraValues()
        assignComboBoxesData()
    }

    override fun getCurrentValue() = current
    override fun getAsNode() = this
    override fun getLinkedConverter() = NESControllerDataValueConverter.INSTANCE
    override fun buildConfigNode(label: Label) = HBox(label, this).apply { spacing = 5.0; alignment = Pos.CENTER_LEFT }

    override fun setCurrentValue(value: NESControllerData) {
        current = value
        listenersDisabledCount++

        val type = typeBox.items.find { it.name == value.builder } ?: typeBox.items.first()
        typeBox.selectionModel.select(type)
        populateComboBoxes()
        createExtraValues()
        assignComboBoxesData()

        listenersDisabledCount--
        listener.accept(current)
    }

    override fun addListener(consumer: Consumer<NESControllerData>) {
        listener = listener.andThen(consumer)
    }


    private fun populateComboBoxes() {
        listenersDisabledCount++
        val values = listOf("-") + (currentValue.builderInstance?.mappingKeys ?: emptyList())
        mappingBoxes.values.forEach {
            it.items.setAll(values)
        }
        listenersDisabledCount--
    }

    private fun assignComboBoxesData() {
        listenersDisabledCount++
        mappingBoxes.values.forEach { it.selectionModel.select(0) }
        currentValue.mapping.forEach { (key, button) ->
            mappingBoxes[button]!!.selectionModel.select(key)
        }
        listenersDisabledCount--
    }

    private fun createExtraValues() {
        extraEditors.forEach { children.remove(it.value) }
        extraEditors.clear()

        typeBox.selectionModel.selectedItem.defaultExtraTypes.forEach { (name, type) ->
            val label = LanguageLabel(EXTRA_LANGUAGE_NODE + name.uppercase())

            val builder = ValueEditors.getByName(type).orNull() ?: return@forEach
            val editor = builder.build()
            val node = editor.buildConfigNode(label)

            current.extra[name]?.let {
                editor.linkedConverter.fromStringSafe(it).ifPresent { value ->
                    editor.currentValue = value
                }
            }

            editor.addListener {
                current = current.copy(extra = current.extra + Pair(name, it.toString()))
                listener.accept(current)
            }

            extraEditors[name] = node
            children += node
        }

    }

    private class Cell : ListCell<NESControllerDeviceBuilder>() {

        private var node: String? = null

        init {
            itemProperty().addListener { _, _, new ->
                node = new?.languageNode
                refreshMessage()
            }
            managerOf<Language>().registerListeners(this, true)
        }

        private fun refreshMessage() = node?.let { text = SELECTED_LANGUAGE.getOrDefault(it) }

        override fun getTypeSelector() = "ListCell"

        @Listener
        private fun onRefresh(event: LanguageRefreshEvent) = refreshMessage()
    }

    class Builder : ValueEditor.Builder<NESControllerData> {
        override fun build() = NESControllerDataValueEditor()
    }
}