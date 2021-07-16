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

import io.github.gaeqs.nes4jams.data.ICON_DRAG
import io.github.gaeqs.nes4jams.gui.util.converter.NESMemoryBankCollectionValueConverter
import io.github.gaeqs.nes4jams.memory.NESMemoryBank
import io.github.gaeqs.nes4jams.memory.NESMemoryBankCollection
import io.github.gaeqs.nes4jams.utils.extension.orNull
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.input.DragEvent
import javafx.scene.layout.HBox
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.gui.image.NearestImageView
import net.jamsimulator.jams.gui.image.icon.Icons
import net.jamsimulator.jams.gui.util.DraggableListCell
import net.jamsimulator.jams.gui.util.value.ValueEditor
import java.util.function.Consumer

class NESMemoryBankCollectionValueEditor : ListView<NESMemoryBank>(), ValueEditor<NESMemoryBankCollection> {

    companion object {
        const val NAME = "nes_memory_bank_collection"
        const val STYLE_CLASS = "nes4jams-memory-bank-collection-value-editor"
        const val ICON_SIZE = 20.0
        private val DRAG_ICON = JamsApplication.getIconManager().getOrLoadSafe(ICON_DRAG).orNull()
    }

    private var listener: Consumer<NESMemoryBankCollection> = Consumer { }
    private var current = NESMemoryBankCollection()

    init {
        styleClass += STYLE_CLASS
        setCellFactory { Cell() }

        // Dummy element. Represents the addition button.
        items += NESMemoryBank(0u, 0u, false)
    }

    private fun refreshValues() {
        // Adds all but the dummy element.
        current = NESMemoryBankCollection(items.subList(0, items.size - 1))
        listener.accept(current)
    }

    override fun getCurrentValue() = current
    override fun getAsNode() = this
    override fun getLinkedConverter() = NESMemoryBankCollectionValueConverter.INSTANCE
    override fun buildConfigNode(label: Label) = HBox(label, this).apply {
        spacing = 5.0
        alignment = Pos.CENTER_LEFT
        this@NESMemoryBankCollectionValueEditor.prefWidthProperty()
            .bind(widthProperty().subtract(label.widthProperty()).subtract(30.0))
    }

    override fun setCurrentValue(collection: NESMemoryBankCollection) {
        current = collection
        items.clear()
        items += collection

        // Dummy element. Represents the addition button.
        items += NESMemoryBank(0u, 0u, false)

        listener.accept(current)
    }

    override fun addListener(consumer: Consumer<NESMemoryBankCollection>) {
        listener = listener.andThen(consumer)
    }

    private inner class Cell : DraggableListCell<NESMemoryBank>() {

        override fun updateItem(item: NESMemoryBank?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic = when {
                item == null -> null
                items.size - 1 == index -> {
                    isDraggable = false
                    createAddButton()
                }
                else -> {
                    isDraggable = true
                    NESMemoryBankValueEditor().apply {
                        children.add(0, NearestImageView(DRAG_ICON, ICON_SIZE, ICON_SIZE))
                        children += createRemoveButton()
                        setCurrentValueUnsafe(item)
                        addListener {
                            items[index] = it
                            refreshValues()
                        }
                    }
                }
            }
        }

        override fun onDragDropped(event: DragEvent?) {
            super.onDragDropped(event)
            refreshValues()
        }

        private fun createRemoveButton(): Button {
            val icon = JamsApplication.getIconManager().getOrLoadSafe(Icons.CONTROL_REMOVE).orElse(null)
            val button = Button(null, NearestImageView(icon, ICON_SIZE, ICON_SIZE))
            button.styleClass += "dark-2-bold-button"
            button.cursor = Cursor.HAND
            button.setOnAction { items.removeAt(index); refreshValues() }
            return button
        }

        private fun createAddButton(): Node {
            val icon = JamsApplication.getIconManager().getOrLoadSafe(Icons.CONTROL_ADD).orElse(null)
            val button = Button(null, NearestImageView(icon, ICON_SIZE, ICON_SIZE))
            button.styleClass += "dark-2-bold-button"
            button.cursor = Cursor.HAND
            button.setOnAction {
                items.add(items.size - 1, NESMemoryBank(0u, 0u, true))
                refreshValues()
            }
            return HBox(button).apply { alignment = Pos.CENTER }
        }

    }

    class Builder : ValueEditor.Builder<NESMemoryBankCollection> {
        override fun build() = NESMemoryBankCollectionValueEditor()
    }
}