package io.github.gaeqs.nes4jams.gui.project.editor.element

import io.github.gaeqs.nes4jams.cpu.directive.NESDirective
import net.jamsimulator.jams.utils.StringUtils

class NESEditorDirective(line: NESLine, text: String, startIndex: Int, endIndex: Int) :
    NESCodeElement(line, text, startIndex, endIndex) {

    override val translatedNameNode: String = "MIPS_ELEMENT_DIRECTIVE"
    override val simpleText: String
    override val styles: List<String> get() = getGeneralStyles("mips-directive")

    val directive: NESDirective?
    val parameters = mutableListOf<NESEditorExpression>()

    init {

        val parts = StringUtils.multiSplitIgnoreInsideStringWithIndex(text, false, " ", ",", "\t")
            .toSortedMap()

        if (parts.isEmpty()) {
            simpleText = ""
            directive = null
        }
        else {
            //The first entry is the directive itself.
            val firstKey = parts.firstKey()
            simpleText = parts[firstKey]!!
            parts.remove(firstKey)

            parts.forEach {
                parameters.add(
                    NESEditorExpression(
                        line,
                        it.value,
                        startIndex + it.key,
                        startIndex + it.key + it.value.length
                    )
                )
            }

            this.startIndex += firstKey
            this.endIndex = startIndex + simpleText.length
            directive = NESDirective.DIRECTIVES[simpleText.substring(1)]
        }
    }

    override fun move(offset: Int) {
        super.move(offset)
        parameters.forEach { it.move(offset) }
    }
}