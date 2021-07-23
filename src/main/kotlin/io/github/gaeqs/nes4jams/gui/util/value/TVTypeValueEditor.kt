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

import io.github.gaeqs.nes4jams.cartridge.TVType
import io.github.gaeqs.nes4jams.gui.util.converter.MirrorValueConverter
import io.github.gaeqs.nes4jams.gui.util.converter.TVTypeValueConverter
import io.github.gaeqs.nes4jams.ppu.Mirror
import javafx.geometry.Pos
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.HBox
import net.jamsimulator.jams.gui.util.converter.ValueConverters
import net.jamsimulator.jams.gui.util.value.ValueEditor
import java.util.function.Consumer

class TVTypeValueEditor() : ComboBox<TVType>(), ValueEditor<TVType> {

    companion object {
        const val NAME = "tv_type"
    }

    private var listener: Consumer<TVType> = Consumer { }
    private var current = TVType.NTSC

    init {
        setCellFactory { Cell() }
        buttonCell = Cell()
        converter = ValueConverters.getByTypeUnsafe(TVType::class.java)
        items += TVType.values()
        selectionModel.select(0)
        selectionModel.selectedItemProperty().addListener { _, _, new -> listener.accept(new) }
    }

    override fun getCurrentValue() = current
    override fun getAsNode() = this
    override fun getLinkedConverter() = TVTypeValueConverter.INSTANCE
    override fun buildConfigNode(label: Label) = HBox(label, this).apply { spacing = 5.0; alignment = Pos.CENTER_LEFT }

    override fun setCurrentValue(value: TVType) {
        current = value
        selectionModel.select(value)
        listener.accept(current)
    }

    override fun addListener(consumer: Consumer<TVType>) {
        listener = listener.andThen(consumer)
    }


    class Builder : ValueEditor.Builder<TVType> {
        override fun build() = TVTypeValueEditor()
    }

    private class Cell : ListCell<TVType>() {
        init {
            itemProperty().addListener { _, _, new -> text = new?.toString() }
        }
    }
}