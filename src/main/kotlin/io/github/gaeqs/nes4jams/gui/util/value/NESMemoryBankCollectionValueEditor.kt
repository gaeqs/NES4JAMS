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

import io.github.gaeqs.nes4jams.memory.NESMemoryBank
import io.github.gaeqs.nes4jams.memory.NESMemoryBankCollection
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.jamsimulator.jams.gui.util.DraggableListCell
import net.jamsimulator.jams.gui.util.value.ValueEditor
import java.util.function.Consumer

class NESMemoryBankCollectionValueEditor : ListView<NESMemoryBank>(), ValueEditor<NESMemoryBankCollection> {

    companion object {
        const val NAME = "nes_memory_bank_collection"
    }

    var listener: Consumer<NESMemoryBankCollection> = Consumer { }
    var current = NESMemoryBankCollection()

    init {
        setCellFactory { Cell() }
    }

    override fun getCurrentValue() = current
    override fun getAsNode() = this
    override fun getLinkedConverter() = null
    override fun buildConfigNode(label: Label) = HBox(label, this).apply { spacing = 5.0; alignment = Pos.CENTER_LEFT }

    override fun setCurrentValue(collection: NESMemoryBankCollection) {
        current = collection
        items.clear()
        items += collection
    }

    override fun addListener(consumer: Consumer<NESMemoryBankCollection>) {
        listener = listener.andThen(consumer)
    }

    private inner class Cell : DraggableListCell<NESMemoryBank>() {

        override fun updateItem(item: NESMemoryBank?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic = if (item == null) null else Label(item.toString())
        }

    }

    class Builder : ValueEditor.Builder<NESMemoryBankCollection> {
        override fun build() = NESMemoryBankCollectionValueEditor()
    }
}