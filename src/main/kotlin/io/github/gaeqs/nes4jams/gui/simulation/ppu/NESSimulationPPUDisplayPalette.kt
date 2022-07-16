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

package io.github.gaeqs.nes4jams.gui.simulation.ppu

import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import net.jamsimulator.jams.gui.util.AnchorUtils

class NESSimulationPPUDisplayPalette(val id : Int) : AnchorPane() {

    companion object {
        const val WIDTH = 30.0
        const val HEIGHT = 30.0
        const val SELECTED_STYLE_CLASS = "ppu-selected-palette"
    }

    val rectangles = listOf(
        Rectangle(WIDTH / 2, HEIGHT / 2),
        Rectangle(WIDTH / 2, HEIGHT / 2),
        Rectangle(WIDTH / 2, HEIGHT / 2),
        Rectangle(WIDTH / 2, HEIGHT / 2)
    )

    var selected: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (field) {
                styleClass += SELECTED_STYLE_CLASS
            } else {
                styleClass -= SELECTED_STYLE_CLASS
            }
        }

    init {
        prefWidth = WIDTH
        prefHeight = HEIGHT
        maxWidth = WIDTH
        maxHeight = HEIGHT
        minWidth = WIDTH
        minHeight = HEIGHT
        AnchorUtils.setAnchor(rectangles[0], 0.0, -1.0, 0.0, -1.0)
        AnchorUtils.setAnchor(rectangles[1], 0.0, -1.0, -1.0, 0.0)
        AnchorUtils.setAnchor(rectangles[2], -1.0, 0.0, 0.0, -1.0)
        AnchorUtils.setAnchor(rectangles[3], -1.0, 0.0, -1.0, 0.0)
        rectangles.forEach { it.fill = Color.BLACK }
        children.addAll(rectangles)
    }

}