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

package io.github.gaeqs.nes4jams.gui.project.editor.indexing

import io.github.gaeqs.nes4jams.gui.project.editor.indexing.element.NESEditorDirective
import io.github.gaeqs.nes4jams.gui.project.editor.indexing.element.NESEditorEquivalent
import io.github.gaeqs.nes4jams.gui.project.editor.indexing.element.NESEditorInstruction
import io.github.gaeqs.nes4jams.gui.project.editor.indexing.element.NESEditorLabel
import io.github.gaeqs.nes4jams.util.extension.getCommentIndex
import net.jamsimulator.jams.gui.editor.code.indexing.EditorIndex
import net.jamsimulator.jams.gui.editor.code.indexing.element.ElementScope
import net.jamsimulator.jams.gui.editor.code.indexing.element.basic.EditorElementComment
import net.jamsimulator.jams.gui.editor.code.indexing.element.basic.EditorElementLabel
import net.jamsimulator.jams.gui.editor.code.indexing.element.line.EditorIndexedLine
import net.jamsimulator.jams.utils.LabelUtils
import java.util.*

class NESEditorLine(index: EditorIndex, scope: ElementScope, start: Int, number: Int, text: String) :
    EditorIndexedLine(index, scope, start, number, text) {

    val label: EditorElementLabel?
    val instruction: NESEditorInstruction?
    val directive: NESEditorDirective?
    val equivalent: NESEditorEquivalent?
    val comment: EditorElementComment?

    override fun isMacroStart() = false
    override fun isMacroEnd() = false
    override fun canBeReferencedByALabel() =
        instruction != null || directive != null && directive.directive?.providesAddress == true

    override fun getDefinedMacroScope(): Optional<ElementScope> = Optional.empty()

    init {
        var current = text
        var currentStart = start

        // Parse comment
        val commentIndex = current.getCommentIndex()
        if (commentIndex != -1) {
            comment = EditorElementComment(
                index, scope, this, currentStart + commentIndex,
                current.substring(commentIndex)
            )
            elements += comment
            current = current.substring(0, commentIndex)
        } else comment = null

        // Parse label
        val labelIndex = LabelUtils.getLabelFinishIndex(current)
        if (labelIndex != -1) {
            label = NESEditorLabel(
                index, scope, this, currentStart,
                current.substring(0, labelIndex + 1)
            )
            elements += label
            currentStart += labelIndex + 1
            current = current.substring(labelIndex + 1)
        } else label = null

        // Parse instruction, directive or equivalence
        val trim = current.trim()
        currentStart += current.indexOf(trim)
        when {
            trim.isEmpty() -> {
                instruction = null
                directive = null
                equivalent = null
            }
            '=' in trim -> {
                instruction = null
                directive = null
                equivalent = NESEditorEquivalent(index, scope, this, currentStart, trim)
                elements += equivalent
            }
            trim[0] == '.' -> {
                instruction = null
                directive = NESEditorDirective(index, scope, this, currentStart, trim)
                equivalent = null
                elements += directive
            }
            else -> {
                instruction = NESEditorInstruction(index, scope, this, currentStart, trim)
                directive = null
                equivalent = null
                elements += instruction
            }
        }
    }
}