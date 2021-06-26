/*
 *  MIT License
 *
 *  Copyright (c) 2021 Gael Rial Costas
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package io.github.gaeqs.nes4jams.cpu.assembler

import io.github.gaeqs.nes4jams.cpu.label.NESLabel
import io.github.gaeqs.nes4jams.utils.extension.isLabelLegal
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException
import net.jamsimulator.jams.utils.LabelUtils

class NESAssemblerLine(
    val file: NESAssemblerFile,
    val index: Int,
    val line: String,
    val original: String,
    labelSuffix: String
) {

    val label: NESLabel?
    val equivalent: NESAssemblerEquivalent?
    val directive: NESDirectiveSnapshot?
    val instruction: NESInstructionSnapshot?
    val macroCall: NESMacroCallSnapshot?

    init {
        equivalent = checkEquivalent()
        if (equivalent != null) {
            label = null
            directive = null
            instruction = null
            macroCall = null
        } else {
            val (l, labelIndex) = checkLabel(labelSuffix)
            label = l

            if (l != null && !l.key.isLabelLegal())
                throw AssemblerException(index, "Label ${l.key} contains illegal characters.")

            val currentLine = if (l != null) line.substring(labelIndex + 1).trim() else line
            if (currentLine.isEmpty()) {
                directive = null
                instruction = null
                macroCall = null
            } else if (currentLine[0] == '.') {
                directive = NESDirectiveSnapshot(this, null, currentLine)
                instruction = null
                macroCall = null
            } else {
                directive = null

                val snapshot = NESInstructionSnapshot(this, null, currentLine)
                if (snapshot.instruction == null
                    && snapshot.parameters.startsWith('(')
                    && snapshot.parameters.endsWith(")")
                ) {
                    instruction = null
                    macroCall = NESMacroCallSnapshot(this, snapshot.mnemonic, snapshot.parameters)
                } else {
                    instruction = snapshot
                    macroCall = null
                }
            }
        }
    }


    private fun checkEquivalent(): NESAssemblerEquivalent? {
        val equivalenceIndex = line.indexOf('=')
        if (equivalenceIndex != -1) {
            val key = line.substring(0, equivalenceIndex).trim()
            val value = line.substring(equivalenceIndex + 1).filter { !it.isWhitespace() }
            if (key.isEmpty()) throw AssemblerException(index, "Equivalence key is empty")
            if (key.any { it.isWhitespace() }) throw AssemblerException(index, "Equivalence key has whitespaces.")
            if (value.isEmpty()) throw AssemblerException(index, "Equivalence value is empty")
            return NESAssemblerEquivalent(this, key, value)
        }
        return null
    }

    private fun checkLabel(suffix: String): Pair<NESLabel?, Int> {
        val labelIndex = LabelUtils.getLabelFinishIndex(line)
        if (labelIndex != -1) {
            return Pair(NESLabel(line.substring(0, labelIndex) + suffix, null, file.name, index), labelIndex)
        }
        return Pair(null, -1)
    }

    override fun toString(): String {
        return "NESAssemblerLine(file=$file, index=$index, line='$line', original='$original', label=$label, equivalent=$equivalent, directive=$directive, instruction=$instruction, macroCall=$macroCall)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NESAssemblerLine

        if (file != other.file) return false
        if (index != other.index) return false

        return true
    }

    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + index
        return result
    }


}