package io.github.gaeqs.nes4jams.cpu.assembler

import io.github.gaeqs.nes4jams.cpu.instruction.NESAddressingMode
import io.github.gaeqs.nes4jams.cpu.instruction.NESAssembledInstruction
import io.github.gaeqs.nes4jams.utils.extension.concatenate
import io.github.gaeqs.nes4jams.utils.extension.toHex


class NESAssembledRawData(val start: UShort, val size: Int) {

    val array = UByteArray(size) { 0u }

    fun write(address: UShort, data: UByte) {
        array[(address - start).toInt()] = data
    }

    fun read(address: UShort): UByte {
        return array[(address - start).toInt()]
    }

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
            val opcode = read(address++)
            val instruction = NESAssembledInstruction.INSTRUCTIONS[opcode.toInt()]
            string += "${instruction.mnemonic}  "

            string += when (instruction.addressingMode) {
                NESAddressingMode.IMPLIED -> "{IMP}"
                NESAddressingMode.IMMEDIATE -> {
                    value = read(address++)
                    "#$${value.toHex(2)} {IMM}"
                }
                NESAddressingMode.ZERO_PAGE -> {
                    low = read(address++)
                    "$${low.toHex(2)}, {ZP0}"
                }
                NESAddressingMode.ZERO_PAGE_X -> {
                    low = read(address++)
                    "$${low.toHex(2)}, X {ZPX}"
                }
                NESAddressingMode.ZERO_PAGE_Y -> {
                    low = read(address++)
                    "$${low.toHex(2)}, Y {ZPY}"
                }
                NESAddressingMode.INDIRECT_X -> {
                    low = read(address++)
                    "($${low.toHex(2)}, X) {IZX}"
                }
                NESAddressingMode.INDIRECT_Y -> {
                    low = read(address++)
                    "($${low.toHex(2)}), Y {IZY}"
                }
                NESAddressingMode.ABSOLUTE -> {
                    low = read(address++)
                    high = read(address++)
                    "$${(high concatenate low).toHex(4)} {ABS}"
                }
                NESAddressingMode.ABSOLUTE_X -> {
                    low = read(address++)
                    high = read(address++)
                    "$${(high concatenate low).toHex(4)}, X {ABX}"
                }
                NESAddressingMode.ABSOLUTE_Y -> {
                    low = read(address++)
                    high = read(address++)
                    "$${(high concatenate low).toHex(4)}, Y {ABY}"
                }
                NESAddressingMode.INDIRECT -> {
                    low = read(address++)
                    high = read(address++)
                    "($${(high concatenate low).toHex(4)}) {IND}"
                }
                NESAddressingMode.RELATIVE -> {
                    value = read(address++)
                    "$${value.toHex(2)} [$${(address.toInt() + value.toByte()).toString(16)}] {REL}"
                }
            }
            map[lineStart] = string
        }
        return map
    }
}