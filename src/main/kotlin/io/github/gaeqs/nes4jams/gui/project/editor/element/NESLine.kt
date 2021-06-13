package io.github.gaeqs.nes4jams.gui.project.editor.element

import io.github.gaeqs.nes4jams.utils.extension.getCommentIndex
import net.jamsimulator.jams.gui.mips.editor.element.MIPSFileElements
import net.jamsimulator.jams.utils.LabelUtils

class NESLine(val elements: MIPSFileElements, val start: Int, val text: String) {

    val label: NESEditorLabel?
    val instruction: NESEditorInstruction?
    val directive: NESEditorDirective?
    val comment: NESEditorComment?

    init {
        var current = text
        var currentStart = start
        var currentEnd = start + current.length

        // Parse comment
        val commentIndex = current.getCommentIndex()
        if (commentIndex != -1) {
            comment = NESEditorComment(this, current.substring(commentIndex), currentStart + commentIndex, currentEnd)
            currentEnd = start + commentIndex
            current = current.substring(0, commentIndex)
        } else comment = null

        // Parse label
        val labelIndex = LabelUtils.getLabelFinishIndex(current)
        if (labelIndex != -1) {
            label = NESEditorLabel(this, current.substring(0, labelIndex + 1), currentStart, currentStart + labelIndex)
            currentStart += labelIndex + 1
            current = current.substring(labelIndex + 1)
        } else label = null

        // Parse instruction, directive or equivalence
        val trim = current.trim()
        if (trim.isEmpty()) {
            instruction = null
            directive = null
        } else if (trim[0] == '.') {
            instruction = null
            directive = NESEditorDirective(this, current, currentStart, currentEnd)
        } else {
            instruction = NESEditorInstruction(this, current, currentStart, currentEnd)
            directive = null
        }

    }

}