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
import io.github.gaeqs.nes4jams.cpu.instruction.NESAssembledInstruction
import io.github.gaeqs.nes4jams.memory.NESMemoryBank
import io.github.gaeqs.nes4jams.util.extension.concatenate
import io.github.gaeqs.nes4jams.util.extension.toHex
import net.jamsimulator.jams.mips.assembler.exception.AssemblerException

class NESAssemblerMemoryBank(
    val id: Int,
    val start: UShort,
    val size: UShort,
    val writable: Boolean,
    val writeOnCartridge: Boolean
) {

    constructor(id: Int, builder: NESMemoryBank) : this(
        id,
        builder.start,
        builder.size,
        builder.writable,
        builder.writeOnCartridge
    )

    var pointer = start

    val array = UByteArray(size.toInt()) { 0u }

    fun writeAndNext(data: UByte) {
        if (!writable) throw AssemblerException("Bank is read only.")
        array[(pointer - start).toInt()] = data
        pointer++
    }

    fun skip(amount: Int) {
        pointer = (pointer.toInt() + amount).toUShort();
    }

    operator fun plusAssign(data: UByte) {
        writeAndNext(data)
    }

    operator fun set(address: UShort, data: UByte) {
        if (!writable) throw AssemblerException("Bank is read only.")
        array[(address - start).toInt()] = data
    }

    operator fun get(address: UShort): UByte {
        return array[(address - start).toInt()]
    }

    fun disassemble() = disassemble(start, (start + size - 1u).toUShort())

    fun disassemble(from: UShort, to: UShort): Map<Int, String> {
        var address = from
        var value: UByte
        var low: UByte
        var high: UByte
        val map = HashMap<Int, String>()
        var lineStart = address.toInt()

        while (address <= to) {
            if (lineStart > address.toInt()) break
            lineStart = address.toInt()
            var string = "$${address.toString(16)}:  "
            val opcode = get(address++)
            val instruction = NESAssembledInstruction.INSTRUCTIONS[opcode.toInt()]
            string += "${instruction.mnemonic}  "

            string += when (instruction.addressingMode) {
                NESAddressingMode.IMPLIED -> "{IMP}"
                NESAddressingMode.IMMEDIATE -> {
                    value = get(address++)
                    "#$${value.toHex(2)} {IMM}"
                }
                NESAddressingMode.ZERO_PAGE -> {
                    low = get(address++)
                    "$${low.toHex(2)}, {ZP0}"
                }
                NESAddressingMode.ZERO_PAGE_X -> {
                    low = get(address++)
                    "$${low.toHex(2)}, X {ZPX}"
                }
                NESAddressingMode.ZERO_PAGE_Y -> {
                    low = get(address++)
                    "$${low.toHex(2)}, Y {ZPY}"
                }
                NESAddressingMode.INDIRECT_X -> {
                    low = get(address++)
                    "($${low.toHex(2)}, X) {IZX}"
                }
                NESAddressingMode.INDIRECT_Y -> {
                    low = get(address++)
                    "($${low.toHex(2)}), Y {IZY}"
                }
                NESAddressingMode.ABSOLUTE -> {
                    low = get(address++)
                    high = get(address++)
                    "$${(high concatenate low).toHex(4)} {ABS}"
                }
                NESAddressingMode.ABSOLUTE_X -> {
                    low = get(address++)
                    high = get(address++)
                    "$${(high concatenate low).toHex(4)}, X {ABX}"
                }
                NESAddressingMode.ABSOLUTE_Y -> {
                    low = get(address++)
                    high = get(address++)
                    "$${(high concatenate low).toHex(4)}, Y {ABY}"
                }
                NESAddressingMode.INDIRECT -> {
                    low = get(address++)
                    high = get(address++)
                    "($${(high concatenate low).toHex(4)}) {IND}"
                }
                NESAddressingMode.RELATIVE -> {
                    value = get(address++)
                    "$${value.toHex(2)} [$${(address.toInt() + value.toByte()).toString(16)}] {REL}"
                }
            }
            map[lineStart] = string
        }
        return map
    }

}