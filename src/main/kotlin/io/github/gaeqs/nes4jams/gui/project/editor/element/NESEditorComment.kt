package io.github.gaeqs.nes4jams.gui.project.editor.element

class NESEditorComment(line: NESLine, text: String, startIndex: Int, endIndex: Int) :
    NESCodeElement(line, text, startIndex, endIndex) {

    override val translatedNameNode = "MIPS_ELEMENT_COMMENT"
    override val simpleText = text
    override val styles = listOf("mips-comment")
}