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
import io.github.gaeqs.nes4jams.util.extension.shr

class Mapper011(override val cartridge: Cartridge) : Mapper {

    override val mirroring = Mirror.HARDWARE
    override val requestingInterrupt = false

    private var selectedPRGBank: UByte = 0u
    private var selectedCHRBank: UByte = 0u

    override fun cpuMapRead(address: UShort) =
        if (address >= 0x8000u) {
            val target = selectedPRGBank * 0x8000u + (address and 0x7FFFu)
            MapperReadResult.array(target.toInt())
        } else MapperReadResult.empty()

    override fun cpuMapWrite(address: UShort, data: UByte) =
        if (address >= 0x8000u) {
            selectedPRGBank = data and 0x03u
            selectedCHRBank = data shr 4
            MapperWriteResult.intrinsic()
        } else MapperWriteResult.empty()

    override fun ppuMapRead(address: UShort) =
        if (address < 0x2000u) {
            val target = selectedCHRBank * 0x2000u + address
            MapperReadResult.array(target.toInt())
        } else MapperReadResult.empty()

    override fun ppuMapWrite(address: UShort, data: UByte) = MapperWriteResult.empty()

    override fun reset() {
        selectedCHRBank = 0u
        selectedPRGBank = 0u
    }

    override fun onScanline(scanline: Int) {}
    override fun onA12Rising() {}

    class Builder private constructor() : MapperBuilder<Mapper011> {
        companion object {
            val INSTANCE = Builder()
        }

        override fun build(cartridge: Cartridge) = Mapper011(cartridge)
        override fun getName() = "11"
    }
}