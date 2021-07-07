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

import io.github.gaeqs.nes4jams.utils.extension.getCommentIndex
import net.jamsimulator.jams.gui.editor.EditorHintBar
import net.jamsimulator.jams.utils.LabelUtils
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.*

class NESLine(val elements: NESFileElements, var start: Int, val text: String) {

    val label: NESEditorLabel?
    val instruction: NESEditorInstruction?
    val directive: NESEditorDirective?
    val equivalent: NESEditorEquivalent?
    val comment: NESEditorComment?
    val sortedElements: SortedSet<NESCodeElement>
    val usedLabels: Set<String>

    val isEmpty get() = comment == null && label == null && directive == null && equivalent == null && instruction == null
    val tabsAmountAfterLabel: Int
        get() {
            if (instruction == null && directive == null && comment == null && equivalent == null) return 0
            val text = if (label == null) text else text.trim().substring(label.text.length)
            return calculateTabs(text)
        }
    val tabsAmountBeforeLabel get() = if (label == null) 0 else calculateTabs(label.text)

    init {
        var current = text
        var currentStart = start
        var currentEnd = start + current.length

        // Parse comment
        val commentIndex = current.getCommentIndex()
        if (commentIndex != -1) {
            comment = NESEditorComment(this, current.substring(commentIndex), currentStart + commentIndex, currentEnd)
            currentEnd = start + commentIndex
            current = current.substring(0, commentIndex)
        } else comment = null

        // Parse label
        val labelIndex = LabelUtils.getLabelFinishIndex(current)
        if (labelIndex != -1) {
            label = NESEditorLabel(this, current.substring(0, labelIndex + 1), currentStart, currentStart + labelIndex)
            currentStart += labelIndex + 1
            current = current.substring(labelIndex + 1)
        } else label = null

        // Parse instruction, directive or equivalence
        val trim = current.trim()
        when {
            trim.isEmpty() -> {
                instruction = null
                directive = null
                equivalent = null
            }
            '=' in trim -> {
                instruction = null
                directive = null
                equivalent = NESEditorEquivalent(this, current, currentStart, currentEnd)
            }
            trim[0] == '.' -> {
                instruction = null
                directive = NESEditorDirective(this, current, currentStart, currentEnd)
                equivalent = null
            }
            else -> {
                instruction = NESEditorInstruction(this, current, currentStart, currentEnd)
                directive = null
                equivalent = null
            }
        }

        // Populate sorted elements
        sortedElements = TreeSet { o1, o2 -> o1.startIndex - o2.startIndex }
        populateSortedElements()

        // Populate used labels
        usedLabels = HashSet()
        populateUsedLabels()
    }

    operator fun get(index: Int): NESCodeElement? {
        for (element in sortedElements) {
            val valid = element.startIndex <= index && element.endIndex > index
            if (valid) return element
        }
        return null
    }

    /**
     * Calculates the amount of spaces and tabs that contains the start of this line.
     * Tabs are counted as 4 spaces.
     *
     * @param text the text.
     * @return the amount of spaces and tabs.
     */
    private fun calculateTabs(text: String): Int {
        var c: Char
        var amount = 0
        var index = 0

        while (index < text.length) {
            c = text[index++]
            when (c) {
                '\t' -> amount += 4
                ' ' -> amount++
                else -> return amount
            }
        }

        return amount
    }

    fun move(offset: Int) {
        start += offset
        comment?.move(offset)
        label?.move(offset)
        instruction?.move(offset)
        directive?.move(offset)
        equivalent?.move(offset)
    }

    fun styleLine(lastEnd: Int, spansBuilder: StyleSpansBuilder<Collection<String>>): Int {
        if (sortedElements.isEmpty()) return lastEnd

        try {
            spansBuilder.add(emptyList(), start - lastEnd)
        } catch (exception: Exception) {
            println("Last: $lastEnd")
            println("Start: $start")
            println("Diff: " + (start - lastEnd))
            throw exception
        }

        var newLastEnd = start
        for (element in sortedElements) {
            try {
                newLastEnd = styleElement(spansBuilder, element, newLastEnd)
            } catch (exception: Exception) {
                println("Last: $newLastEnd")
                println("Element: ${element.text}")
                throw exception
            }
        }

        val end = start + text.length
        if (end > newLastEnd) {
            spansBuilder.add(emptyList(), end - newLastEnd)
            newLastEnd = end
        }

        return newLastEnd
    }


    fun refreshHints(bar: EditorHintBar?, index: Int) {
        //TODO
    }

    private fun styleElement(
        spansBuilder: StyleSpansBuilder<Collection<String>>,
        element: NESCodeElement,
        lastEnd: Int
    ): Int {
        if (element.startIndex != lastEnd) {
            spansBuilder.add(emptyList(), element.startIndex - lastEnd)
        }
        spansBuilder.add(element.styles, element.simpleText.length)
        return element.startIndex + element.simpleText.length
    }

    private fun populateSortedElements() {
        val set = sortedElements as TreeSet
        if (comment != null) set.add(comment)
        if (label != null) set.add(label)
        if (instruction != null) {
            set.add(instruction)
            if (instruction.expression != null) set.addAll(instruction.expression.parts)
        }
        if (directive != null) {
            set.add(directive)
            directive.parameters.forEach { set.addAll(it.parts) }
        }
        if (equivalent != null) {
            set.add(equivalent)
            if (equivalent.expression != null) set.addAll(equivalent.expression.parts)
        }
    }

    private fun populateUsedLabels() {
        val set = usedLabels as HashSet
        instruction?.expression?.parts?.forEach {
            if (it.type == NESEditorExpressionPartType.LABEL) set.add(it.simpleText)
        }
        equivalent?.expression?.parts?.forEach {
            if (it.type == NESEditorExpressionPartType.LABEL) set.add(it.simpleText)
        }
        directive?.parameters?.flatMap { it.parts }?.forEach {
            if (it.type == NESEditorExpressionPartType.LABEL) set.add(it.simpleText)
        }
    }

    fun refreshMetadata(nesFileElements: NESFileElements) {

    }

}