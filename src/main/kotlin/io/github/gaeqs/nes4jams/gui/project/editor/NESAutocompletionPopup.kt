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

import io.github.gaeqs.nes4jams.cpu.directive.NESDirective
import io.github.gaeqs.nes4jams.cpu.instruction.NESInstruction
import io.github.gaeqs.nes4jams.gui.project.editor.indexing.element.NESEditorDirectiveMnemonic
import io.github.gaeqs.nes4jams.gui.project.editor.indexing.element.NESEditorInstructionMnemonic
import io.github.gaeqs.nes4jams.util.extension.orNull
import javafx.application.Platform
import net.jamsimulator.jams.gui.editor.code.indexing.EditorIndex
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexedElement
import net.jamsimulator.jams.gui.editor.code.popup.AutocompletionPopup
import net.jamsimulator.jams.gui.image.icon.Icons

class NESAutocompletionPopup(display: NESAssemblyFileEditor) : AutocompletionPopup(display) {

    companion object {
        private val ICON_DIRECTIVE = Icons.AUTOCOMPLETION_DIRECTIVE
        private val ICON_INSTRUCTION = Icons.AUTOCOMPLETION_INSTRUCTION
    }

    override fun getDisplay() = super.getDisplay() as NESAssemblyFileEditor

    private val index = display.index
    private var element: EditorIndexedElement? = null

    override fun execute(caretOffset: Int, autocompleteIfOne: Boolean) {
        val caretPosition = display.caretPosition + caretOffset
        if (caretPosition <= 0) return

        try {
            index.lock(false)
            element = index.getElementAt(caretPosition - 1).orElse(null)
            if (element == null) {
                hide()
                return
            }
        } finally {
            index.unlock(false)
        }

        Platform.runLater {
            refreshContents(caretPosition)
            if (isEmpty) {
                hide()
                return@runLater
            }

            if (autocompleteIfOne && size() == 1) {
                hide()
                autocomplete()
            } else {
                val bounds = display.caretBounds.orNull() ?: return@runLater

                var zoomX = display.zoom.zoom.x
                var zoomY = display.zoom.zoom.y

                if (zoomX < 1 || zoomY < 1) {
                    zoomY = 1.0
                    zoomX = 1.0
                } else {
                    zoomX = zoomX * 0.5 + 0.5
                    zoomY = zoomY * 0.5 + 0.5
                }

                scroll.scaleX = zoomX
                scroll.scaleY = zoomY
                show(display, bounds.minX, bounds.minY + 20 * display.zoom.zoom.y)
            }

        }
    }

    override fun refreshContents(caretPosition: Int) {
        try {
            index.lock(false)
            elements.clear()
            val to = caretPosition - element!!.start
            var start = element!!.identifier

            if (to > 0 && to < start.length) {
                start = start.substring(0, to)
            }

            start = when (element) {
                is NESEditorDirectiveMnemonic -> refreshDirectives(start)
                is NESEditorInstructionMnemonic -> refreshInstructionsMacrosAndDirectives(start)
                else -> start
            }

            sortAndShowElements(start)
            if (!isEmpty) {
                selectedIndex = 0
                refreshSelected()
            }
        } finally {
            index.unlock(false)
        }
    }

    override fun autocomplete() {
        if (isEmpty) return
        val replacement = selected.autocompletion
        val caretPosition = display.caretPosition
        if (caretPosition == 0) return


        val element = index.withLockF(false)
        { i: EditorIndex -> i.getElementAt(caretPosition - 1).orElse(null) } ?: return

        if (element.text.substring(0, caretPosition - element.start) == replacement) return
        display.replaceText(element.start + selected.offset, caretPosition, replacement)
    }

    private fun refreshDirectives(start: String): String {
        val directive = start.substring(1).lowercase()
        addElements(
            NESDirective.DIRECTIVES.values.filter { it.mnemonic.lowercase().startsWith(directive) },
            { it.mnemonic },
            { ".${it.mnemonic} " },
            0, ICON_DIRECTIVE
        )
        return directive
    }

    private fun refreshInstructionsMacrosAndDirectives(start: String): String {

        val name = start.lowercase()

        // DIRECTIVES
        addElements(
            NESDirective.DIRECTIVES.values.filter { it.mnemonic.lowercase().startsWith(name) },
            { it.mnemonic },
            { ".${it.mnemonic} " },
            0, ICON_DIRECTIVE
        )

        // INSTRUCTIONS
        addElements(
            NESInstruction.INSTRUCTIONS.values.filter { it.mnemonic.lowercase().startsWith(name) },
            { it.mnemonic.lowercase() },
            { "${it.mnemonic.lowercase()} " },
            0, ICON_INSTRUCTION
        )

        // MACROS
        // TODO macros

        return name
    }


}