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

import io.github.gaeqs.nes4jams.cpu.directive.NESDirective
import io.github.gaeqs.nes4jams.cpu.instruction.NESInstruction
import io.github.gaeqs.nes4jams.gui.project.editor.element.NESEditorExpressionPartLabel
import io.github.gaeqs.nes4jams.gui.project.editor.indexing.element.NESEditorDirectiveMnemonic
import io.github.gaeqs.nes4jams.gui.project.editor.indexing.element.NESEditorInstructionMnemonic
import net.jamsimulator.jams.gui.editor.code.autocompletion.AutocompletionCandidate
import net.jamsimulator.jams.gui.editor.code.autocompletion.AutocompletionPopupController
import net.jamsimulator.jams.gui.editor.code.indexing.element.EditorIndexedElement
import net.jamsimulator.jams.gui.editor.code.indexing.element.basic.EditorElementLabel
import net.jamsimulator.jams.gui.image.icon.Icons

class NESAutocompletionPopupController : AutocompletionPopupController() {

    private val instructions = mutableSetOf<AutocompletionCandidate<*>>()
    private val directives = mutableSetOf<AutocompletionCandidate<*>>()

    init {
        NESInstruction.INSTRUCTIONS.forEach { (mnemonic, instruction) ->
            instructions += AutocompletionCandidate(
                instruction, mnemonic, "$mnemonic ", emptyList(), Icons.AUTOCOMPLETION_INSTRUCTION
            )
        }
        NESDirective.DIRECTIVES.forEach { (mnemonic, directive) ->
            directives += AutocompletionCandidate(
                directive,
                ".${mnemonic.lowercase()}",
                ".${mnemonic.lowercase()} ",
                emptyList(),
                Icons.AUTOCOMPLETION_DIRECTIVE
            )
        }
    }

    override fun isCandidateValidForContext(p0: EditorIndexedElement?, p1: AutocompletionCandidate<*>?) = true

    override fun refreshCandidates(context: EditorIndexedElement, caretPosition: Int) {
        candidates.clear()
        when (context) {
            is NESEditorInstructionMnemonic -> {
                candidates.addAll(instructions)
                candidates.addAll(directives)
            }
            is NESEditorDirectiveMnemonic -> candidates.addAll(directives)
            is NESEditorExpressionPartLabel -> addLabels(context)
        }
    }

    private fun addLabels(context: NESEditorExpressionPartLabel) {

        val localLabels = context.index.withLockF(false) { index ->
            index.getReferencedElementsOfType(EditorElementLabel::class.java, context.referencingScope)
        }

        localLabels.forEach {
            candidates.add(
                AutocompletionCandidate(it, it.identifier, it.identifier, emptyList(), Icons.AUTOCOMPLETION_LABEL)
            )
        }

        context.index.globalIndex.ifPresent { index ->
            index.searchReferencedElementsOfType(EditorElementLabel::class.java).forEach {
                candidates.add(
                    AutocompletionCandidate(it, it.identifier, it.identifier, emptyList(), Icons.AUTOCOMPLETION_LABEL)
                )
            }
        }
    }
}