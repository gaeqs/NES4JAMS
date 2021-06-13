package io.github.gaeqs.nes4jams.gui.project.editor.element

class NESEditorExpressionPart(line: NESLine, text: String, startIndex: Int, endIndex: Int, val isLabel: Boolean) :
    NESCodeElement(line, text, startIndex, endIndex) {

    override val translatedNameNode: String = "MIPS_ELEMENT_INSTRUCTION_PARAMETER_IMMEDIATE"
    override val simpleText: String = text
    override val styles: List<String> get() = getGeneralStyles(if (isLabel) "mips-directive-parameter" else "mips-instruction-parameter-immediate")

}