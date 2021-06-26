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

package io.github.gaeqs.nes4jams.gui.project.editor.element

import io.github.gaeqs.nes4jams.utils.RangeCollection
import io.github.gaeqs.nes4jams.utils.extension.indexesOf
import io.github.gaeqs.nes4jams.utils.extension.parseParameterExpressionWithInvalids

class NESEditorExpression(val line: NESLine, val text: String, val startIndex: Int, val endIndex: Int) {

    val parts = mutableListOf<NESEditorExpressionPart>()

    init {
        val (value, invalids) = text.parseParameterExpressionWithInvalids()
        if (value == null || invalids.isEmpty()) {
            parts += NESEditorExpressionPart(line, text, startIndex, endIndex, false)
        } else {
            val ranges = RangeCollection()
            invalids.forEach { invalid ->
                ranges.addAll(text.indexesOf(invalid).map { IntRange(it, it + invalid.length - 1) })
            }

            val inverse = ranges.invert(0, text.length - 1)

            ranges.forEach {
                parts.add(
                    NESEditorExpressionPart(
                        line,
                        text.substring(it),
                        startIndex + it.first,
                        startIndex + it.last + 1,
                        true
                    )
                )
            }

            inverse.forEach {
                parts.add(
                    NESEditorExpressionPart(
                        line,
                        text.substring(it),
                        startIndex + it.first,
                        startIndex + it.last + 1,
                        false
                    )
                )
            }


        }
    }

    fun move(offset: Int) {
        parts.forEach { it.move(offset) }
    }
}