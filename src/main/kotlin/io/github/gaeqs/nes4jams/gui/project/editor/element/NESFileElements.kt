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

import io.github.gaeqs.nes4jams.gui.project.editor.NESAssemblyFileEditor
import io.github.gaeqs.nes4jams.project.NESFilesToAssemble
import io.github.gaeqs.nes4jams.project.NESProject
import net.jamsimulator.jams.collection.Bag
import net.jamsimulator.jams.gui.editor.EditorHintBar
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.*

class NESFileElements(val project: NESProject?, var filesToAssemble: NESFilesToAssemble? = null) {

    val lines = mutableListOf<NESLine>()
    val labels = Bag<String>()
    private val setAsGlobalLabels = Bag<String>()

    private val equivalents = mutableSetOf<NESEditorEquivalent>()
    private val requiresUpdate = TreeSet<Int>()

    fun isLabelDeclared(label: String): Boolean {
        return label in labels || filesToAssemble?.globalLabels?.contains(label) ?: false
    }

    fun getExistingGlobalLabels(): Set<String> {
        return setAsGlobalLabels.filter { it in labels }.toSet()
    }

    fun getElementAt(index: Int): NESCodeElement? {
        return try {
            lines[lineAt(index)][index]
        } catch (ex: IndexOutOfBoundsException) {
            null
        }
    }

    fun lineAt(position: Int): Int {
        if (position < 0) return -1
        for ((i, line) in lines.withIndex()) {
            if (line.start <= position && line.start + line.text.length >= position) return i
        }
        return -1
    }

    fun removeLine(index: Int, bar: EditorHintBar?): Pair<MutableList<String>, Boolean> {
        require(index in 0 until lines.size) { "Index out of bounds" }
        val line = lines.removeAt(index)
        val length = line.text.length + 1 // + 1 \n
        for (i in index until lines.size) lines[i].move(-length)

        requiresUpdate -= lines.size
        if (line.equivalent != null) removeEquivalent(line.equivalent)
        bar?.applyLineRemoval(index)
        return checkLabels(line, false)
    }

    fun addLine(index: Int, text: String, bar: EditorHintBar?): Pair<MutableList<String>, Boolean> {
        require(index in 0 until lines.size) { "Index out of bounds" }
        require('\n' !in text && '\r' !in text) { "Invalid line!" }

        val start = if (index != 0) {
            val previous = lines[index - 1]
            previous.start + previous.text.length + 1 // + 1 \n
        } else 0

        val line = NESLine(this, start, text)
        val length = text.length + 1
        lines.add(index, line)
        for (i in index + 1 until lines.size) lines[i].move(length)

        if (line.equivalent != null) addEquivalent(line.equivalent)
        requiresUpdate.add(index)
        bar?.applyLineAddition(index)
        return checkLabels(line, true)
    }

    fun editLine(index: Int, text: String): Pair<MutableList<String>, Boolean> {
        require(index in 0 until lines.size) { "Index out of bounds" }
        require('\n' !in text && '\r' !in text) { "Invalid line!" }

        val old = lines[index]
        val difference = text.length - old.text.length
        val line = NESLine(this, old.start, text)
        lines[index] = line
        for (i in index + 1 until lines.size) lines[i].move(difference)

        if (old.equivalent != null) removeEquivalent(old.equivalent)
        if (line.equivalent != null) addEquivalent(line.equivalent)
        requiresUpdate.add(index)

        val a = checkLabels(old, false)
        val b = checkLabels(line, true)
        a.first.addAll(b.first)
        return Pair(a.first, a.second || b.second)
    }


    fun refreshAll(text: String) {
        lines.clear()
        labels.clear()
        setAsGlobalLabels.clear()
        equivalents.clear()
        if (text.isBlank()) return
        var start = 0
        var builder = StringBuilder()

        //Checks all lines
        for ((end, c) in text.toCharArray().withIndex()) {
            if (c == '\n' || c == '\r') {
                refreshAllManageLine(NESLine(this, start, builder.toString()))
                //Restarts the builder.
                builder = StringBuilder()
                start = end + 1
            } else builder.append(c)
        }

        //Final line
        if (text.length >= start) {
            refreshAllManageLine(NESLine(this, start, builder.toString()))
        }


        lines.forEach { it.refreshMetadata(this) }
    }

    private fun refreshAllManageLine(line: NESLine) {
        if (line.label != null) labels += line.label.simpleText

        //if (line.directive != null) {
        //    if (line.directive.directive is NESDirectiveGlobl) {
        //        line.directive.parameters.forEach { setAsGlobalLabels += it.text }
        //    }
        //}

        lines += line
        if (line.equivalent != null) equivalents += line.equivalent
    }

    fun styleAll(area: CodeArea, bar: EditorHintBar) {
        if (lines.isEmpty()) return
        val spansBuilder = StyleSpansBuilder<Collection<String>>()

        var lastEnd = 0
        lines.forEachIndexed { index, line ->
            lastEnd = line.styleLine(lastEnd, spansBuilder)
            line.refreshHints(bar, index)
        }
        requiresUpdate.clear()

        val spans = try {
            spansBuilder.create()
        } catch (ex: IllegalStateException) {
            // No spans have been added.
            return
        }
        area.setStyleSpans(0, spans)
    }

    fun searchForLabelsUpdates(labelsToCheck: Collection<String>) {
        var used: Collection<String?>
        for ((i, nesLine) in lines.withIndex()) {
            used = nesLine.usedLabels
            for (label in labelsToCheck) {
                if (used.contains(label)) {
                    requiresUpdate.add(i)
                    break
                }
            }
            if (nesLine.label != null) {
                if (labelsToCheck.contains(nesLine.label.simpleText)) {
                    requiresUpdate.add(i)
                }
            }
        }
    }

    fun update(editor: NESAssemblyFileEditor) {
        if (requiresUpdate.isEmpty()) return
        val bar = editor.hintBar

        requiresUpdate.forEach {
            if (it < 0 || it >= lines.size) return@forEach
            val builder = StyleSpansBuilder<Collection<String>>()
            val line = lines[it]
            line.refreshMetadata(this)
            line.styleLine(line.start, builder)
            line.refreshHints(bar, it)

            val spans = try {
                builder.create()
            } catch (ex: IllegalStateException) {
                return@forEach
            }

            editor.setStyleSpans(it, 0, spans)
        }
    }


    private fun addEquivalent(equivalent: NESEditorEquivalent) {
        equivalents += equivalent
        if (!equivalent.isKeyLegal) return
        lines.forEachIndexed { index, line ->
            if (line.usedLabels.contains(equivalent.simpleText)) requiresUpdate.add(index)
        }
    }

    fun removeEquivalent(equivalent: NESEditorEquivalent) {
        equivalents -= equivalent
        if (!equivalent.isKeyLegal) return
        lines.forEachIndexed { index, line ->
            if (line.usedLabels.contains(equivalent.simpleText)) requiresUpdate.add(index)
        }
    }

    private fun checkLabels(line: NESLine, add: Boolean): Pair<MutableList<String>, Boolean> {
        val check = mutableListOf<String>()
        var globalLabelUpdated = false

        // LABEl
        if (line.label != null) {
            val label = line.label.simpleText
            if (add) labels += label
            else labels -= label
            check += label

            if (line.label.isGlobal) {
                if (add) setAsGlobalLabels += label
                else setAsGlobalLabels -= label
            }
            globalLabelUpdated = globalLabelUpdated || line.label.isGlobal || label in setAsGlobalLabels
        }

        // DIRECTIVE
        //if (line.directive != null && line.directive.directive is NESDirectiveGlobl) {
        //    line.directive.parameters.forEach {
        //        if (add) setAsGlobalLabels += it.text
        //        else setAsGlobalLabels -= it.text
        //        check += it.text
        //    }
        //    globalLabelUpdated = globalLabelUpdated || line.directive.parameters.isNotEmpty()
        //}

        return Pair(check, globalLabelUpdated)
    }


}