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

import io.github.gaeqs.nes4jams.cpu.instruction.NESInstruction

class NESEditorInstruction(line: NESLine, text: String, startIndex: Int, endIndex: Int) :
    NESCodeElement(line, text, startIndex, endIndex) {

    override val translatedNameNode: String = "MIPS_ELEMENT_INSTRUCTION"
    override val simpleText: String
    override val styles: List<String> get() = getGeneralStyles("mips-instruction")

    val instruction: NESInstruction?
    val expression: NESEditorExpression?

    init {
        if (text.isNotBlank()) {
            val textWithoutStart = text.trimStart()
            val instructionStart = text.indexOf(textWithoutStart[0])
            val instructionEnd = textWithoutStart.indexOfAny(charArrayOf(' ', '\t'))

            this.startIndex += instructionStart

            if (instructionEnd == -1) {
                simpleText = textWithoutStart
                expression = null
            } else {
                simpleText = textWithoutStart.substring(0, instructionEnd)
                val expressionText = textWithoutStart.substring(instructionEnd + 1)
                val expressionStart = this.startIndex + instructionEnd + 1
                expression = NESEditorExpression(
                    line, expressionText,
                    expressionStart,
                    expressionStart + expressionText.length
                )
            }

            instruction = NESInstruction.INSTRUCTIONS[simpleText.uppercase()]
            this.endIndex = this.startIndex + simpleText.length + 1
        } else {
            simpleText = text
            instruction = null
            expression = null
        }
    }

    override fun move(offset: Int) {
        super.move(offset)
        expression?.move(offset)
    }
}