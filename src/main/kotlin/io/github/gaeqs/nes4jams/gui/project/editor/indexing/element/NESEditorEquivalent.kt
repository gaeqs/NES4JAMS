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

import io.github.gaeqs.nes4jams.data.NES4JAMS_ELEMENT_EQUIVALENT
import net.jamsimulator.jams.gui.editor.code.indexing.EditorIndex
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexedParentElement
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexedParentElementImpl
import net.jamsimulator.jams.gui.editor.code.indexing.element.ElementScope

class NESEditorEquivalent(
    index: EditorIndex,
    scope: ElementScope,
    parent: EditorIndexedParentElement,
    start: Int,
    text: String
) : EditorIndexedParentElementImpl(index, scope, parent, start, text, NES4JAMS_ELEMENT_EQUIVALENT) {

    override fun getIdentifier() = super.getIdentifier().substring(1)

    init {
        val eqIndex = text.indexOf('=')
        if (eqIndex != -1) {
            val mnemonic = text.substring(0, eqIndex)
            val expression = text.substring(eqIndex + 1)
            elements += NESEditorLabel(index, scope, this, start, mnemonic)
            elements += NESEditorExpression(index, scope, this, start + eqIndex + 1, expression)
        }
    }

}