/*
 *  MIT License
 *
 *  Copyright (c) 2022 Gael Rial Costas
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

import io.github.gaeqs.nes4jams.util.extension.SELECTED_LANGUAGE
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexStyleableElement
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexedElement
import net.jamsimulator.jams.gui.editor.code.indexing.inspection.InspectionLevel
import net.jamsimulator.jams.language.Messages
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.StyleClassedTextArea

class NESHoverInfo(element: EditorIndexedElement) :
    VirtualizedScrollPane<StyleClassedTextArea>(StyleClassedTextArea()) {

    init {
        element.index.withLock(false) {
            addHeader(element)
            addErrors(element)
            addWarnings(element)
            addFooter(element)
        }

        content.isWrapText = true
        content.isEditable = false
        content.styleClass.addAll("documentation", "code-area", "hover-popup")
        prefWidth = 400.0
        maxHeight = 600.0

        content.totalHeightEstimateProperty().addListener { _, _, new -> prefHeight = new + 10 }
    }


    private fun addHeader(element: EditorIndexedElement) {
        content.appendText("${element.translatedTypeName} ")
        content.append(element.identifier, if (element is EditorIndexStyleableElement) element.styles else emptyList())
    }

    private fun addWarnings(element: EditorIndexedElement) {
        val inspections = element.metadata.inspections
        val warnings = inspections.filter { it.level.ordinal < InspectionLevel.ERROR.ordinal }
        if (warnings.isNotEmpty()) {
            content.append(
                "\n\n${SELECTED_LANGUAGE.getOrDefault(Messages.MIPS_ELEMENT_WARNINGS)}",
                listOf("warning")
            )
            warnings.forEach {
                content.append("\n- ${it.buildMessage()}", emptyList())
            }
        }
    }

    private fun addErrors(element: EditorIndexedElement) {
        val inspections = element.metadata.inspections
        val warnings = inspections.filter { it.level.ordinal >= InspectionLevel.ERROR.ordinal }
        if (warnings.isNotEmpty()) {
            content.append(
                "\n\n${SELECTED_LANGUAGE.getOrDefault(Messages.MIPS_ELEMENT_ERRORS)}",
                listOf("error")
            )
            warnings.forEach {
                content.append("\n- ${it.buildMessage()}", emptyList())
            }
        }
    }

    private fun addFooter(element: EditorIndexedElement) {
        val scope = element.referencedScope
        content.appendText("\n\n")
        content.append(scope.fullName, emptyList())
    }


}