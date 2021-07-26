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
import io.github.gaeqs.nes4jams.util.BIT2
import io.github.gaeqs.nes4jams.util.BIT3
import io.github.gaeqs.nes4jams.util.BIT4
import io.github.gaeqs.nes4jams.util.extension.shl
import io.github.gaeqs.nes4jams.util.extension.shr

class Mapper001(override val cartridge: Cartridge) : Mapper {

    override var mirroring = Mirror.HORIZONTAL
    override val requestingInterrupt = false

    private var shift: UByte = 0u
    private var latch: UByte = 0u
    private var control: UByte = 0xCu
    private var soromLatch = false

    private var chr0: UByte = 0u
    private var chr1: UByte = 0u
    private var prg: UByte = 0u

    private var selectedChrBankHigh: UByte = if (cartridge.chrBanks == 0) 0u else (cartridge.chrBanks - 1).toUByte()
    private var selectedChrBankLow: UByte = if (cartridge.chrBanks == 0) 0u else (cartridge.chrBanks - 1).toUByte()
    private var selectedPrgBankHigh: UByte = if (cartridge.prgBanks == 0) 0u else (cartridge.prgBanks - 1).toUByte()
    private var selectedPrgBankLow: UByte = if (cartridge.prgBanks == 0) 0u else (cartridge.prgBanks - 1).toUByte()

    private val staticVRAM = UByteArray(0x2000)

    override fun cpuMapRead(address: UShort): MapperReadResult {
        if (address in 0x6000u..0x7FFFu) {
            return MapperReadResult.intrinsic(staticVRAM[address.toInt() and 0x1FFF])
        }

        if (address in 0x8000u..0xFFFFu) {
            return if (control and BIT3 > 0u) {
                // 16K mode
                if (address in 0x8000u..0xBFFFu) {
                    MapperReadResult.array(selectedPrgBankLow.toInt() * 0x4000 + (address.toInt() and 0x3FFF))
                } else {
                    MapperReadResult.array(selectedPrgBankHigh.toInt() * 0x4000 + (address.toInt() and 0x3FFF))
                }
            } else {
                // 32k mode
                MapperReadResult.array(selectedPrgBankLow.toInt() * 0x8000 + (address.toInt() and 0x7FFF))
            }
        }

        return MapperReadResult.empty()
    }

    override fun cpuMapWrite(address: UShort, data: UByte): MapperWriteResult {
        if (address in 0x6000u..0x7FFFu) {
            staticVRAM[address.toInt() and 0x1FFF] = data
            return MapperWriteResult.intrinsic()
        }

        if (address in 0x8000u..0xFFFFu) {
            if (data and 0x80u > 0u) {
                // Load register
                shift = 0u
                latch = 0u
                control = control or 0xCu
                refreshSelectedCHRBank()
                refreshSelectedPRGBank()
                return MapperWriteResult.intrinsic()
            }

            shift = ((shift shr 1) + (data and 0x1u shl 4)).toUByte()
            latch++
            // Nothing to do yet!
            if (latch < 5u) return MapperWriteResult.intrinsic()

            val targetRegister = (address shr 13) and 0x03u
            when (targetRegister.toUInt()) {
                0u -> writeControl()
                1u -> writeCHRBank0()
                2u -> writeCHRBank1()
                3u -> writePRGBank()
            }
            latch = 0u
            shift = 0u
        }

        return MapperWriteResult.empty()
    }

    private fun writeControl() {
        control = shift and 0x1Fu
        mirroring = when (control.toUInt() and 0x03u) {
            0u -> Mirror.ONESCREEN_LO
            1u -> Mirror.ONESCREEN_HI
            2u -> Mirror.VERTICAL
            else -> Mirror.HORIZONTAL
        }
        refreshSelectedCHRBank()
        refreshSelectedPRGBank()
    }

    private fun writeCHRBank0() {
        chr0 = if (cartridge.prgBanks > 16) {
            // Support for SOROM boards
            soromLatch = shift and BIT4 > 0u
            shift and 0x0Fu
        } else {
            shift and 0x1Fu
        }
        refreshSelectedCHRBank()
    }

    private fun writeCHRBank1() {
        chr1 = if (cartridge.prgBanks > 16) {
            // Support for SOROM boards
            shift and 0x0Fu
        } else {
            shift and 0x1Fu
        }
        refreshSelectedCHRBank()
    }

    private fun writePRGBank() {
        prg = shift and 0xFu
        refreshSelectedPRGBank()
    }

    private fun refreshSelectedCHRBank() {
        if (control and BIT4 > 0u) {
            // 4k mode
            selectedChrBankLow = chr0
            selectedChrBankHigh = chr1
        } else {
            // 8k mode
            selectedChrBankLow = chr0
            selectedChrBankHigh = chr0
        }
    }

    private fun refreshSelectedPRGBank() {
        if (control and BIT3 > 0u) {
            if (control and BIT2 > 0u) {
                // 16K mode, last bank fixed
                selectedPrgBankLow = prg
                selectedPrgBankHigh = (cartridge.prgBanks - 1).toUByte()
            } else {
                // 16K mode, first bank fixed
                selectedPrgBankLow = 0u
                selectedPrgBankHigh = prg
            }
        } else {
            // 32k mode
            selectedPrgBankLow = prg shr 1
            selectedPrgBankHigh = prg shr 1
        }

        // SOROM
        if (soromLatch && cartridge.prgBanks > 16) {
            selectedPrgBankHigh = (selectedPrgBankHigh + 16u).toUByte()
            selectedChrBankLow = (selectedChrBankLow + 16u).toUByte()
        }
    }

    override fun ppuMapRead(address: UShort): MapperReadResult {

        if (address in 0x0000u..0x1FFFu) {
            if (cartridge.chrBanks == 0) return MapperReadResult.array(address.toInt())

            return if (control and BIT4 > 0u) {
                // 4k mode
                if (address in 0x0000u..0x0FFFu) {
                    MapperReadResult.array(selectedChrBankLow.toInt() * 0x1000 + (address.toInt()))
                } else {
                    MapperReadResult.array(selectedChrBankHigh.toInt() * 0x1000 + (address.toInt() and 0x0FFF))
                }
            } else {
                // 8k mode
                MapperReadResult.array(selectedChrBankLow.toInt() * 0x2000 + (address.toInt()))
            }

        }

        return MapperReadResult.empty()
    }

    override fun ppuMapWrite(address: UShort, data: UByte): MapperWriteResult {
        if (address in 0x0000u..0x1FFFu) {
            return MapperWriteResult.array(address.toInt())
        }

        return MapperWriteResult.empty()
    }

    override fun reset() {
    }

    override fun clearInterruptRequest() {}
    override fun onScanLine() {}
    override fun onA12Notification(address: UShort) {}

    class Builder private constructor() : MapperBuilder<Mapper001> {
        companion object {
            val INSTANCE = Builder()
        }

        override fun build(cartridge: Cartridge) = Mapper001(cartridge)
        override fun getName() = "1"
    }
}