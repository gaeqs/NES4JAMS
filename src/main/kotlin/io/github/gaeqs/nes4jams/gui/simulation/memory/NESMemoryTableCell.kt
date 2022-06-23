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

package io.github.gaeqs.nes4jams.gui.simulation.memory

import io.github.gaeqs.nes4jams.data.BYTE_RANGE
import io.github.gaeqs.nes4jams.util.extension.toHex
import io.github.gaeqs.nes4jams.util.extension.toIntOldWayOrNull
import javafx.scene.control.cell.TextFieldTableCell
import javafx.util.StringConverter

class NESMemoryTableCell(val offset: Int) :
    TextFieldTableCell<NESMemoryTableEntry, String>() {

    init {
        converter = MemoryCellStringConverter()
    }

    override fun cancelEdit() {
        super.cancelEdit()
        val entry = tableRow.item ?: return
        val representation = entry.table.pane.representation
        val address = entry.address + offset.toUInt()
        val item = representation.represent(address, entry.table.pane)
        updateItem(item, false)
    }

    override fun commitEdit(new: String) {
        super.commitEdit(new)

        val entry = tableRow.item ?: return
        val representation = entry.table.pane.representation
        if (representation.requiresNextWord) {
            if (offset != 3) {
                entry.update(offset + 1)
            } else {
                entry.table.entries[(entry.address - 4u)]?.update(0)
            }
        }
    }

    override fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || tableRow == null || tableRow.item == null) {
            text = null
            graphic = null
        } else {
            if (!tableRow.item.table.pane.representation.isColor) {
                text = item
                style = "-fx-background-color: transparent"
            } else {
                text = ""
                style = "-fx-background-color: $item"
            }
        }
    }

    private inner class MemoryCellStringConverter : StringConverter<String>() {

        override fun toString(entry: String?): String {
            if (entry == null) return ""
            if (tableRow == null) return entry
            val item = tableRow.item ?: return entry
            val pane = item.table.pane
            val value = pane.view.read(pane.simulation, item.address + offset.toUInt())
            return "$${value.toHex(2)}"
        }

        override fun fromString(string: String?): String {
            if (string == null) return ""
            val item = tableRow.item ?: return string
            val value = string.toIntOldWayOrNull()
            if (value != null && value in BYTE_RANGE) {
                val pane = item.table.pane
                pane.view.write(pane.simulation, (item.address + offset.toUInt()), value.toUByte())
            } else {
                // Add char
                if (string.length != 1) return string
                val code = string[0].code
                if (code in BYTE_RANGE) {
                    val pane = item.table.pane
                    pane.view.write(pane.simulation, item.address + offset.toUInt(), code.toUByte())
                }
            }

            return string
        }

    }

}

