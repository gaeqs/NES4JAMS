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

import io.github.gaeqs.nes4jams.gui.ppu.PPUColorPicker
import io.github.gaeqs.nes4jams.util.extension.ColorFxRGB
import io.github.gaeqs.nes4jams.util.extension.toRGB
import javafx.geometry.Pos
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.shape.Rectangle
import javafx.stage.Popup
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.gui.util.AnchorUtils

class PCXEditorControls(val editor: PCXFileEditor) : HBox() {

    val primaryColor = Rectangle(23.0, 20.0, ColorFxRGB(editor.canvas.palette[editor.canvas.primaryColor]))
    val secondaryColor = Rectangle(20.0, 20.0, ColorFxRGB(editor.canvas.palette[editor.canvas.secondaryColor]))

    val colors = Array(4) { Rectangle(30.0, 30.0, ColorFxRGB(editor.canvas.palette[it])) }

    private var popup: Popup? = null

    init {
        alignment = Pos.CENTER_LEFT
        spacing = 5.0

        val colorsAnchorPane = AnchorPane()
        colorsAnchorPane.prefWidth = 40.0
        colorsAnchorPane.prefHeight = 40.0
        colorsAnchorPane.children.addAll(secondaryColor, primaryColor)
        AnchorUtils.setAnchor(secondaryColor, 0.0, 10.0, 0.0, 10.0)
        AnchorUtils.setAnchor(primaryColor, 10.0, 0.0, 10.0, 00.0)
        children += colorsAnchorPane

        children.addAll(colors)
        colors.forEachIndexed { index, value ->
            value.focusedProperty().addListener { _, _, _ -> popup?.hide(); popup = null }
            value.setOnMouseClicked {
                when (it.button) {
                    MouseButton.PRIMARY -> {
                        if (it.isControlDown) openPopup(value, index, it) else selectPrimaryColor(index)
                    }
                    MouseButton.SECONDARY -> selectSecondaryColor(index)
                    MouseButton.MIDDLE -> openPopup(value, index, it)
                    else -> {
                    }
                }
            }
        }
    }

    fun selectPrimaryColor(index: Int) {
        editor.canvas.primaryColor = index
        primaryColor.fill = ColorFxRGB(editor.canvas.palette[editor.canvas.primaryColor])
    }

    fun selectSecondaryColor(index: Int) {
        editor.canvas.secondaryColor = index
        secondaryColor.fill = ColorFxRGB(editor.canvas.palette[editor.canvas.secondaryColor])
    }

    private fun openPopup(rectangle: Rectangle, index: Int, event: MouseEvent) {
        val popup = Popup()
        val picker = PPUColorPicker()
        popup.content?.add(picker)
        popup.show(JamsApplication.getStage(), event.screenX, event.screenY)
        picker.colorProperty.addListener { _, _, new ->
            rectangle.fill = new
            this.popup?.hide()
            this.popup = null

            val palette = editor.canvas.palette.toMutableList()
            palette[index] = new.toRGB()
            editor.canvas.palette = palette

            if (editor.canvas.primaryColor == index) primaryColor.fill = new
            if (editor.canvas.secondaryColor == index) secondaryColor.fill = new

        }
        rectangle.requestFocus()
        this.popup = popup
    }

}