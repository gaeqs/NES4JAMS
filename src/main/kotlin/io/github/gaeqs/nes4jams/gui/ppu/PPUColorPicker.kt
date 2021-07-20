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

package io.github.gaeqs.nes4jams.gui.ppu

import io.github.gaeqs.nes4jams.ppu.PPUColors
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle

class PPUColorPicker : GridPane() {

    companion object {
        const val STYLE_CLASS = "ppu-color-picker"
    }

    val colorProperty = SimpleObjectProperty<Color>(null)

    init {
        styleClass += STYLE_CLASS
        vgap = 4.0
        hgap = 4.0
        for (x in 0..7) {
            for (y in 0..7) {
                val rectangle = Rectangle(15.0, 15.0, PPUColors.COLORS[y * 8 + x])
                add(rectangle, x, y)
                rectangle.setOnMouseClicked { colorProperty.set(PPUColors.COLORS[y * 8 + x]) }
            }
        }
    }
}