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

package io.github.gaeqs.nes4jams.cpu

import io.github.gaeqs.nes4jams.cpu.instruction.NESAddressingMode
import io.github.gaeqs.nes4jams.cpu.instruction.NESAssembledInstruction
import io.github.gaeqs.nes4jams.simulation.NESSimulation
import io.github.gaeqs.nes4jams.util.extension.*

class NESCPU(val simulation: NESSimulation) {

    var accumulator: UByte = 0u
    var xRegister: UByte = 0u
    var yRegister: UByte = 0u
    var stackPointer: UByte = 0u
    var pc: UShort = 0u
    var status: UByte = 0u

    var fetched: UByte = 0u
    var absoluteAddress: UShort = 0u
    var relativeAddress: UShort = 0u

    var currentOpcode: UByte = 0u
    var cyclesLeft: UByte = 0u

    fun clock() {
        if (cyclesLeft > 0u) {
            cyclesLeft--
            return
        }

        currentOpcode = read(pc++)
        val instruction = NESAssembledInstruction.INSTRUCTIONS[currentOpcode.toInt()]
        cyclesLeft = instruction.cycles
        val addCycleOne = instruction.addressingMode.addressingFunction.call(this)
        val addCycleTwo = instruction.operation.call(this)
        if (!addCycleOne || !addCycleTwo) cyclesLeft--
    }

    fun reset() {
        accumulator = 0u
        xRegister = 0u
        yRegister = 0u
        stackPointer = 0xFDu
        status = 0x00u
        status = status or StatusFlag.UNUSED.mask

        pc = read(0xFFFDu) concatenate read(0xFFFCu)

        relativeAddress = 0u
        absoluteAddress = 0u
        fetched = 0u
        cyclesLeft = 8u
    }

    fun interruptRequest() {
        if (getFlag(StatusFlag.DISABLE_INTERRUPTS)) return
        pushToStack((pc shr 8).toUByte())
        pushToStack(pc.toUByte())

        setFlag(StatusFlag.BREAK, true)
        setFlag(StatusFlag.UNUSED, true)
        setFlag(StatusFlag.DISABLE_INTERRUPTS, true)
        pushToStack(status)

        pc = read(0xFFFFu) concatenate read(0xFFFEu)
        cyclesLeft = 7u
    }

    fun nonMaskableInterrupt() {
        pushToStack((pc shr 8).toUByte())
        pushToStack(pc.toUByte())
        setFlag(StatusFlag.BREAK, true)
        setFlag(StatusFlag.UNUSED, true)
        setFlag(StatusFlag.DISABLE_INTERRUPTS, true)
        pushToStack(status)

        pc = read(0xFFFBu) concatenate read(0xFFFAu)
        cyclesLeft = 8u
    }

    fun fetch(): UByte {
        val instruction = NESAssembledInstruction.INSTRUCTIONS[currentOpcode.toInt()]
        if (instruction.addressingMode != NESAddressingMode.IMPLIED) {
            fetched = read(absoluteAddress)
        }
        return fetched
    }

    // region OPCODES

    fun adc(): Boolean {
        fetch()
        var temp = (accumulator.toUShort() + fetched.toUShort()).toUShort()
        if (getFlag(StatusFlag.CARRY_BIT)) temp++

        setFlag(StatusFlag.CARRY_BIT, temp > 255u)
        setFlag(StatusFlag.ZERO, (temp and 0x00FFu).isZero())
        setFlag(StatusFlag.NEGATIVE, (temp and 0x80u) > 0u)
        setFlag(
            StatusFlag.OVERFLOW,
            (accumulator xor fetched).toUShort().inv() and (accumulator.toUShort() xor temp) and 0x0080u > 0u
        )
        accumulator = temp.toUByte()
        return true
    }

    fun and(): Boolean {
        fetch()
        accumulator = accumulator and fetched
        setFlag(StatusFlag.ZERO, accumulator.isZero())
        setFlag(StatusFlag.NEGATIVE, accumulator and 0x80u > 0u)
        return true
    }

    fun asl(): Boolean {
        fetch()
        val temp = fetched.toUShort() shl 1
        setFlag(StatusFlag.CARRY_BIT, temp and 0xFF00u > 0u)
        setFlag(StatusFlag.ZERO, (temp and 0x00FFu).isZero())
        setFlag(StatusFlag.NEGATIVE, temp and 0x80u > 0u)

        if (NESAssembledInstruction.INSTRUCTIONS[currentOpcode.toInt()].addressingMode == NESAddressingMode.IMPLIED) {
            accumulator = temp.toUByte()
        } else {
            write(absoluteAddress, temp.toUByte())
        }
        return false
    }

    fun bcc(): Boolean {
        if (!getFlag(StatusFlag.CARRY_BIT)) branch()
        return false
    }

    fun bcs(): Boolean {
        if (getFlag(StatusFlag.CARRY_BIT)) branch()
        return false
    }

    fun beq(): Boolean {
        if (getFlag(StatusFlag.ZERO)) branch()
        return false
    }

    fun bit(): Boolean {
        fetch()
        val temp = accumulator and fetched
        setFlag(StatusFlag.ZERO, (temp and 0x00FFu).isZero())
        setFlag(StatusFlag.NEGATIVE, fetched and 0x80u > 0u)
        setFlag(StatusFlag.OVERFLOW, fetched and 0x40u > 0u)
        return false
    }

    fun bmi(): Boolean {
        if (getFlag(StatusFlag.NEGATIVE)) branch()
        return false
    }

    fun bne(): Boolean {
        if (!getFlag(StatusFlag.ZERO)) branch()
        return false
    }

    fun bpl(): Boolean {
        if (!getFlag(StatusFlag.NEGATIVE)) branch()
        return false
    }

    fun brk(): Boolean {
        pc++
        setFlag(StatusFlag.DISABLE_INTERRUPTS, true)
        pushToStack((pc shr 8).toUByte())
        pushToStack(pc.toUByte())
        setFlag(StatusFlag.BREAK, true)
        pushToStack(status)
        setFlag(StatusFlag.BREAK, false)
        pc = read(0xFFFFu) concatenate read(0xFFFEu)
        return false
    }

    fun bvc(): Boolean {
        if (!getFlag(StatusFlag.OVERFLOW)) branch()
        return false
    }

    fun bvs(): Boolean {
        if (getFlag(StatusFlag.OVERFLOW)) branch()
        return false
    }

    fun clc(): Boolean {
        setFlag(StatusFlag.CARRY_BIT, false)
        return false
    }

    fun cld(): Boolean {
        setFlag(StatusFlag.DECIMAL_MODE, false)
        return false
    }

    fun cli(): Boolean {
        setFlag(StatusFlag.DISABLE_INTERRUPTS, false)
        return false
    }

    fun clv(): Boolean {
        setFlag(StatusFlag.OVERFLOW, false)
        return false
    }

    fun cmp(): Boolean {
        fetch()
        val temp = (accumulator.toUShort() - fetched.toUShort()).toUShort()
        setFlag(StatusFlag.CARRY_BIT, accumulator >= fetched)
        setFlag(StatusFlag.ZERO, (temp and 0x00FFu).isZero())
        setFlag(StatusFlag.NEGATIVE, temp and 0x80u > 0u)
        return true
    }

    fun cpx(): Boolean {
        fetch()
        val temp = (xRegister.toUShort() - fetched.toUShort()).toUShort()
        setFlag(StatusFlag.CARRY_BIT, xRegister >= fetched)
        setFlag(StatusFlag.ZERO, (temp and 0x00FFu).isZero())
        setFlag(StatusFlag.NEGATIVE, temp and 0x80u > 0u)
        return false
    }

    fun cpy(): Boolean {
        fetch()
        val temp = (yRegister.toUShort() - fetched.toUShort()).toUShort()
        setFlag(StatusFlag.CARRY_BIT, yRegister >= fetched)
        setFlag(StatusFlag.ZERO, (temp and 0x00FFu).isZero())
        setFlag(StatusFlag.NEGATIVE, temp and 0x80u > 0u)
        return false
    }

    fun dec(): Boolean {
        fetch()
        val temp = fetched.dec()
        write(absoluteAddress, temp)
        setFlag(StatusFlag.ZERO, temp.isZero())
        setFlag(StatusFlag.NEGATIVE, temp and 0x80u > 0u)
        return false
    }

    fun dex(): Boolean {
        xRegister--
        setFlag(StatusFlag.ZERO, xRegister.isZero())
        setFlag(StatusFlag.NEGATIVE, xRegister and 0x80u > 0u)
        return false
    }

    fun dey(): Boolean {
        yRegister--
        setFlag(StatusFlag.ZERO, yRegister.isZero())
        setFlag(StatusFlag.NEGATIVE, yRegister and 0x80u > 0u)
        return false
    }

    fun eor(): Boolean {
        fetch()
        accumulator = accumulator xor fetched
        setFlag(StatusFlag.ZERO, accumulator.isZero())
        setFlag(StatusFlag.NEGATIVE, accumulator and 0x80u > 0u)
        return true
    }

    fun inc(): Boolean {
        fetch()
        val temp = fetched.inc()
        write(absoluteAddress, temp)
        setFlag(StatusFlag.ZERO, temp.isZero())
        setFlag(StatusFlag.NEGATIVE, temp and 0x80u > 0u)
        return false
    }

    fun inx(): Boolean {
        xRegister++
        setFlag(StatusFlag.ZERO, xRegister.isZero())
        setFlag(StatusFlag.NEGATIVE, xRegister and 0x80u > 0u)
        return false
    }

    fun iny(): Boolean {
        yRegister++
        setFlag(StatusFlag.ZERO, yRegister.isZero())
        setFlag(StatusFlag.NEGATIVE, yRegister and 0x80u > 0u)
        return false
    }

    fun jmp(): Boolean {
        pc = absoluteAddress
        return false
    }

    fun jsr(): Boolean {
        pc--

        pushToStack((pc shr 8).toUByte())
        pushToStack(pc.toUByte())

        pc = absoluteAddress

        return false
    }

    fun lda(): Boolean {
        fetch()
        accumulator = fetched
        setFlag(StatusFlag.ZERO, accumulator.isZero())
        setFlag(StatusFlag.NEGATIVE, accumulator and 0x80u > 0u)
        return true
    }

    fun ldx(): Boolean {
        fetch()
        xRegister = fetched
        setFlag(StatusFlag.ZERO, xRegister.isZero())
        setFlag(StatusFlag.NEGATIVE, xRegister and 0x80u > 0u)
        return true
    }

    fun ldy(): Boolean {
        fetch()
        yRegister = fetched
        setFlag(StatusFlag.ZERO, yRegister.isZero())
        setFlag(StatusFlag.NEGATIVE, yRegister and 0x80u > 0u)
        return true
    }

    fun lsr(): Boolean {
        fetch()
        setFlag(StatusFlag.CARRY_BIT, fetched and 0x1u > 0u)
        val temp = fetched shr 1
        setFlag(StatusFlag.ZERO, temp.isZero())
        setFlag(StatusFlag.NEGATIVE, temp and 0x80u > 0u)

        if (NESAssembledInstruction.INSTRUCTIONS[currentOpcode.toInt()].addressingMode == NESAddressingMode.IMPLIED) {
            accumulator = temp
        } else {
            write(absoluteAddress, temp)
        }

        return false
    }

    fun nop(): Boolean {

        return when (currentOpcode.toUInt()) {
            0x1Cu,
            0x3Cu,
            0x5Cu,
            0x7Cu,
            0xDCu,
            0xFCu,
            -> true
            else -> false
        }
    }

    fun ora(): Boolean {
        fetch()
        accumulator = accumulator or fetched
        setFlag(StatusFlag.ZERO, accumulator.isZero())
        setFlag(StatusFlag.NEGATIVE, accumulator and 0x80u > 0u)
        return true
    }

    fun pha(): Boolean {
        pushToStack(accumulator)
        return false
    }

    fun php(): Boolean {
        pushToStack(status or StatusFlag.BREAK.mask or StatusFlag.UNUSED.mask)
        setFlag(StatusFlag.BREAK, false)
        setFlag(StatusFlag.UNUSED, false)
        return false
    }

    fun pla(): Boolean {
        accumulator = popFromStack()
        setFlag(StatusFlag.ZERO, accumulator.isZero())
        setFlag(StatusFlag.NEGATIVE, accumulator and 0x80u > 0u)
        return false
    }

    fun plp(): Boolean {
        status = popFromStack()
        setFlag(StatusFlag.UNUSED, true)
        return false
    }

    fun rol(): Boolean {
        fetch()
        val temp = fetched.toUShort() shl 1 or if (getFlag(StatusFlag.CARRY_BIT)) 1u else 0u
        setFlag(StatusFlag.CARRY_BIT, temp and 0xFF00u > 0u)
        setFlag(StatusFlag.ZERO, (temp and 0x00FFu).isZero())
        setFlag(StatusFlag.NEGATIVE, temp and 0x80u > 0u)

        if (NESAssembledInstruction.INSTRUCTIONS[currentOpcode.toInt()].addressingMode == NESAddressingMode.IMPLIED) {
            accumulator = temp.toUByte()
        } else {
            write(absoluteAddress, temp.toUByte())
        }

        return false
    }

    fun ror(): Boolean {
        fetch()

        val temp = (if (getFlag(StatusFlag.CARRY_BIT)) 128u else 0u).toUByte() or (fetched shr 1)
        setFlag(StatusFlag.CARRY_BIT, fetched and 0x1u > 0u)
        setFlag(StatusFlag.ZERO, temp.isZero())
        setFlag(StatusFlag.NEGATIVE, temp and 0x80u > 0u)

        if (NESAssembledInstruction.INSTRUCTIONS[currentOpcode.toInt()].addressingMode == NESAddressingMode.IMPLIED) {
            accumulator = temp
        } else {
            write(absoluteAddress, temp)
        }

        return false
    }

    fun rti(): Boolean {
        status = popFromStack()
        status = status and StatusFlag.BREAK.mask.inv()
        status = status and StatusFlag.UNUSED.mask.inv()

        val low = popFromStack()
        val high = popFromStack()
        pc = high concatenate low

        return false
    }

    fun rts(): Boolean {
        val low = popFromStack()
        val high = popFromStack()
        pc = high concatenate low
        pc++
        return false
    }

    fun sbc(): Boolean {
        fetch()
        val invert = fetched.toUShort() xor 0x00FFu
        var temp = (accumulator.toUShort() + invert).toUShort()
        if (getFlag(StatusFlag.CARRY_BIT)) temp++

        setFlag(StatusFlag.CARRY_BIT, temp and 0xFF00u > 0u)
        setFlag(StatusFlag.ZERO, (temp and 0x00FFu).isZero())
        setFlag(StatusFlag.NEGATIVE, temp and 0x80u > 0u)
        setFlag(
            StatusFlag.OVERFLOW,
            (accumulator.toUShort() xor temp) and (invert xor temp) and 0x0080u > 0u
        )
        accumulator = temp.toUByte()
        return true
    }

    fun sec(): Boolean {
        setFlag(StatusFlag.CARRY_BIT, true)
        return false
    }

    fun sed(): Boolean {
        setFlag(StatusFlag.DECIMAL_MODE, true)
        return false
    }

    fun sei(): Boolean {
        setFlag(StatusFlag.DISABLE_INTERRUPTS, true)
        return false
    }

    fun sta(): Boolean {
        write(absoluteAddress, accumulator)
        return false
    }

    fun stx(): Boolean {
        write(absoluteAddress, xRegister)
        return false
    }

    fun sty(): Boolean {
        write(absoluteAddress, yRegister)
        return false
    }

    fun tax(): Boolean {
        xRegister = accumulator;
        setFlag(StatusFlag.ZERO, xRegister.isZero())
        setFlag(StatusFlag.NEGATIVE, xRegister and 0x80u > 0u)
        return false
    }

    fun tay(): Boolean {
        yRegister = accumulator;
        setFlag(StatusFlag.ZERO, yRegister.isZero())
        setFlag(StatusFlag.NEGATIVE, yRegister and 0x80u > 0u)
        return false
    }

    fun tsx(): Boolean {
        xRegister = stackPointer
        setFlag(StatusFlag.ZERO, xRegister.isZero())
        setFlag(StatusFlag.NEGATIVE, xRegister and 0x80u > 0u)
        return false
    }

    fun txa(): Boolean {
        accumulator = xRegister
        setFlag(StatusFlag.ZERO, accumulator.isZero())
        setFlag(StatusFlag.NEGATIVE, accumulator and 0x80u > 0u)
        return false
    }

    fun txs(): Boolean {
        stackPointer = xRegister
        return false
    }

    fun tya(): Boolean {
        accumulator = yRegister
        setFlag(StatusFlag.ZERO, accumulator.isZero())
        setFlag(StatusFlag.NEGATIVE, accumulator and 0x80u > 0u)
        return false
    }

    fun ill(): Boolean {
        return false
    }

    //endregion

    // region ADDRESSING MODES

    fun imp(): Boolean {
        fetched = accumulator
        return false
    }

    fun imm(): Boolean {
        absoluteAddress = pc++
        return false
    }

    fun zp0(): Boolean {
        absoluteAddress = read(pc++).toUShort()
        return false
    }

    fun zpx(): Boolean {
        absoluteAddress = (read(pc++) + xRegister).toUShort() and 0x00FFu
        return false
    }

    fun zpy(): Boolean {
        absoluteAddress = (read(pc++) + yRegister).toUShort() and 0x00FFu
        return false
    }

    fun rel(): Boolean {
        relativeAddress = read(pc++).toUShort()

        //If negative, extend the sign
        if (relativeAddress and 0x80u > 0u) {
            relativeAddress = relativeAddress or 0xFF00u
        }
        return false
    }

    fun abs(): Boolean {
        val low = read(pc++)
        val high = read(pc++)
        absoluteAddress = high concatenate low
        return false
    }

    fun abx(): Boolean {
        val low = read(pc++)
        val high = read(pc++)
        absoluteAddress = high concatenate low
        absoluteAddress = (absoluteAddress + xRegister).toUShort()

        return absoluteAddress and 0xFF00u != high.toUShort() shl 8
    }

    fun aby(): Boolean {
        val low = read(pc++)
        val high = read(pc++)
        absoluteAddress = high concatenate low
        absoluteAddress = (absoluteAddress + yRegister).toUShort()

        return absoluteAddress and 0xFF00u != high.toUShort() shl 8
    }

    fun ind(): Boolean {
        val low = read(pc++)
        val high = read(pc++)

        val ptr: UShort = high concatenate low

        // Page boundary bug!
        absoluteAddress = if (low == UByte.MAX_VALUE) {
            read(ptr and 0xFF00u).toUShort() shl 8 or read(ptr).toUShort()
        } else {
            read(ptr.inc()).toUShort() shl 8 or read(ptr).toUShort()
        }

        return false
    }

    fun izx(): Boolean {
        val temp = (read(pc++) + xRegister).toUShort()

        val low = read(temp and 0x00FFu)
        val high = read((temp + 1u).toUShort() and 0x00FFu)
        absoluteAddress = high concatenate low

        return false
    }

    fun izy(): Boolean {
        val temp = read(pc++).toUShort()

        val low = read(temp and 0x00FFu)
        val high = read((temp + 1u).toUShort() and 0x00FFu)
        absoluteAddress = high concatenate low
        absoluteAddress = (absoluteAddress + yRegister).toUShort()

        return absoluteAddress and 0xFF00u != high.toUShort() shl 8
    }

    //endregion

    // region HELPER FUNCTIONS

    fun isCycleCompleted() = cyclesLeft.isZero()

    private fun branch() {
        cyclesLeft++
        absoluteAddress = (pc + relativeAddress).toUShort()

        if (absoluteAddress and 0xFF00u != pc and 0xFF00u) cyclesLeft++
        pc = absoluteAddress
    }

    private fun read(address: UShort) = simulation.cpuRead(address, false)

    private fun write(address: UShort, data: UByte) = simulation.cpuWrite(address, data)

    fun getFlag(flag: StatusFlag) = status and flag.mask > 0u

    fun setFlag(flag: StatusFlag, value: Boolean) {
        status = if (value) {
            status or flag.mask
        } else {
            status and flag.mask.inv()
        }
    }

    private fun pushToStack(value: UByte) = write((0x0100u + stackPointer--).toUShort(), value)

    private fun popFromStack() = read((0x0100u + ++stackPointer).toUShort())

    //endregion
}

enum class StatusFlag(val mask: UByte) {

    CARRY_BIT(0b00000001u),
    ZERO(0b00000010u),
    DISABLE_INTERRUPTS(0b00000100u), // Unused for NES
    DECIMAL_MODE(0b00001000u),
    BREAK(0b00010000u),
    UNUSED(0b00100000u),
    OVERFLOW(0b01000000u),
    NEGATIVE(0b10000000u)

}