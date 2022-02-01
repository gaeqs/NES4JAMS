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
import javafx.scene.layout.AnchorPane
import net.jamsimulator.jams.gui.editor.FileEditor
import net.jamsimulator.jams.gui.editor.holder.FileEditorTab
import net.jamsimulator.jams.gui.util.AnchorUtils
import net.jamsimulator.jams.gui.util.PixelScrollPane

class PCXFileEditor(private val tab: FileEditorTab) : PixelScrollPane(), FileEditor {

    val image: PictureExchangeImage

    init {
        val stream = tab.file.inputStream()
        image = PictureExchangeImage(stream)
        stream.close()
    }

    val canvas = PCXEditorCanvas(this)
    val controls = PCXEditorControls(this)

    init {
        isPannable = true
        content = canvas
        controls.prefHeight = 40.0
    }

    override fun supportsActionRegion(region: String): Boolean {
        return "EDITOR_TAB" == region || "EDITOR" == region
    }

    override fun getTab() : FileEditorTab = tab
    override fun onClose() {}
    override fun save() {
        val out = tab.file.outputStream()
        image.write(out)
        out.close()
        tab.isSaveMark = false
    }

    override fun reload() {}

    override fun addNodesToTab(anchor: AnchorPane) {
        anchor.children.addAll(this, controls)
        AnchorUtils.setAnchor(this, 0.0, 40.0, 0.0, 0.0)
        AnchorUtils.setAnchor(controls, -1.0, 00.0, 0.0, 0.0)
    }
}