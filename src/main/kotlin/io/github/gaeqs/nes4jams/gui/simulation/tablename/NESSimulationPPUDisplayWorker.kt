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

package io.github.gaeqs.nes4jams.gui.simulation.tablename

import io.github.gaeqs.nes4jams.ppu.PPUColors
import io.github.gaeqs.nes4jams.util.extension.shl
import io.github.gaeqs.nes4jams.util.extension.shr
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class NESSimulationPPUDisplayWorker(val display: NESSimulationPPUDisplay) : Thread() {

    @Volatile
    private var running = true

    private val lock = ReentrantLock()
    private val wait = lock.newCondition()

    private var ppuData: UByteArray? = null

    init {
        priority = MIN_PRIORITY
        isDaemon = true
    }

    fun stopWorker() {
        running = false
        interrupt()
    }

    fun push(ppuData: UByteArray) {
        lock.withLock {
            if (this.ppuData != null) return
            this.ppuData = ppuData
            wait.signalAll()
        }
    }

    fun generateAndPushIfPossible(generator: () -> UByteArray) {
        lock.withLock {
            if (this.ppuData != null) return
            this.ppuData = generator()
            wait.signalAll()
        }
    }

    override fun run() {
        while (running) {
            lock.withLock {
                try {
                    while (ppuData == null) wait.await()
                    val data = ppuData!!
                    decodePatternTables(data)
                    decodePalettes(data)
                    ppuData = null
                } catch (ex: InterruptedException) {
                    running = false
                }
            }
        }
    }

    private fun decodePatternTables(data: UByteArray) {
        val selectedPalette = display.selectedPalette.id
        display.patternTables.forEachIndexed { index, table ->
            val patterOffset = 0x1000 * index

            table.startDataTransmission { buffer ->
                for (y in 0 until 16) {
                    for (x in 0 until 16) {
                        val offset = y * 256 + x * 16
                        for (row in 0 until 8) {
                            var most = data[patterOffset + offset + row]
                            var least = data[patterOffset + offset + row + 8]
                            for (column in 0 until 8) {
                                val pixel = (most and 0x01u shl 1) or (least and 0x01u)
                                most = most shr 1
                                least = least shr 1

                                val px = (x shl 3) + 7 - column
                                val py = (y shl 3) + row
                                buffer[px + (py shl 7)] =
                                    PPUColors.INT_COLORS[data[0x03F00 + (selectedPalette shl 2) + pixel.toInt()].toInt()]
                            }
                        }
                    }
                }
            }

        }
    }

    private fun decodePalettes(data: UByteArray) {
        var i = 0x3F00
        display.palettes.forEach { palette ->
            palette.rectangles.forEach {
                it.fill = PPUColors.COLORS[data[i++].toInt()]
            }
        }
    }
}
