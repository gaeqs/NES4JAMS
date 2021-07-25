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

class Mapper002(override val cartridge: Cartridge) : Mapper {

    override val mirroring = Mirror.HARDWARE
    override val requestingInterrupt = false

    private val selectedPRGBankHigh: UByte = (cartridge.prgBanks - 1).toUByte()

    private var selectedPRGBankLow: UByte = 0u

    override fun cpuMapRead(address: UShort): MapperReadResult {
        if (address in 0x8000u..0xBFFFu) {
            val result = selectedPRGBankLow * 0x4000u + (address and 0x3FFFu)
            return MapperReadResult.array(result.toInt())
        }
        if (address in 0xC000u..0xFFFFu) {
            val result = selectedPRGBankHigh * 0x4000u + (address and 0x3FFFu)
            return MapperReadResult.array(result.toInt())
        }

        return MapperReadResult.empty()
    }

    override fun cpuMapWrite(address: UShort, data: UByte): MapperWriteResult {
        if (address in 0x8000u..0xFFFFu) {
            selectedPRGBankLow = data and 0x0Fu
        }

        return MapperWriteResult.empty()
    }

    override fun ppuMapRead(address: UShort): MapperReadResult {
        if (address in 0x0000u..0x1FFFu) {
            return MapperReadResult.array(address.toInt())
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
        selectedPRGBankLow = 0u
    }

    override fun clearInterruptRequest() {}
    override fun onScanLine() {}

    class Builder private constructor() : MapperBuilder<Mapper002> {
        companion object {
            val INSTANCE = Builder()
        }

        override fun build(cartridge: Cartridge) = Mapper002(cartridge)
        override fun getName() = "2"
    }
}