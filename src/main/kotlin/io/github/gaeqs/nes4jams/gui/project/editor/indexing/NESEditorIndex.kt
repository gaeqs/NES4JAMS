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

import io.github.gaeqs.nes4jams.gui.project.editor.indexing.element.NESEditorDirectiveMnemonic
import io.github.gaeqs.nes4jams.gui.project.editor.indexing.element.NESEditorExpression
import io.github.gaeqs.nes4jams.gui.project.editor.indexing.inspection.NESInspectorManager
import io.github.gaeqs.nes4jams.util.extension.getEnum
import net.jamsimulator.jams.Jams
import net.jamsimulator.jams.gui.editor.code.indexing.element.ElementScope
import net.jamsimulator.jams.gui.editor.code.indexing.element.basic.EditorElementLabel
import net.jamsimulator.jams.gui.editor.code.indexing.line.EditorLineIndex
import net.jamsimulator.jams.gui.mips.editor.MIPSSpaces
import net.jamsimulator.jams.project.Project

class NESEditorIndex(project: Project, name: String) :
    EditorLineIndex<NESEditorLine>(project, NESInspectorManager.INSTANCE, name) {

    companion object {
        private const val NODE_SPACE_AFTER_DIRECTIVE_PARAMETER = "editor.nes.space_after_directive_parameter"
        private const val NODE_MAX_BLANK_LINES = "editor.nes.maximum_blank_lines"
        private const val NODE_USE_TABS = "editor.nes.use_tabs"
        private const val NODE_PRESERVE_TABS_AFTER_LABEL = "editor.nes.preserve_tabs"
        private const val NODE_PRESERVE_TABS_BEFORE_LABEL = "editor.nes.preserve_tabs_before_labels"

    }


    override fun generateNewLine(start: Int, number: Int, text: String, scope: ElementScope) =
        NESEditorLine(this, scope, start, number, text)

    override fun reformat(): String {
        val builder = StringBuilder()
        val config = Jams.getMainConfiguration()

        val afterDirectiveParameter = config.getEnum<MIPSSpaces>(NODE_SPACE_AFTER_DIRECTIVE_PARAMETER)
            .map { it.value }.orElse(" ")

        val maxBlankLines = config.get<Int>(NODE_MAX_BLANK_LINES).orElse(2)
        val tabText = if (config.get<Boolean>(NODE_USE_TABS).orElse(false)) "\t" else "    "
        val tabsAfterLabel = config.get<Boolean>(NODE_PRESERVE_TABS_AFTER_LABEL).orElse(false)
        val tabsBeforeLabel = config.get<Boolean>(NODE_PRESERVE_TABS_BEFORE_LABEL).orElse(false)

        var blankLineCount = 0

        lines.forEachIndexed { index, line ->
            val tabAccumulator = StringBuilder()
            if (line.isEmpty) blankLineCount++
            else blankLineCount = 0

            if (blankLineCount <= maxBlankLines && index > 0)
                builder.append('\n')

            if (line.label != null) {
                if (tabsBeforeLabel || tabsAfterLabel) {
                    calculateTabsBeforeLabel(line.label, builder, tabAccumulator, tabsBeforeLabel)
                }
                builder.append(line.label.identifier).append(':')
            }

            if (tabsAfterLabel) {
                calculateTabsAfterLabel(line, builder, tabAccumulator)
            } else {
                builder.append(tabText)
            }

            if (line.directive != null) {
                line.directive.elements.forEachIndexed { i, element ->
                    when (element) {
                        is NESEditorDirectiveMnemonic -> builder.append(element.text)
                        is NESEditorExpression -> {
                            if (i == 1) builder.append(' ')
                            else if (i > 1) builder.append(afterDirectiveParameter)
                            builder.append(element.identifier.filter { !it.isWhitespace() })
                        }
                    }
                }
            }

            if (line.instruction != null) {
                val mnemonic = line.instruction.elements[0]
                val expression = line.instruction.elements.getOrNull(1)
                builder.append(mnemonic.identifier)
                if (expression != null) {
                    builder.append(' ')
                    builder.append(expression.identifier.filter { !it.isWhitespace() })
                }
            }

            if (line.equivalent != null) {
                val label = line.equivalent.elements[0]
                val expression = line.equivalent.elements[1]
                builder.append(label.identifier)
                builder.append(" = ")
                builder.append(expression.identifier.filter { !it.isWhitespace() })
            }

            if (line.comment != null) {
                if (line.directive != null || line.instruction != null || line.equivalent != null) {
                    builder.append(tabText)
                }
                builder.append(line.comment.identifier)
            }
        }
        indexAll(builder.toString())
        return builder.toString()
    }

    private fun calculateTabsBeforeLabel(
        label: EditorElementLabel,
        builder: StringBuilder,
        tabAccumulator: StringBuilder,
        tabsBeforeLabel: Boolean
    ) {
        val sub = label.text.substring(0, label.text.indexOf(label.identifier))
        if (tabsBeforeLabel) {
            builder.append(sub)
        } else {
            tabAccumulator.append(sub)
        }
    }

    private fun calculateTabsAfterLabel(line: NESEditorLine, builder: StringBuilder, tabAccumulator: StringBuilder) {
        val dataStart = when {
            line.directive != null -> line.directive.start - line.start
            line.instruction != null -> line.instruction.start - line.start
            line.equivalent != null -> line.equivalent.start - line.start
            line.comment != null -> line.comment.start - line.start
            else -> return
        }

        val tabsStart = line.label?.let { it.end - line.start } ?: 0
        val afterLabel = line.text.substring(tabsStart, dataStart)
        builder.append(tabAccumulator)
        builder.append(afterLabel)
    }
}
