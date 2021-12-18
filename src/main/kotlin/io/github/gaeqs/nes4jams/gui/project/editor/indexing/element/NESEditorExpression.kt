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

package io.github.gaeqs.nes4jams.gui.project.editor.indexing.element

import io.github.gaeqs.nes4jams.cpu.instruction.NESAddressingMode
import io.github.gaeqs.nes4jams.gui.project.editor.element.NESEditorExpressionPart
import io.github.gaeqs.nes4jams.gui.project.editor.element.NESEditorExpressionPartLabel
import io.github.gaeqs.nes4jams.gui.project.editor.element.NESEditorExpressionPartType
import io.github.gaeqs.nes4jams.util.RangeCollection
import io.github.gaeqs.nes4jams.util.extension.indexesOf
import io.github.gaeqs.nes4jams.util.extension.parseParameterExpressionWithInvalids
import net.jamsimulator.jams.gui.editor.code.indexing.EditorIndex
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexedParentElement
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexedParentElementImpl
import net.jamsimulator.jams.gui.editor.code.indexing.element.ElementScope

class NESEditorExpression(
    index: EditorIndex,
    scope: ElementScope,
    parent: EditorIndexedParentElement,
    start: Int,
    text: String
) : EditorIndexedParentElementImpl(index, scope, parent, start, text) {

    init {
        // First we get the expression.
        val (addressingModes, expression) = NESAddressingMode.getCompatibleAddressingModes(text)

        // If the addressing mode is implied, add the whole text to the parts and return.
        if (NESAddressingMode.IMPLIED in addressingModes) {
            addAllAsAddressingMode()
        } else {
            val expressionStart = start + text.indexOf(expression)
            val expressionEnd = expressionStart + expression.length
            val (value, invalids) = expression.parseParameterExpressionWithInvalids()
            when {
                value == null ->
                    addExpressionAs(expression, expressionStart, NESEditorExpressionPartType.INVALID)
                invalids.isEmpty() ->
                    addExpressionAs(expression, expressionStart, NESEditorExpressionPartType.IMMEDIATE)
                else ->
                    rangeExpression(expression, expressionStart, invalids)
            }

            if (expressionStart > start) {
                addExpressionAs(
                    text.substring(0, expressionStart - start),
                    start,
                    NESEditorExpressionPartType.ADDRESSING_MODE
                )
            }
            if (expressionEnd < start) {
                addExpressionAs(
                    text.substring(expressionEnd - start),
                    expressionEnd,
                    NESEditorExpressionPartType.ADDRESSING_MODE
                )
            }
        }
    }

    private fun addAllAsAddressingMode() {
        elements += NESEditorExpressionPart(
            index, scope, this,
            start, text, NESEditorExpressionPartType.ADDRESSING_MODE
        )
    }

    private fun addExpressionAs(
        expression: String,
        expressionStart: Int,
        type: NESEditorExpressionPartType
    ) {
        elements += NESEditorExpressionPart(index, scope, this, expressionStart, expression, type)
    }

    private fun rangeExpression(expression: String, expressionStart: Int, invalids: Set<String>) {
        val ranges = RangeCollection()
        invalids.forEach { invalid ->
            ranges.addAll(expression.indexesOf(Regex.escape(invalid)).map { IntRange(it, it + invalid.length - 1) })
        }

        val inverse = ranges.invert(0, expression.length - 1)

        ranges.forEach {
            elements += NESEditorExpressionPartLabel(
                index, scope, this,
                expressionStart + it.first, text.substring(it)
            )
        }

        inverse.forEach {
            elements += NESEditorExpressionPart(
                index, scope, this,
                expressionStart + it.first, text.substring(it),
                NESEditorExpressionPartType.IMMEDIATE
            )
        }
    }
}