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

import io.github.gaeqs.nes4jams.gui.util.converter.NESMemoryBankCollectionValueConverter
import io.github.gaeqs.nes4jams.memory.NESMemoryBank
import io.github.gaeqs.nes4jams.memory.NESMemoryBankCollection
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.input.DragEvent
import javafx.scene.layout.HBox
import net.jamsimulator.jams.gui.util.DraggableListCell
import net.jamsimulator.jams.gui.util.value.ValueEditor
import java.util.function.Consumer

class NESMemoryBankCollectionValueEditor : ListView<NESMemoryBank>(), ValueEditor<NESMemoryBankCollection> {

    companion object {
        const val NAME = "nes_memory_bank_collection"
        const val STYLE_CLASS = "nes4jams-memory-bank-collection-value-editor"
    }

    private var listener: Consumer<NESMemoryBankCollection> = Consumer { }
    private var current = NESMemoryBankCollection()

    init {
        styleClass += STYLE_CLASS
        setCellFactory { Cell() }
    }

    private fun refreshValues() {
        current = NESMemoryBankCollection(items)
        listener.accept(current)
    }

    override fun getCurrentValue() = current
    override fun getAsNode() = this
    override fun getLinkedConverter() = NESMemoryBankCollectionValueConverter.INSTANCE
    override fun buildConfigNode(label: Label) = HBox(label, this).apply {
        spacing = 5.0;
        alignment = Pos.CENTER_LEFT
        this@NESMemoryBankCollectionValueEditor.prefWidthProperty()
            .bind(widthProperty().subtract(label.widthProperty()).subtract(30.0))
    }

    override fun setCurrentValue(collection: NESMemoryBankCollection) {
        current = collection
        items.clear()
        items += collection
        listener.accept(current)
    }

    override fun addListener(consumer: Consumer<NESMemoryBankCollection>) {
        listener = listener.andThen(consumer)
    }

    private inner class Cell : DraggableListCell<NESMemoryBank>() {

        override fun updateItem(item: NESMemoryBank?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic =
                if (item == null) null else NESMemoryBankValueEditor(drag = true).apply {
                    setCurrentValueUnsafe(item)
                    this.addListener {
                        items[index] = it
                        refreshValues()
                    }
                }
        }

        override fun onDragDropped(event: DragEvent?) {
            super.onDragDropped(event)
            refreshValues()
        }

    }

    class Builder : ValueEditor.Builder<NESMemoryBankCollection> {
        override fun build() = NESMemoryBankCollectionValueEditor()
    }
}