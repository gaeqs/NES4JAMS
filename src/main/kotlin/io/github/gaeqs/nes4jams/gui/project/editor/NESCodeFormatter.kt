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
import net.jamsimulator.jams.Jams
import net.jamsimulator.jams.configuration.Configuration

class NESCodeFormatter(val elements: NESEditorIndex) {

    private val tabChar: Char
    private val tabCharNumber: Int
    private val preserveTabs: Boolean
    private val preserveTabsBeforeLabels: Boolean
    private var maxBlankLines: Int

    init {
        val c: Configuration = Jams.getMainConfiguration().data
        val useTabs = c.get<Any>("editor.mips.use_tabs").orElse(false) as Boolean
        tabChar = if (useTabs) '\t' else ' '
        tabCharNumber = 4
        preserveTabs = c.get<Any>("editor.mips.preserve_tabs").orElse(false) as Boolean
        preserveTabsBeforeLabels = c.get<Any>("editor.mips.preserve_tabs_before_labels").orElse(false) as Boolean
        maxBlankLines = c.get<Any>("editor.mips.maximum_blank_lines").orElse(0) as Int
        if (maxBlankLines < 0) maxBlankLines = 0
    }

    fun format(): String {
        //val builder = StringBuilder()
        //var blankLines = 0
        //var first = true
        //for (line in elements.lines) {
        //    //CHECK BLANK LINES
        //    blankLines = if (line.isEmpty) {
        //        if (!first && blankLines < maxBlankLines) {
        //            builder.append('\n')
        //        }
        //        blankLines++
        //        continue
        //    } else {
        //        0
        //    }
        //    if (first) first = false else builder.append('\n')
        //    if (preserveTabsBeforeLabels) {
        //        var amount = line.tabsAmountBeforeLabel
        //        while (amount > 0) {
        //            builder.append(tabChar)
        //            amount -= if (tabChar == '\t') 4 else 1
        //        }
        //    }
        //    if (line.label != null) {
        //        builder.append(line.label.simpleText).append(":")
        //    }
//
        //    var amount = if (preserveTabs) tabCharNumber.coerceAtLeast(line.tabsAmountAfterLabel) else tabCharNumber
        //    while (amount > 0) {
        //        builder.append(tabChar)
        //        amount -= if (tabChar == '\t') 4 else 1
        //    }
//
        //    if (line.instruction != null) formatInstruction(builder, line.instruction)
        //    if (line.directive != null) formatDirective(builder, line.directive)
        //    if (line.equivalent != null) formatEquivalent(builder, line.equivalent)
//
        //    if (line.comment != null) {
        //        if (line.directive != null || line.instruction != null) builder.append(" ")
        //        builder.append(line.comment.simpleText)
        //    }
        //}
        return ""//builder.toString()
    }

   //private fun formatInstruction(builder: StringBuilder, instruction: NESEditorInstruction) {
   //    if (instruction.instruction != null) {
   //        builder.append(instruction.instruction.mnemonic.lowercase())
   //    } else {
   //        builder.append(instruction.simpleText)
   //    }
   //    if (instruction.expression != null) {
   //        builder.append(' ').append(instruction.expression.text.trim())
   //    }
   //}


   //private fun formatDirective(builder: StringBuilder, directive: NESEditorDirective) {
   //    if (directive.directive != null) {
   //        builder.append('.').append(directive.directive.mnemonic.lowercase())
   //    } else {
   //        builder.append(directive.simpleText)
   //    }

   //    builder.append(' ')

   //    val iterator = directive.parameters.iterator()
   //    while (iterator.hasNext()) {
   //        val parameter = iterator.next()
   //        builder.append(parameter.text.trim())
   //        if (iterator.hasNext()) builder.append(' ')
   //    }
   //}

   //private fun formatEquivalent(builder: StringBuilder, equivalent: NESEditorEquivalent) {
   //    builder.append(equivalent.simpleText.trim()).append(" =")
   //    if (equivalent.expression != null) {
   //        builder.append(' ').append(equivalent.expression.text.trim())
   //    }
   //}

}