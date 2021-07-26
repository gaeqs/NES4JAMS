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
import io.github.gaeqs.nes4jams.util.BIT12
import io.github.gaeqs.nes4jams.util.extension.isZero
import io.github.gaeqs.nes4jams.util.extension.shr

class Mapper004(override val cartridge: Cartridge) : Mapper {

    override var mirroring = Mirror.HORIZONTAL
    override var requestingInterrupt = false

    private var targetRegister: UByte = 0u

    private var prgBankMode = false
    private var chrInversion = false
    private val register = IntArray(8)
    private val chrBank = IntArray(8)
    private val prgBank = IntArray(4)

    private var irqEnabled = false
    private var irqUpdate = false
    private var irqCounter: UShort = 0u
    private var irqReload: UShort = 0u

    private var lastA12: Boolean = false
    private var a12Timer = 0

    private val staticVRAM = UByteArray(0x2000)

    override fun cpuMapRead(address: UShort): MapperReadResult {
        return when (address) {
            in 0x6000u..0x7FFFu -> {
                MapperReadResult.intrinsic(staticVRAM[address.toInt() and 0x1FFF])
            }
            in 0x8000u..0xFFFFu -> {
                val index = address.toInt() / 0x2000 - 4
                MapperReadResult.array(prgBank[index] + (address and 0x1FFFu).toInt())
            }
            else -> MapperReadResult.empty()
        }

    }

    override fun cpuMapWrite(address: UShort, data: UByte): MapperWriteResult {
        return when (address) {
            in 0x6000u..0x7FFFu -> {
                staticVRAM[address.toInt() and 0x1FFF] = data
                MapperWriteResult.intrinsic()
            }
            in 0x8000u..0x9FFFu -> {
                if ((address and 0x0001u).isZero()) {
                    targetRegister = data and 0x07u
                    prgBankMode = data and 0x40u > 0u
                    chrInversion = data and 0x80u > 0u
                } else {
                    register[targetRegister.toInt()] = data.toInt()
                }

                if (chrInversion) {
                    chrBank[0] = register[2] * 0x0400
                    chrBank[1] = register[3] * 0x0400
                    chrBank[2] = register[4] * 0x0400
                    chrBank[3] = register[5] * 0x0400
                    chrBank[4] = (register[0] and 0xFE) * 0x400
                    chrBank[5] = (register[0] and 0xFE) * 0x400 + 0x400
                    chrBank[6] = (register[1] and 0xFE) * 0x400
                    chrBank[7] = (register[1] and 0xFE) * 0x400 + 0x400
                } else {
                    chrBank[0] = (register[0] and 0xFE) * 0x400
                    chrBank[1] = (register[0] and 0xFE) * 0x400 + 0x400
                    chrBank[2] = (register[1] and 0xFE) * 0x400
                    chrBank[3] = (register[1] and 0xFE) * 0x400 + 0x400
                    chrBank[4] = register[2] * 0x0400
                    chrBank[5] = register[3] * 0x0400
                    chrBank[6] = register[4] * 0x0400
                    chrBank[7] = register[5] * 0x0400
                }

                if (prgBankMode) {
                    prgBank[2] = (register[6] and 0x3F) * 0x2000
                    prgBank[0] = (cartridge.prgBanks * 2 - 2) * 0x2000
                } else {
                    prgBank[0] = (register[6] and 0x3F) * 0x2000
                    prgBank[2] = (cartridge.prgBanks * 2 - 2) * 0x2000
                }

                prgBank[1] = (register[7] and 0x3F) * 0x2000
                prgBank[3] = (cartridge.prgBanks * 2 - 1) * 0x2000

                MapperWriteResult.empty()
            }
            in 0xA000u..0xBFFFu -> {
                if ((address and 0x0001u).isZero()) {
                    mirroring = if (data and 0x01u > 0u) Mirror.HORIZONTAL else Mirror.VERTICAL
                }
                // TODO PRG RAM PROTECT
                MapperWriteResult.empty()
            }
            in 0xC000u..0xDFFFu -> {
                if ((address and 0x0001u).isZero()) {
                    irqReload = data.toUShort()
                } else {
                    irqCounter = 0u
                }
                MapperWriteResult.empty()
            }
            in 0xE000u..0xFFFFu -> {
                if ((address and 0x0001u).isZero()) {
                    irqEnabled = false
                    requestingInterrupt = false
                } else {
                    irqEnabled = true
                }
                MapperWriteResult.empty()
            }
            else -> MapperWriteResult.empty()
        }
    }

    override fun ppuMapRead(address: UShort): MapperReadResult {
        onA12Notification(address)
        if (address in 0x0000u..0x1FFFu) {
            val index = address shr 10 and 0x7u
            return MapperReadResult.array(chrBank[index.toInt()] + (address and 0x03FFu).toInt())
        }
        return MapperReadResult.empty()
    }

    override fun ppuMapWrite(address: UShort, data: UByte): MapperWriteResult {
        onA12Notification(address)
        return MapperWriteResult.empty()
    }

    override fun reset() {
        targetRegister = 0u
        prgBankMode = false
        chrInversion = false
        mirroring = Mirror.HORIZONTAL

        requestingInterrupt = false
        irqEnabled = false
        irqUpdate = false
        irqCounter = 0u
        irqReload = 0u

        chrBank.fill(0)
        register.fill(0)

        prgBank[0] = 0x0000
        prgBank[1] = 0x2000
        prgBank[2] = (cartridge.prgBanks * 2 - 2) * 0x2000
        prgBank[3] = (cartridge.prgBanks * 2 - 1) * 0x2000
    }

    override fun clearInterruptRequest() {
        requestingInterrupt = false
    }

    override fun onScanLine() {

    }

    override fun onA12Notification(address: UShort) {
        val a12 = address and BIT12 > 0u
        if (a12 && !lastA12) {
            if (a12Timer <= 0) {
                a12scanLine()
            }
        } else if (!a12 && lastA12) {
            a12Timer = 8
        }
        if (a12Timer > 0) {
            a12Timer--
        }
        lastA12 = a12
    }

    private fun a12scanLine() {
        if (irqCounter.isZero()) {
            irqCounter = irqReload
        } else {
            irqCounter--
        }

        if (irqCounter.isZero() && irqEnabled && !requestingInterrupt) {
            requestingInterrupt = true
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