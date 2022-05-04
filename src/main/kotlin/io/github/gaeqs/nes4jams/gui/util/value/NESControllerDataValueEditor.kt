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

import io.github.gaeqs.nes4jams.gui.util.converter.NESControllerDataValueConverter
import io.github.gaeqs.nes4jams.simulation.controller.NESButton
import io.github.gaeqs.nes4jams.simulation.controller.NESControllerData
import io.github.gaeqs.nes4jams.simulation.controller.NESControllerDeviceBuilder
import io.github.gaeqs.nes4jams.simulation.controller.NESKeyboardController
import io.github.gaeqs.nes4jams.util.managerOf
import javafx.geometry.Pos
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import net.jamsimulator.jams.gui.util.value.ValueEditor
import java.util.function.Consumer

class NESControllerDataValueEditor : VBox(), ValueEditor<NESControllerData> {

    companion object {
        const val NAME = "nes_controller_data"
    }

    private var listener: Consumer<NESControllerData> = Consumer { }
    private var current = NESControllerData(NESKeyboardController.Builder.NAME, emptyMap(), emptyMap())
    private var listenersDisabledCount = 0

    private val typeBox = ComboBox<NESControllerDeviceBuilder>().apply {
        items += managerOf<NESControllerDeviceBuilder>()
        setCellFactory { Cell() }
        selectionModel.selectedItemProperty().addListener { _, _, new ->
            if(listenersDisabledCount > 0) return@addListener
            current = NESControllerData(new.name, emptyMap(), emptyMap())
            populateComboBoxes()
            assignComboBoxesData()
            listener.accept(current)
        }
    }

    private val mappingBoxes = mutableMapOf<NESButton, ComboBox<String>>()

    init {
        val typeHBox = HBox()
        typeHBox.children += Label("Type:")
        typeHBox.children += typeBox
        children += typeHBox

        NESButton.values().forEach { button ->
            val box = HBox()
            val label = Label(button.name.lowercase().replaceFirstChar { it.uppercase() })

            val comboBox = ComboBox<String>()
            mappingBoxes[button] = comboBox

            box.children += label
            box.children += comboBox
            children += box

            comboBox.selectionModel.selectedItemProperty().addListener { _, _, new ->
                if(listenersDisabledCount > 0) return@addListener
                current = current.copy(mapping = current.mapping + Pair(new, button))
                listener.accept(current)
            }
        }

        populateComboBoxes()
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

    private class Cell : ListCell<NESControllerDeviceBuilder>() {
        init {
            itemProperty().addListener { _, _, new -> text = new?.toString() }
        }
    }

    class Builder : ValueEditor.Builder<NESControllerData> {
        override fun build() = NESControllerDataValueEditor()
    }
}