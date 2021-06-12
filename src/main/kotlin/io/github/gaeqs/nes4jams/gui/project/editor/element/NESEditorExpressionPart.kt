package io.github.gaeqs.nes4jams.gui.project.editor.element

import net.jamsimulator.jams.gui.mips.editor.element.MIPSLine

class NESEditorExpressionPart(line: MIPSLine, text: String, startIndex: Int, endIndex: Int, val isLabel: Boolean) :
    NESCodeElement(line, text, startIndex, endIndex) {

    override val translatedNameNode: String = "MIPS_ELEMENT_DIRECTIVE_PARAMETER "
    override val simpleText: String = text
    override val styles: List<String> get() = getGeneralStyles("mips-directive-parameter")

    init {


    }
}