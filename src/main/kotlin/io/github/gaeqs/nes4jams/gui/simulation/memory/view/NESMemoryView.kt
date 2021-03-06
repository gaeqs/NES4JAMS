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

abstract class NESMemoryView(
    private val name: String,
    val readOnly: Boolean,
    val languageNode: String = "NES_MEMORY_VIEW_$name"
) : ManagerResource {

    override fun getName(): String = name

    companion object {
        val CPU = object : NESMemoryView("CPU", true) {
            override fun getResourceProvider(): ResourceProvider = NES4JAMS.INSTANCE
            override fun sizeOf(simulation: NESSimulation) = 0x10000
            override fun read(simulation: NESSimulation, address: UInt) = simulation.cpuRead(address.toUShort(), true)
            override fun write(simulation: NESSimulation, address: UInt, value: UByte) =
                simulation.cpuWrite(address.toUShort(), value);
        }
        val PPU = object : NESMemoryView("PPU", true) {
            override fun getResourceProvider(): ResourceProvider = NES4JAMS.INSTANCE
            override fun sizeOf(simulation: NESSimulation) = 0x4000
            override fun read(simulation: NESSimulation, address: UInt) = simulation.ppu.ppuRead(address.toUShort())
            override fun write(simulation: NESSimulation, address: UInt, value: UByte) =
                simulation.ppu.ppuWrite(address.toUShort(), value)
        }
        val CARTRIDGE_PRG = object : NESMemoryView("CARTRIDGE_PRG", false) {
            override fun getResourceProvider(): ResourceProvider = NES4JAMS.INSTANCE
            override fun sizeOf(simulation: NESSimulation) = simulation.cartridge.prgMemory.size
            override fun read(simulation: NESSimulation, address: UInt) = with(address.toInt()) {
                simulation.cartridge.prgMemory.let {
                    if (it.size > this) it[this] else (0u).toUByte()
                }
            }

            override fun write(simulation: NESSimulation, address: UInt, value: UByte) = with(address.toInt()) {
                simulation.cartridge.prgMemory.let {
                    if (it.size > this) it[this] = value
                }
            }
        }
        val CARTRIDGE_CHR = object : NESMemoryView("CARTRIDGE_CHR", false) {
            override fun getResourceProvider(): ResourceProvider = NES4JAMS.INSTANCE
            override fun sizeOf(simulation: NESSimulation) = simulation.cartridge.chrMemory.size
            override fun read(simulation: NESSimulation, address: UInt) = with(address.toInt()) {
                simulation.cartridge.chrMemory.let {
                    if (it.size > this) it[this] else (0u).toUByte()
                }
            }

            override fun write(simulation: NESSimulation, address: UInt, value: UByte) = with(address.toInt()) {
                simulation.cartridge.chrMemory.let {
                    if (it.size > this) it[this] = value
                }
            }
        }
    }

    abstract fun sizeOf(simulation: NESSimulation): Int
    abstract fun read(simulation: NESSimulation, address: UInt): UByte
    abstract fun write(simulation: NESSimulation, address: UInt, value: UByte)

}