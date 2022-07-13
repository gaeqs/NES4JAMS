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

package io.github.gaeqs.nes4jams.gui.project.editor

import io.github.gaeqs.nes4jams.gui.project.editor.indexing.NESEditorIndex
import io.github.gaeqs.nes4jams.project.NESProject
import javafx.application.Platform
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.stage.Popup
import net.jamsimulator.jams.Jams
import net.jamsimulator.jams.gui.editor.code.CodeFileEditor
import net.jamsimulator.jams.gui.editor.code.autocompletion.AutocompletionPopup
import net.jamsimulator.jams.gui.editor.code.autocompletion.view.AutocompletionPopupBasicView
import net.jamsimulator.jams.gui.editor.code.indexing.EditorIndex
import net.jamsimulator.jams.gui.editor.holder.FileEditorTab
import net.jamsimulator.jams.utils.LabelUtils
import org.fxmisc.richtext.event.MouseOverTextEvent
import java.time.Duration

class NESAssemblyFileEditor(tab: FileEditorTab) : CodeFileEditor(tab) {

    companion object {
        @JvmStatic
        val TAB_CONFIG_NODE = "editor.nes.use_tabs"

        @JvmStatic
        val THRESHOLD = 20
    }

    private val popup = Popup().apply {
        isAutoFix = true
        isAutoHide = true
        eventDispatcher = this@NESAssemblyFileEditor.eventDispatcher
    }

    init {
        autocompletionPopup = AutocompletionPopup(
            this, NESAutocompletionPopupController(), AutocompletionPopupBasicView()
        )
        initializePopupListeners()

        applyAutoIndent()
        applyIndentRemoval()
        applyLabelTabRemover()
    }

    fun getNESProject() = project as? NESProject

    override fun getIndex() = super.getIndex() as NESEditorIndex

    override fun generateIndex(): EditorIndex {
        val index = NESEditorIndex(project, tab.file.name)
        val taskExecutor = tab.workingPane.projectTab.project.taskExecutor
        taskExecutor.executeIndexing(index, tab.file.name, text)
        return index
    }

    override fun useTabCharacter(): Boolean = Jams.getMainConfiguration().data.getOrElse(TAB_CONFIG_NODE, false)

    private fun applyIndentRemoval() {
        addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (event.code == KeyCode.BACK_SPACE) {
                if (selection.length > 0) return@addEventFilter
                val currentLine = currentParagraph
                if (currentLine == 0) return@addEventFilter
                val caretPosition = caretColumn
                val text = getParagraph(currentLine).substring(0, caretPosition)
                if (text.isEmpty() || text.isNotBlank()) return@addEventFilter
                replaceText(currentLine, 0, currentLine, caretPosition, "")
                if (event.isControlDown) {
                    // Avoid \n removal.
                    event.consume()
                }
            }
        }
    }

    private fun applyLabelTabRemover() {
        addEventHandler(KeyEvent.KEY_TYPED) { event ->
            if (event.character == ":") {
                val column = caretColumn
                val currentParagraph = currentParagraph
                val text = getParagraph(currentParagraph).text
                val trimmed = text.trim { it <= ' ' }
                if (trimmed.isEmpty()) return@addEventHandler
                val offset = text.indexOf(trimmed[0])
                val label = trimmed.substring(0, trimmed.lastIndexOf(':') + 1) + text.substring(0, offset)
                replaceText(currentParagraph, 0, currentParagraph, column, label)
            }
        }
    }

    private fun applyAutoIndent() {
        addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (event.code == KeyCode.ENTER && !event.isConsumed) {
                val caretPosition = caretPosition
                val currentLine = currentParagraph
                val currentColumn = caretColumn
                var previous = getParagraph(currentLine).text.substring(0, currentColumn)
                val labelIndex = LabelUtils.getLabelFinishIndex(previous)
                if (labelIndex != -1) {
                    previous = previous.substring(labelIndex + 1)
                }
                val builder = StringBuilder()
                for (c in previous.toCharArray()) {
                    if (!Character.isWhitespace(c)) break
                    builder.append(c)
                }
                Platform.runLater { insertText(caretPosition + 1, builder.toString()) }
            }
        }
    }

    private fun initializePopupListeners() {
        mouseOverTextDelay = Duration.ofMillis(300)
        addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN) { event ->
            val index = event.characterIndex
            val optional = this.index.withLockF(false) { it.getElementAt(index) }
            if (optional.isEmpty) return@addEventHandler

            popup.content.setAll(NESHoverInfo(optional.get()))
            val position = event.screenPosition
            popup.show(this, position.x, position.y + 10)
        }

        addEventHandler(MouseEvent.MOUSE_MOVED) { event ->
            val x = event.screenX
            val y = event.screenY

            if (
                x < popup.x - THRESHOLD
                || x > popup.x + popup.width + THRESHOLD
                || y < popup.y - THRESHOLD
                || y > popup.y + popup.height + THRESHOLD
            ) {
                popup.hide()
            }
        }
        addEventFilter(KeyEvent.KEY_PRESSED) { popup.hide() }
    }

}