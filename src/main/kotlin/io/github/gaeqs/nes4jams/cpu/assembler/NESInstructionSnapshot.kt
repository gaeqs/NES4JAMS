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

import io.github.gaeqs.nes4jams.cpu.instruction.NESAddressingMode
import io.github.gaeqs.nes4jams.cpu.instruction.NESInstruction
import io.github.gaeqs.nes4jams.util.Value
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException
import kotlin.math.min

class NESInstructionSnapshot(val line: NESAssemblerLine, var address: UShort?, val raw: String) {

    val mnemonic: String
    val parameters: String

    val instruction: NESInstruction?

    lateinit var addressingMode: NESAddressingMode
        private set

    lateinit var expression: String
        private set

    var value: Value? = null

    init {
        var mnemonicIndex: Int = raw.indexOf(' ')
        val tabIndex: Int = raw.indexOf("\t")
        if (mnemonicIndex == -1) mnemonicIndex = tabIndex
        else if (tabIndex != -1) mnemonicIndex = min(mnemonicIndex, tabIndex)

        if (mnemonicIndex == -1) {
            mnemonic = raw.uppercase()
            parameters = ""
        } else {
            mnemonic = raw.substring(0, mnemonicIndex).uppercase()
            parameters = raw.substring(mnemonicIndex + 1).trim()
        }

        this.instruction = NESInstruction.INSTRUCTIONS[mnemonic]
    }

    fun calculateInstructionAddressingMode(): Int {
        if (instruction == null) throw AssemblerException("Couldn't find instruction $mnemonic!")

        val (addressingModes, expression) = NESAddressingMode.getCompatibleAddressingModes(parameters)
        try {
            val (value, isWord) =
                if (NESAddressingMode.IMPLIED in addressingModes) Pair(Value(0, false), false)
                else line.file.evaluate(expression)

            this.value = value
            this.expression = expression

            val compatibleAddressingModes = (addressingModes intersect instruction.supportedAddressingModes.keys)
                .sortedBy { if (it.usesWordInAssembler == isWord) 0 else 1 }

            if (compatibleAddressingModes.isEmpty()) throw AssemblerException(
                line.index,
                "Addressing mode not found for $mnemonic $parameters. Found modes: $addressingModes. " +
                        "Supported modes : ${instruction.supportedAddressingModes.keys}"
            )

            addressingMode = compatibleAddressingModes[0]
            return addressingMode.bytesUsed
        } catch (ex: Exception) {
            throw AssemblerException(line.index, "Error parsing instruction $mnemonic $parameters.", ex)
        }
    }

    fun calculateFinalValue(): Value {
        try {
            if (value != null) return value!!
            val (value, _) = line.file.evaluate(expression)
            this.value = value
            return value ?: throw AssemblerException(line.index, "Couldn't parse value for expression $expression!")
        } catch (ex : UninitializedPropertyAccessException) {
            println("Instruction : $mnemonic $parameters ($addressingMode)")
            throw ex
        }
    }

    fun writeValue() {
        var data = calculateFinalValue().value

        if (addressingMode == NESAddressingMode.RELATIVE) {
            data = data - address!!.toInt() - addressingMode.bytesUsed - 1
        }

        val assembler = line.file.assembler
        assembler.write(line.index, address!!, instruction!!.supportedAddressingModes[addressingMode]!!)

        for (i in 1u..addressingMode.bytesUsed.toUInt()) {
            assembler.write(line.index, (address!! + i).toUShort(), data.toUByte())
            data = data ushr 8
        }
    }


}