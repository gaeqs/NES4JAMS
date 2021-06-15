package io.github.gaeqs.nes4jams.gui.project.editor

import io.github.gaeqs.nes4jams.gui.project.editor.element.NESEditorDirective
import io.github.gaeqs.nes4jams.gui.project.editor.element.NESEditorEquivalent
import io.github.gaeqs.nes4jams.gui.project.editor.element.NESEditorInstruction
import io.github.gaeqs.nes4jams.gui.project.editor.element.NESFileElements
import net.jamsimulator.jams.Jams
import net.jamsimulator.jams.configuration.Configuration

class NESCodeFormatter(val elements: NESFileElements) {

    private val tabChar: Char
    private val tabCharNumber: Int
    private val preserveTabs: Boolean
    private val preserveTabsBeforeLabels: Boolean
    private var maxBlankLines: Int

    init {
        val c: Configuration = Jams.getMainConfiguration()
        val useTabs = c.get<Any>("editor.mips.use_tabs").orElse(false) as Boolean
        tabChar = if (useTabs) '\t' else ' '
        tabCharNumber = 4
        preserveTabs = c.get<Any>("editor.mips.preserve_tabs").orElse(false) as Boolean
        preserveTabsBeforeLabels = c.get<Any>("editor.mips.preserve_tabs_before_labels").orElse(false) as Boolean
        maxBlankLines = c.get<Any>("editor.mips.maximum_blank_lines").orElse(0) as Int
        if (maxBlankLines < 0) maxBlankLines = 0
    }

    fun format(): String {
        val builder = StringBuilder()
        var blankLines = 0
        var first = true
        for (line in elements.lines) {
            //CHECK BLANK LINES
            blankLines = if (line.isEmpty) {
                if (!first && blankLines < maxBlankLines) {
                    builder.append('\n')
                }
                blankLines++
                continue
            } else {
                0
            }
            if (first) first = false else builder.append('\n')
            if (preserveTabsBeforeLabels) {
                var amount = line.tabsAmountBeforeLabel
                while (amount > 0) {
                    builder.append(tabChar)
                    amount -= if (tabChar == '\t') 4 else 1
                }
            }
            if (line.label != null) {
                builder.append(line.label.simpleText).append(":")
            }

            var amount = if (preserveTabs) tabCharNumber.coerceAtLeast(line.tabsAmountAfterLabel) else tabCharNumber
            while (amount > 0) {
                builder.append(tabChar)
                amount -= if (tabChar == '\t') 4 else 1
            }

            if (line.instruction != null) formatInstruction(builder, line.instruction)
            if (line.directive != null) formatDirective(builder, line.directive)
            if (line.equivalent != null) formatEquivalent(builder, line.equivalent)

            if (line.comment != null) {
                if (line.directive != null || line.instruction != null) builder.append(" ")
                builder.append(line.comment.simpleText)
            }
        }
        return builder.toString()
    }

    private fun formatInstruction(builder: StringBuilder, instruction: NESEditorInstruction) {
        if (instruction.instruction != null) {
            builder.append(instruction.instruction.mnemonic.lowercase())
        } else {
            builder.append(instruction.simpleText)
        }
        if (instruction.expression != null) {
            builder.append(' ').append(instruction.expression.text)
        }
    }


    private fun formatDirective(builder: StringBuilder, directive: NESEditorDirective) {
        if (directive.directive != null) {
            builder.append(directive.directive.mnemonic.lowercase())
        } else {
            builder.append(directive.simpleText)
        }

        builder.append(' ')

        val iterator = directive.parameters.iterator()
        while (iterator.hasNext()) {
            val parameter = iterator.next()
            builder.append(parameter.text.trim())
            if (iterator.hasNext()) builder.append(' ')
        }
    }

    private fun formatEquivalent(builder: StringBuilder, equivalent: NESEditorEquivalent) {
        builder.append(equivalent.simpleText).append(" =")
        if (equivalent.expression != null) {
            builder.append(' ').append(equivalent.expression.text)
        }
    }

}