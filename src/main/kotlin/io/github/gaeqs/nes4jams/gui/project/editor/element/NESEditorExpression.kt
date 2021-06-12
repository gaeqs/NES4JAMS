package io.github.gaeqs.nes4jams.gui.project.editor.element

import io.github.gaeqs.nes4jams.utils.RangeCollection
import io.github.gaeqs.nes4jams.utils.extension.indexesOf
import io.github.gaeqs.nes4jams.utils.extension.parseParameterExpressionWithInvalids
import net.jamsimulator.jams.gui.mips.editor.element.MIPSLine

class NESEditorExpression(val line: MIPSLine, val text: String, val startIndex: Int, val endIndex: Int) {

    val parts = mutableListOf<NESEditorExpressionPart>()

    init {
        val (value, invalids) = text.parseParameterExpressionWithInvalids()
        if (value == null || invalids.isEmpty()) {
            parts += NESEditorExpressionPart(line, text, startIndex, endIndex, false)
        } else {
            val ranges = RangeCollection()
            invalids.forEach { invalid ->
                ranges.addAll(text.indexesOf(invalid).map { IntRange(it, it + invalid.length - 1) })
            }

            val inverse = ranges.invert(0, text.length - 1)

            ranges.forEach {
                parts.add(
                    NESEditorExpressionPart(
                        line,
                        text.substring(it),
                        startIndex + it.first,
                        startIndex + it.last - 1,
                        true
                    )
                )
            }

            inverse.forEach {
                parts.add(
                    NESEditorExpressionPart(
                        line,
                        text.substring(it),
                        startIndex + it.first,
                        startIndex + it.last - 1,
                        false
                    )
                )
            }


        }
    }

    fun move(offset: Int) {
        parts.forEach { it.move(offset) }
    }
}