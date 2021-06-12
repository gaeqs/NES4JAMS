package io.github.gaeqs.nes4jams.gui.project.editor.element

import io.github.gaeqs.nes4jams.cpu.instruction.NESInstruction
import net.jamsimulator.jams.gui.mips.editor.element.MIPSLine

class NESEditorInstruction(line: MIPSLine, text: String, startIndex: Int, endIndex: Int) :
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
            this.endIndex = this.startIndex + simpleText.length
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