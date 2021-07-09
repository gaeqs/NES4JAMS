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
import io.github.gaeqs.nes4jams.gui.project.editor.element.NESCodeElement
import io.github.gaeqs.nes4jams.gui.project.editor.element.NESEditorDirective
import io.github.gaeqs.nes4jams.gui.project.editor.element.NESEditorInstruction
import javafx.application.Platform
import net.jamsimulator.jams.gui.JamsApplication
import net.jamsimulator.jams.gui.editor.popup.AutocompletionPopup
import net.jamsimulator.jams.gui.image.icon.Icons

class NESAutocompletionPopup(display: NESFileEditor) : AutocompletionPopup(display) {

    companion object {
        private val ICON_DIRECTIVE =
            JamsApplication.getIconManager().getOrLoadSafe(Icons.AUTOCOMPLETION_DIRECTIVE).orElse(null)
        private val ICON_INSTRUCTION =
            JamsApplication.getIconManager().getOrLoadSafe(Icons.AUTOCOMPLETION_INSTRUCTION).orElse(null)
    }

    override fun getDisplay() = super.getDisplay() as NESFileEditor

    private val nesElements = display.elements
    private var element: NESCodeElement? = null

    override fun execute(caretOffset: Int, autocompleteIfOne: Boolean) {
        val caretPosition = display.caretPosition + caretOffset
        if (caretPosition <= 0) return
        element = nesElements.getElementAt(caretPosition - 1)
        if (element == null) {
            hide()
            return
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
                val bounds = display.caretBounds.orElse(null) ?: return@runLater

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
        elements.clear()
        val to = caretPosition - element!!.startIndex
        var start = element!!.simpleText

        if (to > 0 && to < start.length) {
            start = start.substring(0, to)
        }

        start = when (element) {
            is NESEditorDirective -> refreshDirectives(start)
            is NESEditorInstruction -> refreshInstructionsMacrosAndDirectives(start)
            else -> start
        }

        sortAndShowElements(start)
        if (!isEmpty) {
            selectedIndex = 0
            refreshSelected()
        }
    }

    override fun autocomplete() {
        if (isEmpty) return
        val replacement = selected.autocompletion
        val caretPosition = display.caretPosition
        if (caretPosition == 0) return
        val element = nesElements.getElementAt(caretPosition - 1) ?: return
        if (element.text.substring(0, caretPosition - element.startIndex) == replacement) return
        display.replaceText(element.startIndex + selected.offset, caretPosition, replacement)
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