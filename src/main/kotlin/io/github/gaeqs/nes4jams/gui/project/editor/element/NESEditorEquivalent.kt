package io.github.gaeqs.nes4jams.gui.project.editor.element

import io.github.gaeqs.nes4jams.utils.extension.isLabelLegal

class NESEditorEquivalent(line: NESLine, text: String, startIndex: Int, endIndex: Int) :
    NESCodeElement(line, text, startIndex, endIndex) {

    override val translatedNameNode: String = "MIPS_ELEMENT_INSTRUCTION"
    override val simpleText: String
    override val styles: List<String> get() = getGeneralStyles("mips-macro-call-parameter")

    val expression: NESEditorExpression?
    val isKeyLegal: Boolean

    init {
        val index = text.indexOf('=')
        if (index == -1) {
            simpleText = text
            expression = null
            isKeyLegal = false
        } else {
            simpleText = text.substring(0, index).trim()
            this.startIndex += text.indexOf(simpleText)
            val eText = text.substring(index + 1)
            expression = NESEditorExpression(
                line,
                eText,
                startIndex + index + 1,
                startIndex + index + 1 + eText.length
            )
            isKeyLegal = simpleText.isLabelLegal()
        }
    }

    override fun move(offset: Int) {
        super.move(offset)
        expression?.move(offset)
    }
}