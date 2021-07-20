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

import io.github.gaeqs.nes4jams.file.pcx.PictureExchangeImage
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ScrollPane
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.paint.Paint
import net.jamsimulator.jams.Jams

class PCXEditorCanvas(val editor: PCXFileEditor) :
    Canvas(editor.image.header.width.toDouble() * DEFAULT_ZOOM, editor.image.header.height.toDouble() * DEFAULT_ZOOM) {

    companion object {
        const val DEFAULT_ZOOM = 15.0
    }

    val image get() = editor.image

    var palette = listOf(0xFF000000.toInt(), 0xFF555555.toInt(), 0xFFAAAAAA.toInt(), 0xFFFFFFFF.toInt())
        set(value) {
            if (value.size != 4) throw IllegalArgumentException("Array must have only 4 elements.")
            field = value
            currentImage = image.toFXImage(PictureExchangeImage.ColorPalette(value.toIntArray()))
            redraw()
        }

    var zoom = DEFAULT_ZOOM
        set(value) {
            if (field == value) return
            field = value
            width = image.header.width * field
            height = image.header.height * field
            redraw()
        }

    var draw8x8Grid = true
        set(value) {
            if (field == value) return
            field = value
            redraw()
        }

    var draw1x1Grid = false
        set(value) {
            if (field == value) return
            field = value
            redraw()
        }

    var primaryColor = 3
    var secondaryColor = 0

    private var currentImage = image.toFXImage(PictureExchangeImage.ColorPalette(palette.toIntArray()))

    init {
        cursor = Cursor.DEFAULT
        val gc = graphicsContext2D
        initScrollPaneZoomListener(editor, gc)

        gc.isImageSmoothing = false

        redraw()

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

    private fun initScrollPaneZoomListener(scrollPane: ScrollPane, gc: GraphicsContext) {
        scrollPane.addEventFilter(ScrollEvent.SCROLL) { event ->
            val sensibility = (Jams.getMainConfiguration().getNumber("editor.zoom_sensibility").orElse(0.2)).toDouble()
            if (event.isControlDown && Jams.getMainConfiguration().get<Boolean>("editor.zoom_using_mouse_wheel")
                    .orElse(true)
            ) {
                zoom = 0.4.coerceAtLeast(zoom + sensibility * event.deltaY * 2.0)
                width = image.header.width * zoom
                height = image.header.height * zoom
                redraw()
                event.consume()
            }
        }
        scrollPane.addEventFilter(MouseEvent.MOUSE_CLICKED) { event ->
            if (event.isControlDown && event.button == MouseButton.MIDDLE && Jams.getMainConfiguration()
                    .get<Any>("editor.reset_zoom_using_middle_button").orElse(true) as Boolean
            ) {
                zoom = 1.0
                width = image.header.width * zoom
                height = image.header.height * zoom
                redraw()
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

    private fun redraw() {
        val gc = graphicsContext2D
        gc.drawImage(currentImage, 0.0, 0.0, width, height)

        if (draw8x8Grid) {
            draw8x8Lines()
        }
        if (draw1x1Grid) {
            draw1x1Lines()
        }
    }

    private fun draw8x8Lines() {
        val gc = graphicsContext2D
        gc.stroke = Paint.valueOf("#555555")
        gc.lineWidth = 2.0
        val imageWidth = image.header.width
        val imageHeight = image.header.height

        var x = 0.0
        while (x < imageWidth) {
            gc.strokeLine(x * zoom, 0.0, x * zoom, height)
            x += 8.0
        }

        var y = 0.0
        while (y < imageHeight) {
            gc.strokeLine(0.0, y * zoom, width, y * zoom)
            y += 8.0
        }
    }

    private fun draw1x1Lines() {
        val gc = graphicsContext2D
        gc.stroke = Paint.valueOf("#555555")
        gc.lineWidth = 1.0
        val imageWidth = image.header.width
        val imageHeight = image.header.height

        var x = 0.0
        while (x < imageWidth) {
            gc.strokeLine(x * zoom, 0.0, x * zoom, height)
            x += 1.0
        }

        var y = 0.0
        while (y < imageHeight) {
            gc.strokeLine(0.0, y * zoom, width, y * zoom)
            y += 1.0
        }
    }

}