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

import io.github.gaeqs.nes4jams.cpu.directive.NESDirective
import io.github.gaeqs.nes4jams.cpu.directive.defaults.NESDirectiveMacro
import javafx.util.Pair
import net.jamsimulator.jams.gui.editor.code.indexing.EditorIndex
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexedParentElement
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexedParentElementImpl
import net.jamsimulator.jams.gui.editor.code.indexing.element.ElementScope
import net.jamsimulator.jams.gui.mips.editor.indexing.element.MIPSEditorDirectiveMacroName
import net.jamsimulator.jams.gui.mips.editor.indexing.element.MIPSEditorDirectiveMacroParameter
import net.jamsimulator.jams.language.Messages
import net.jamsimulator.jams.utils.StringUtils
import java.util.*

class NESEditorDirective(
    index: EditorIndex,
    scope: ElementScope,
    parent: EditorIndexedParentElement,
    start: Int,
    text: String
) : EditorIndexedParentElementImpl(index, scope, parent, start, text, Messages.MIPS_ELEMENT_DIRECTIVE) {

    val directive: NESDirective?

    override fun getIdentifier() = super.getIdentifier().substring(1)

    init {
        val parts = StringUtils.multiSplitIgnoreInsideStringWithIndex(text, false, " ", ",", "\t")
        if (parts.isNotEmpty()) {
            val stringParameters = parts.toSortedMap()
            val firstKey = stringParameters.firstKey()
            val firstValue = stringParameters[firstKey]!!
            stringParameters.remove(firstKey)
            directive = NESDirective.DIRECTIVES[firstValue.substring(1)]
            elements += NESEditorDirectiveMnemonic(index, scope, this, start + firstKey, firstValue)
            parseParameters(stringParameters)
        } else {
            directive = null
        }
    }


    private fun parseParameters(parameters: SortedMap<Int, String>) {
        if (directive is NESDirectiveMacro) {
            if (parameters.isEmpty()) return
            val mnemonicIndex = parameters.firstKey()
            val mnemonic = parameters[mnemonicIndex]!!
            parameters.remove(mnemonicIndex)

            val sanitized = ArrayList<Pair<Int, String>>(parameters.size)
            var i = 0
            for ((key, value) in parameters) {
                sanityMacroParameter(i++, key, parameters.size, value)?.let { sanitized.add(it) }
            }

            val mnemonicElement = MIPSEditorDirectiveMacroName(
                index,
                scope,
                this,
                start + mnemonicIndex,
                mnemonic,
                sanitized.size,
                parameters.values.toList()
            )
            elements += mnemonicElement

            val macroScope = mnemonicElement.macroScope
            for (parameter in sanitized) {
                elements += MIPSEditorDirectiveMacroParameter(
                    index,
                    macroScope,
                    this,
                    start + parameter.key,
                    parameter.value
                )
            }

        } else {
            parameters.forEach {
                elements += NESEditorExpression(index, scope, this, start + it.key, it.value)
            }
        }
    }

    private fun sanityMacroParameter(index: Int, start: Int, size: Int, parameter: String): Pair<Int, String>? {
        var start = start
        var parameter = parameter
        if (index == 0) {
            if (parameter == "(" || parameter == "()") {
                return null
            }
            if (parameter.startsWith("(")) {
                parameter = parameter.substring(1)
                start++
            }
        }
        if (index == size - 1) {
            if (parameter == ")") return null
            if (parameter.endsWith(")")) {
                parameter = parameter.substring(0, parameter.length - 1)
            }
        }
        return Pair(start, parameter)
    }

}