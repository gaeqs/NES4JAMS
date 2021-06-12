package io.github.gaeqs.nes4jams.gui.project.editor.element

import net.jamsimulator.jams.gui.mips.editor.element.MIPSLine

class NESComment(line: MIPSLine, text: String, startIndex: Int, endIndex: Int) :
    NESCodeElement(line, text, startIndex, endIndex) {

    override val translatedNameNode: String = "MIPS_ELEMENT_COMMENT"
    override val simpleText: String = text
    override val styles: List<String> = listOf("mips-comment")
}