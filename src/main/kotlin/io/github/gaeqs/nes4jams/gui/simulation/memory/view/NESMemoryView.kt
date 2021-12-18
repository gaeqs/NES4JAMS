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

package io.github.gaeqs.nes4jams.gui.simulation.memory.view

import io.github.gaeqs.nes4jams.NES4JAMS
import io.github.gaeqs.nes4jams.simulation.NESSimulation
import net.jamsimulator.jams.manager.ManagerResource
import net.jamsimulator.jams.manager.ResourceProvider

abstract class NESMemoryView(val readOnly: Boolean) : ManagerResource {

    companion object {
        val CPU = object : NESMemoryView(true) {
            override fun getName() = "CPU"
            override fun getResourceProvider(): ResourceProvider = NES4JAMS.INSTANCE
            override fun sizeOf(simulation: NESSimulation) = 0x10000
            override fun read(simulation: NESSimulation, address: UShort) = simulation.cpuRead(address, true)
            override fun write(simulation: NESSimulation, address: UShort, value: UByte) {}
        }
        val PPU = object : NESMemoryView(true) {
            override fun getName() = "PPU"
            override fun getResourceProvider(): ResourceProvider = NES4JAMS.INSTANCE
            override fun sizeOf(simulation: NESSimulation) = 0x4000
            override fun read(simulation: NESSimulation, address: UShort) = simulation.ppu.ppuRead(address)
            override fun write(simulation: NESSimulation, address: UShort, value: UByte) {}
        }
        val CARTRIDGE_PRG = object : NESMemoryView(false) {
            override fun getName() = "CARTRIDGE_PRG"
            override fun getResourceProvider(): ResourceProvider = NES4JAMS.INSTANCE
            override fun sizeOf(simulation: NESSimulation) = simulation.cartridge.prgMemory.size
            override fun read(simulation: NESSimulation, address: UShort) =
                simulation.cartridge.prgMemory[address.toInt()]

            override fun write(simulation: NESSimulation, address: UShort, value: UByte) {
                simulation.cartridge.prgMemory[address.toInt()] = value
            }
        }
        val CARTRIDGE_CHR = object : NESMemoryView(false) {
            override fun getName() = "CARTRIDGE_CHR"
            override fun getResourceProvider(): ResourceProvider = NES4JAMS.INSTANCE
            override fun sizeOf(simulation: NESSimulation) = simulation.cartridge.chrMemory.size
            override fun read(simulation: NESSimulation, address: UShort) =
                simulation.cartridge.chrMemory[address.toInt()]

            override fun write(simulation: NESSimulation, address: UShort, value: UByte) {
                simulation.cartridge.chrMemory[address.toInt()] = value
            }
        }
    }

    abstract fun sizeOf(simulation: NESSimulation): Int
    abstract fun read(simulation: NESSimulation, address: UShort): UByte
    abstract fun write(simulation: NESSimulation, address: UShort, value: UByte)

}