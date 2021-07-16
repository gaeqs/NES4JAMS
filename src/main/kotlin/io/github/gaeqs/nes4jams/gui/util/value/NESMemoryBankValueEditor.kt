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

import io.github.gaeqs.nes4jams.data.NES4JAMS_MEMORY_BANK_SIZE
import io.github.gaeqs.nes4jams.data.NES4JAMS_MEMORY_BANK_START
import io.github.gaeqs.nes4jams.data.NES4JAMS_MEMORY_BANK_WRITABLE
import io.github.gaeqs.nes4jams.data.NES4JAMS_MEMORY_BANK_WRITE_ON_CARTRIDGE
import io.github.gaeqs.nes4jams.gui.util.converter.NESMemoryBankValueConverter
import io.github.gaeqs.nes4jams.memory.NESMemoryBank
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import net.jamsimulator.jams.gui.util.value.RangedIntegerValueEditor
import net.jamsimulator.jams.gui.util.value.ValueEditor
import net.jamsimulator.jams.language.wrapper.LanguageLabel
import java.util.function.Consumer

class NESMemoryBankValueEditor(showWritable: Boolean = true) : HBox(), ValueEditor<NESMemoryBank> {
    companion object {
        const val NAME = "nes_memory_bank"
        const val STYLE_CLASS = "nes4jams-memory-bank-value-editor"
    }

    private var listener: Consumer<NESMemoryBank> = Consumer { }
    private var current = NESMemoryBank(0u, 0u, true, true)

    private val start = RangedIntegerValueEditor()
    private val size = RangedIntegerValueEditor()
    private val writable = CheckBox().apply { isSelected = true; cursor = Cursor.HAND }
    private val writeOnCartridge = CheckBox().apply { isSelected = true; cursor = Cursor.HAND }

    init {
        styleClass += STYLE_CLASS
        children.addAll(
            LanguageLabel(NES4JAMS_MEMORY_BANK_START), start,
            LanguageLabel(NES4JAMS_MEMORY_BANK_SIZE), size,
            LanguageLabel(NES4JAMS_MEMORY_BANK_WRITE_ON_CARTRIDGE), writeOnCartridge
        )

        if (showWritable) {
            children.addAll(LanguageLabel(NES4JAMS_MEMORY_BANK_WRITABLE), writable)
        }

        start.currentValue = current.start.toInt()
        size.currentValue = current.size.toInt()

        start.min = 0
        start.max = UShort.MAX_VALUE.toInt()
        size.min = 0
        size.max = UShort.MAX_VALUE.toInt()

        start.addListener { refreshValue() }
        size.addListener { refreshValue() }
        writable.selectedProperty().addListener { _, _, _ -> refreshValue() }
        writeOnCartridge.selectedProperty().addListener { _, _, _ -> refreshValue() }
    }

    private fun refreshValue() {
        current = NESMemoryBank(
            start.currentValue.toUShort(),
            size.currentValue.toUShort(),
            writable.isSelected,
            writeOnCartridge.isSelected
        )
        listener.accept(current)
    }


    override fun getCurrentValue() = current
    override fun getAsNode() = this
    override fun getLinkedConverter() = NESMemoryBankValueConverter.INSTANCE
    override fun buildConfigNode(label: Label) = HBox(label, this).apply { spacing = 5.0; alignment = Pos.CENTER_LEFT }

    override fun setCurrentValue(value: NESMemoryBank) {
        current = value
        start.text = current.start.toString()
        size.text = current.size.toString()
        writable.isSelected = current.writable
        writeOnCartridge.isSelected = current.writeOnCartridge
        listener.accept(current)
    }

    override fun addListener(consumer: Consumer<NESMemoryBank>) {
        listener = listener.andThen(consumer)
    }


    class Builder : ValueEditor.Builder<NESMemoryBank> {
        override fun build() = NESMemoryBankValueEditor()
    }
}