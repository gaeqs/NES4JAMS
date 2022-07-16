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

package io.github.gaeqs.nes4jams.gui.simulation.memory.representation

import io.github.gaeqs.nes4jams.NES4JAMS
import io.github.gaeqs.nes4jams.gui.simulation.memory.NESMemoryPane
import io.github.gaeqs.nes4jams.ppu.PPUColors
import io.github.gaeqs.nes4jams.util.extension.concatenate
import io.github.gaeqs.nes4jams.util.extension.toHex
import io.github.gaeqs.nes4jams.util.extension.toRGBA
import net.jamsimulator.jams.manager.ManagerResource
import net.jamsimulator.jams.manager.ResourceProvider
import net.jamsimulator.jams.utils.StringUtils

abstract class NESNumberRepresentation(
    private val name: String,
    val requiresNextWord: Boolean,
    val isColor: Boolean,
    val languageNode: String = "NUMBER_FORMAT_$name",
) : ManagerResource {

    companion object {
        val DECIMAL = object : NESNumberRepresentation("DECIMAL", false, false) {
            override fun represent(first: UByte, second: UByte) = first.toString()
        }
        val HEXADECIMAL = object : NESNumberRepresentation("HEXADECIMAL", false, false) {
            override fun represent(first: UByte, second: UByte) = "$" + first.toHex(2)
        }
        val OCTAL = object : NESNumberRepresentation("OCTAL", false, false) {
            override fun represent(first: UByte, second: UByte) = "@" + first.toString(8)
        }
        val BINARY = object : NESNumberRepresentation("BINARY", false, false) {
            override fun represent(first: UByte, second: UByte) = "%" + StringUtils.addZeros(first.toString(2), 8)
        }
        val SHORT = object : NESNumberRepresentation("SHORT", true, false) {
            override fun represent(first: UByte, second: UByte) = (second concatenate first).toString()
        }
        val HEXADECIMAL_SHORT = object : NESNumberRepresentation("HEXADECIMAL_SHORT", true, false) {
            override fun represent(first: UByte, second: UByte) = "$" + (second concatenate first).toHex(4)
        }
        val NES_COLOR = object : NESNumberRepresentation("NES_COLOR", false, true) {
            override fun represent(first: UByte, second: UByte) =
                "#" + StringUtils.addZeros(PPUColors.COLORS[first.toInt() and 0x3F].toRGBA().toUInt().toString(16), 8)
        }
    }

    override fun getName() = name
    override fun getResourceProvider(): ResourceProvider = NES4JAMS.INSTANCE

    fun represent(address: UInt, pane: NESMemoryPane): String {
        var first: UByte = 0u
        var second: UByte = 0u
        pane.simulation.runSynchronized {
            first = pane.view.read(pane.simulation, address)
            second = if (requiresNextWord && address.inc().toInt() < pane.view.sizeOf(pane.simulation)) {
                pane.view.read(pane.simulation, address.inc())
            } else {
                0u
            }
        }

        return represent(first, second)
    }

    abstract fun represent(first: UByte, second: UByte): String
}