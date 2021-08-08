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

import io.github.gaeqs.nes4jams.util.extension.toHex
import javafx.beans.property.SimpleStringProperty

class NESMemoryTableEntry(val table: NESMemoryTable, val address: UShort) {

    val addressProperty by lazy { SimpleStringProperty(this, "address", "$${address.toHex(4)}") }

    private val lazies = arrayOf(
        lazy { SimpleStringProperty(this, "0", represent(address)) },
        lazy { SimpleStringProperty(this, "1", represent(address, 1u)) },
        lazy { SimpleStringProperty(this, "2", represent(address, 2u)) },
        lazy { SimpleStringProperty(this, "3", represent(address, 3u)) }
    )

    val property0 by lazies[0]
    val property1 by lazies[1]
    val property2 by lazies[2]
    val property3 by lazies[3]

    fun propertyByIndex(index: Int): SimpleStringProperty? {
        return when (index) {
            0 -> property0
            1 -> property1
            2 -> property2
            3 -> property3
            else -> null
        }
    }

    fun refresh() {
        repeat(4) { update((address + it.toUInt()).toUShort(), it) }
    }

    fun update(offset: Int) {
        update((address + offset.toUInt()).toUShort(), offset)
    }

    fun update(address: UShort, offset: Int) {
        lazies[offset].takeIf { it.isInitialized() }?.value?.set(represent(address))
    }

    private fun represent(address: UShort, offset: UShort = 0u): String {
        return table.pane.representation.represent((address + offset).toUShort(), table.pane)
    }
}