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

package io.github.gaeqs.nes4jams.gui.pcx

import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent

class PCXEditorCanvas(val editor: PCXFileEditor) : PCXVisualizer(editor.image, editor) {

    var primaryColor = 3
    var secondaryColor = 0

    init {
        addEventFilter(MouseEvent.MOUSE_PRESSED) { event ->
            if (event.isControlDown) {
                if (event.button == MouseButton.PRIMARY || event.button == MouseButton.SECONDARY) {
                    pickColor(event)
                }
            } else if ((event.button == MouseButton.PRIMARY || event.button == MouseButton.SECONDARY)) {
                draw(event)
            }
        }

        addEventFilter(MouseEvent.MOUSE_DRAGGED) { event ->
            // Cancels the ScrollPane drag event with the primary key.
            if ((event.button == MouseButton.PRIMARY || event.button == MouseButton.SECONDARY) && !event.isControlDown) {
                draw(event)
                event.consume()
            }
        }
    }

    private fun draw(event: MouseEvent) {
        val pixelX = (event.x * image.header.width / width).toInt()
        val pixelY = (event.y * image.header.height / height).toInt()

        val index = if (event.button == MouseButton.PRIMARY) primaryColor else secondaryColor

        image[pixelX, pixelY] = index
        currentImage.pixelWriter.setArgb(pixelX, pixelY, palette[index])
        redraw()
        editor.tab.isSaveMark = true
    }

    private fun pickColor(event: MouseEvent) {
        val pixelX = (event.x * image.header.width / width).toInt()
        val pixelY = (event.y * image.header.height / height).toInt()
        val color = image[pixelX, pixelY]
        if (event.button == MouseButton.PRIMARY) {
            editor.controls.selectPrimaryColor(color)
        } else {
            editor.controls.selectSecondaryColor(color)
        }
    }
}