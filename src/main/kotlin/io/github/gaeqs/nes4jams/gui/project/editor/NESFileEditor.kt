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

import io.github.gaeqs.nes4jams.gui.project.NESStructurePane
import io.github.gaeqs.nes4jams.gui.project.editor.element.NESFileElements
import io.github.gaeqs.nes4jams.project.NESProject
import javafx.application.Platform
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Popup
import net.jamsimulator.jams.gui.editor.CodeFileEditor
import net.jamsimulator.jams.gui.editor.FileEditorTab
import net.jamsimulator.jams.utils.StringUtils
import org.fxmisc.richtext.model.PlainTextChange
import kotlin.math.max
import kotlin.math.min

class NESFileEditor(tab: FileEditorTab) : CodeFileEditor(tab) {

    val project: NESProject?
    val elements: NESFileElements

    init {
        val workingPane = tab.workingPane
        if (workingPane is NESStructurePane) {
            project = workingPane.project
            elements = project.data.filesToAssemble.files[tab.file] ?: NESFileElements(project)
        } else {
            project = null
            elements = NESFileElements(null)
        }

        autocompletionPopup = NESAutocompletionPopup(this)
        documentationPopup = null

        applyLabelTabRemover()
        Platform.runLater { index() }
    }


    override fun onTextRefresh(change: PlainTextChange) {
        val added = change.inserted
        val removed = change.removed
        //Check current line.
        var currentLine = elements.lineAt(change.position)
        if (currentLine == -1) {
            index()
            return
        }

        var refresh = elements.editLine(currentLine, getParagraph(currentLine).text)

        //Check next lines.
        val addedLines = StringUtils.charCount(added, '\n', '\r')
        val removedLines = StringUtils.charCount(removed, '\n', '\r')

        val filesToAssemble = elements.filesToAssemble

        if (removedLines == 0 && addedLines == 0) {
            elements.searchForLabelsUpdates(refresh.first)
            if (refresh.second && filesToAssemble != null) {
                filesToAssemble.refreshGlobalLabels()
            } else {
                elements.update(this)
            }
            return
        }

        currentLine++
        val editedLines = min(addedLines, removedLines)
        val linesToAdd = max(0, addedLines - removedLines)
        val linesToRemove = max(0, removedLines - addedLines)

        for (i in 0 until editedLines) {
            val new = elements.editLine(currentLine + i, getParagraph(currentLine + i).text)
            refresh.first.addAll(new.first)
            refresh = Pair(refresh.first, refresh.second or new.second)
        }

        if (linesToRemove > 0) {
            for (i in 0 until linesToRemove) {
                val new = elements.removeLine(currentLine + editedLines, hintBar)
                refresh.first.addAll(new.first)
                refresh = Pair(refresh.first, refresh.second or new.second)
            }
        } else if (linesToAdd > 0) {
            for (i in 0 until linesToAdd) {
                val new = elements.addLine(
                    currentLine + i + editedLines,
                    getParagraph(currentLine + i + editedLines).text, hintBar
                )
                refresh.first.addAll(new.first)
                refresh = Pair(refresh.first, refresh.second or new.second)
            }
        }

        elements.searchForLabelsUpdates(refresh.first)

        if (refresh.second && filesToAssemble != null) {
            filesToAssemble.refreshGlobalLabels()
        } else {
            elements.update(this)
        }
    }

    fun index() {
        elements.refreshAll(text)
        elements.styleAll(this, hintBar)
    }

    fun index(text: String) {
        elements.refreshAll(text)
        elements.styleAll(this, hintBar)
    }

    override fun reformat() {
        enableRefreshEvent(false)
        val reformattedCode = NESCodeFormatter(elements).format()
        val text = text
        if (reformattedCode == text) return
        val oLine = currentParagraph
        val oColumn = caretColumn

        replaceText(0, text.length, reformattedCode)

        val lines = lines

        val newSize = lines.size
        val line = oLine.coerceAtMost(newSize - 1)
        val column = oColumn.coerceAtMost(lines[line].text.length)
        moveTo(line, column)
        val height: Double =
            if (totalHeightEstimateProperty().value == null) 0.0 else totalHeightEstimateProperty().value

        var toPixel = height * line / newSize - layoutBounds.height / 2
        toPixel = max(0.0, min(height, toPixel))

        scrollPane.scrollYBy(toPixel)
        index(reformattedCode)
        tab.isSaveMark = true
        tab.layoutDisplay()
        enableRefreshEvent(true)
    }

    override fun reload() {
        super.reload()
        index()
    }

    override fun applyAutoIndent() {
        addEventHandler(
            KeyEvent.KEY_PRESSED
        ) { event: KeyEvent ->
            if (event.code == KeyCode.ENTER) {
                val caretPosition = caretPosition
                val currentLine: Int = elements.lineAt(caretPosition)
                if (currentLine == -1) return@addEventHandler
                var previous = getParagraph(currentLine - 1).text
                val line = elements.lines[currentLine - 1]
                if (line.label != null) {
                    val label = line.label
                    previous = previous.substring(label.text.length)
                }
                val builder = StringBuilder()
                for (c in previous.toCharArray()) {
                    if (c != '\t' && c != ' ') break
                    builder.append(c)
                }
                Platform.runLater {
                    insertText(
                        caretPosition,
                        builder.toString()
                    )
                }
            }
        }
    }

    private fun applyLabelTabRemover() {
        addEventHandler(
            KeyEvent.KEY_TYPED
        ) { event: KeyEvent ->
            if (event.character == ":") {
                val caretPosition = caretPosition
                val currentParagraph = currentParagraph
                val line = elements.lines[currentParagraph]
                if (line.label == null) return@addEventHandler
                val label = line.label
                if (label.endIndex != caretPosition - 1) return@addEventHandler
                val text = label.text
                var i = 0
                for (c in text.toCharArray()) {
                    if (c != '\t' && c != ' ') break
                    i++
                }
                if (i == 0) return@addEventHandler
                val first = text.substring(0, i)
                val last = text.substring(i)
                replaceText(label.startIndex, label.endIndex + 1, last + first)
            }
        }
    }

}