package io.github.gaeqs.nes4jams.gui.project.editor.element

class NESEditorLabel(line: NESLine, text: String, startIndex: Int, endIndex: Int, var isGlobal: Boolean = false) :
    NESCodeElement(line, text, startIndex, endIndex) {

    override val translatedNameNode: String get() = if (isGlobal) "MIPS_ELEMENT_GLOBAL_LABEL" else "MIPS_ELEMENT_LABEL"
    override val simpleText: String = text.substring(0, text.length - 1).trim()
    override val styles: List<String> get() = getGeneralStyles(if (isGlobal) "mips-global-label" else "mips-label")

    init {
        this.startIndex += text.indexOf(text.trimStart())
    }
}