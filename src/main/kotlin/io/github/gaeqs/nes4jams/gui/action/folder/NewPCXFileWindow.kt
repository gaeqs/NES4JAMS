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

package io.github.gaeqs.nes4jams.gui.action.folder

import io.github.gaeqs.nes4jams.file.pcx.PictureExchangeImage
import javafx.geometry.Pos
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.layout.VBox
import javafx.stage.Stage
import net.jamsimulator.jams.gui.popup.PopupWindowHelper
import net.jamsimulator.jams.language.wrapper.LanguageLabel
import net.jamsimulator.jams.utils.Validate
import java.io.File

class NewPCXFileWindow(stage: Stage, folder: File) : VBox() {

    companion object {
        const val WIDTH = 500
        const val HEIGHT = 50

        fun open(folder: File) {
            val stage = Stage()
            PopupWindowHelper.open(stage, NewPCXFileWindow(stage, folder), WIDTH, HEIGHT, true)
        }
    }

    init {
        Validate.isTrue(folder.isDirectory, "Folder must be a directory!")
        styleClass += "v.box"
        alignment = Pos.CENTER
        children += LanguageLabel("ACTION_FOLDER_EXPLORER_ELEMENT_NEW_PCX_FILE")
        val field = TextField()
        children += field

        field.setOnAction {
            if (field.text.isEmpty()) {
                stage.close()
                return@setOnAction
            }

            val file = File(folder, field.text + ".pcx")
            try {
                if (file.createNewFile()) {
                    val stream = file.outputStream()
                    PictureExchangeImage.createEmpty().write(stream)
                    stream.close()
                    stage.close()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        setOnKeyPressed {
            if (it.code == KeyCode.ESCAPE) {
                stage.close()
                it.consume()
            }
        }
    }

}