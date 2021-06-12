package io.github.gaeqs.nes4jams.gui.project.editor.element

import io.github.gaeqs.nes4jams.utils.extension.DEFAULT_LANGUAGE
import io.github.gaeqs.nes4jams.utils.extension.get
import net.jamsimulator.jams.gui.mips.editor.element.MIPSLine
import net.jamsimulator.jams.language.Language

abstract class NESCodeElement(val line: MIPSLine, val text: String, var startIndex: Int, var endIndex: Int) {

    abstract val translatedNameNode: String
    abstract val simpleText: String
    abstract val styles: List<String>

    val translatedName get() = getTranslatedName(DEFAULT_LANGUAGE)

    fun getTranslatedName(language: Language): String {
        return language[translatedNameNode]
    }

    fun move(offset: Int) {
        startIndex += offset
        endIndex += offset
    }

    fun getGeneralStyles(baseStyle: String): MutableList<String> {
        val list = mutableListOf(baseStyle)
        //if (hasInspections()) {
        //    list.add(if (hasErrors()) "mips-error" else "mips-warning")
        //}
        return list
    }

}