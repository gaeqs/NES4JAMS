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

import net.jamsimulator.jams.gui.editor.code.indexing.EditorIndex
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexStyleableElement
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexedElementImpl
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexedParentElement
import net.jamsimulator.jams.gui.editor.code.indexing.element.ElementScope
import net.jamsimulator.jams.gui.editor.code.indexing.element.basic.EditorElementLabel
import net.jamsimulator.jams.gui.editor.code.indexing.element.reference.EditorElementReference
import net.jamsimulator.jams.gui.editor.code.indexing.element.reference.EditorReferencingElement

enum class NESEditorExpressionPartType(val style: String) {
    LABEL("mips-label"),
    IMMEDIATE("mips-instruction-parameter-immediate"),
    ADDRESSING_MODE("mips-instruction-parameter-register"),
    INVALID("mips-error")
}


open class NESEditorExpressionPart(
    index: EditorIndex,
    scope: ElementScope,
    parent: EditorIndexedParentElement,
    start: Int,
    text: String,
    val type: NESEditorExpressionPartType
) : EditorIndexedElementImpl(index, scope, parent, start, text), EditorIndexStyleableElement {

    private val style = setOf(type.style)
    override fun getStyles() = style

}

class NESEditorExpressionPartLabel(
    index: EditorIndex,
    scope: ElementScope,
    parent: EditorIndexedParentElement,
    start: Int,
    text: String,
) : NESEditorExpressionPart(index, scope, parent, start, text, NESEditorExpressionPartType.LABEL),
    EditorReferencingElement<EditorElementLabel> {

    private val references = setOf(EditorElementReference(EditorElementLabel::class.java, identifier))
    override fun getReferences() = references

}