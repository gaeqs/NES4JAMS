package io.github.gaeqs.nes4jams.gui.project.editor.element

import io.github.gaeqs.nes4jams.cpu.instruction.NESInstruction

class NESEditorEquivalent(line: NESLine, text: String, startIndex: Int, endIndex: Int) :
    NESCodeElement(line, text, startIndex, endIndex) {

    override val translatedNameNode: String = "MIPS_ELEMENT_INSTRUCTION"
    override val simpleText: String
    override val styles: List<String> get() = getGeneralStyles("mips-macro-call-parameter")

    val expression: NESEditorExpression?

    init {
        simpleText = text
        expression = null
        //val index = text.indexOf('=')
        //if(index == -1) {
        //    simpleText = text
        //    expression = null
        //} else {
        //    simpleText = text.indexOf(index)
        //}
//
        //if (text.isNotBlank()) {
        //    val textWithoutStart = text.trimStart()
        //    val instructionStart = text.indexOf(textWithoutStart[0])
        //    val instructionEnd = textWithoutStart.indexOfAny(charArrayOf(' ', '\t'))
//
        //    this.startIndex += instructionStart
//
        //    if (instructionEnd == -1) {
        //        simpleText = textWithoutStart
        //        expression = null
        //    } else {
        //        simpleText = textWithoutStart.substring(0, instructionEnd)
        //        val expressionText = textWithoutStart.substring(instructionEnd + 1)
        //        val expressionStart = this.startIndex + instructionEnd + 1
        //        expression = NESEditorExpression(
        //            line, expressionText,
        //            expressionStart,
        //            expressionStart + expressionText.length
        //        )
        //    }
//
        //    instruction = NESInstruction.INSTRUCTIONS[simpleText.uppercase()]
        //    this.endIndex = this.startIndex + simpleText.length
        //} else {
        //    simpleText = text
        //    instruction = null
        //    expression = null
        //}
    }

    override fun move(offset: Int) {
        super.move(offset)
        expression?.move(offset)
    }
}