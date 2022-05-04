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

package io.github.gaeqs.nes4jams.cpu.instruction

class NESInstruction(val mnemonic: String, val supportedAddressingModes: Map<NESAddressingMode, UByte>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return mnemonic == (other as NESInstruction).mnemonic
    }

    override fun hashCode(): Int {
        return mnemonic.hashCode()
    }

    override fun toString(): String {
        return "NESInstruction(mnemonic='$mnemonic')"
    }

    companion object {

        val INSTRUCTIONS: Map<String, NESInstruction>

        init {
            val temp = mutableMapOf<String, MutableMap<NESAddressingMode, UByte>>()
            NESAssembledInstruction.INSTRUCTIONS.forEachIndexed { opcode, assembled ->
                temp.computeIfAbsent(assembled.mnemonic) { mutableMapOf() } += assembled.addressingMode to opcode.toUByte()
            }
            INSTRUCTIONS = temp.map { it.key to NESInstruction(it.key, it.value) }.toMap()
        }

    }
}