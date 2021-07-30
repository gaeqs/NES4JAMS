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

package io.github.gaeqs.nes4jams.cartridge.mapper.defaults

import io.github.gaeqs.nes4jams.cartridge.Cartridge
import io.github.gaeqs.nes4jams.cartridge.mapper.Mapper
import io.github.gaeqs.nes4jams.cartridge.mapper.MapperBuilder
import io.github.gaeqs.nes4jams.cartridge.mapper.MapperReadResult
import io.github.gaeqs.nes4jams.cartridge.mapper.MapperWriteResult
import io.github.gaeqs.nes4jams.ppu.Mirror
import io.github.gaeqs.nes4jams.util.BIT0
import io.github.gaeqs.nes4jams.util.BIT6
import io.github.gaeqs.nes4jams.util.BIT7
import io.github.gaeqs.nes4jams.util.extension.shr

class Mapper004(override val cartridge: Cartridge) : Mapper {

    override var mirroring = Mirror.HORIZONTAL
    override var requestingInterrupt = false

    private var target = 0
    private var prgMode = false
    private var chrMode = false

    private var interruptCounterReload = 0
    private var interruptCounterRequiresReload = false
    private var interruptCounter = 0
    private var interruptEnabled = false

    private var registers = IntArray(8)

    private val chrBank = IntArray(8)
    private val prgBank = IntArray(4)

    private val staticVRAM = UByteArray(0x2000)

    init {
        repeat(prgBank.size) { prgBank[it] = (cartridge.prgBanks * 2 - 4 + it) * 0x2000 }
    }

    override fun cpuMapRead(address: UShort): MapperReadResult {
        return when (address) {
            in 0x6000u..0x7FFFu -> {
                MapperReadResult.intrinsic(staticVRAM[address.toInt() and 0x1FFF])
            }
            in 0x8000u..0xFFFFu -> {
                val key = address.toInt() shr 13 and 0x3
                MapperReadResult.array(prgBank[key] + (address and 0x1FFFu).toInt())
            }
            else -> MapperReadResult.empty()
        }
    }

    override fun cpuMapWrite(address: UShort, data: UByte): MapperWriteResult {
        when (address) {
            in 0x6000u..0x7FFFu -> {
                staticVRAM[address.toInt() and 0x1FFF] = data
                return MapperWriteResult.intrinsic()
            }
            in 0x8000u..0xFFFFu -> {
                val odd = address and 0x1u > 0u
                val key = address.toInt() shr 13 and 0x3

                if (odd) {
                    when (key) {
                        0 -> changeBank(data)
                        // 1 - Prg ram write protect
                        2 -> interruptCounterRequiresReload = true
                        3 -> interruptEnabled = true
                    }
                } else {
                    when (key) {
                        0 -> configurationRegister(data)
                        1 -> changeMirroring(data)
                        2 -> {
                            interruptCounterReload = data.toInt()
                        }
                        3 -> disableInterrupts()
                    }
                }
                return MapperWriteResult.intrinsic()
            }
        }
        return MapperWriteResult.empty()
    }

    private fun changeBank(data: UByte) {
        registers[target] = data.toInt()
        if (target < 6) reloadChrBanks()
        else reloadPrgBanks()
    }

    private fun configurationRegister(data: UByte) {
        target = (data and 0x7u).toInt()
        prgMode = data and BIT6 > 0u
        chrMode = data and BIT7 > 0u
        reloadChrBanks()
        reloadPrgBanks()
    }

    private fun changeMirroring(data: UByte) {
        mirroring = if (data and BIT0 > 0u) Mirror.HORIZONTAL else Mirror.VERTICAL
    }

    private fun disableInterrupts() {
        requestingInterrupt = false
        interruptEnabled = false
    }

    private fun reloadChrBanks() {
        if (chrMode) {
            chrBank[0] = registers[2] shl 10
            chrBank[1] = registers[3] shl 10
            chrBank[2] = registers[4] shl 10
            chrBank[3] = registers[5] shl 10

            chrBank[4] = registers[0] and 0xFE shl 10
            chrBank[5] = (registers[0] and 0xFE shl 10) + 0x0400

            chrBank[6] = registers[1] and 0xFE shl 10
            chrBank[7] = (registers[1] and 0xFE shl 10) + 0x0400
        } else {
            chrBank[4] = registers[2] shl 10
            chrBank[5] = registers[3] shl 10
            chrBank[6] = registers[4] shl 10
            chrBank[7] = registers[5] shl 10

            chrBank[0] = registers[0] and 0xFE shl 10
            chrBank[1] = (registers[0] and 0xFE shl 10) + 0x0400

            chrBank[2] = registers[1] and 0xFE shl 10
            chrBank[3] = (registers[1] and 0xFE shl 10) + 0x0400
        }
    }

    private fun reloadPrgBanks() {
        if (prgMode) {
            prgBank[0] = cartridge.prgBanks * 2 - 2 shl 13
            prgBank[2] = registers[6] and 0x3F shl 13
        } else {
            prgBank[0] = registers[6] and 0x3F shl 13
            prgBank[2] = cartridge.prgBanks * 2 - 2 shl 13
        }

        prgBank[1] = (registers[7] and 0x3F) * 0x2000
    }

    override fun ppuMapRead(address: UShort): MapperReadResult {
        if (address in 0x0000u..0x1FFFu) {
            val index = address shr 10 and 0x7u
            return MapperReadResult.array(chrBank[index.toInt()] + (address and 0x03FFu).toInt())
        }
        return MapperReadResult.empty()
    }

    override fun ppuMapWrite(address: UShort, data: UByte): MapperWriteResult {
        return MapperWriteResult.empty()
    }

    override fun reset() {
        mirroring = Mirror.HORIZONTAL
        requestingInterrupt = false

        target = 0
        prgMode = false
        chrMode = false
        interruptCounterReload = 0
        interruptCounter = 0
        interruptEnabled = false
        interruptCounterRequiresReload = false
        registers.fill(0)
        chrBank.fill(0)
        repeat(prgBank.size) { prgBank[it] = (cartridge.prgBanks * 2 - 4 + it) * 0x2000 }
        staticVRAM.fill(0u)
    }

    override fun onScanLine() {
    }

    override fun onA12Change(old: Boolean, now: Boolean) {
        if (now and !old) {
            if (interruptCounterRequiresReload || interruptCounter == 0) {
                interruptCounter = interruptCounterReload
                interruptCounterRequiresReload = false
            } else {
                interruptCounter--
            }

            requestingInterrupt = requestingInterrupt || interruptCounter == 0 && interruptEnabled
        }
    }

    class Builder private constructor() : MapperBuilder<Mapper004> {
        companion object {
            val INSTANCE = Builder()
        }

        override fun build(cartridge: Cartridge) = Mapper004(cartridge)
        override fun getName() = "4"
    }
}