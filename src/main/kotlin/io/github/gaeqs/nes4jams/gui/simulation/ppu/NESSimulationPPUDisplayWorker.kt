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

@file:OptIn(ExperimentalUnsignedTypes::class)

package io.github.gaeqs.nes4jams.gui.simulation.ppu

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
    private val decodedBackgroundPatternTable = UByteArray(
        NESSimulationPPUDisplay.PATTERN_TABLE_SIZE * NESSimulationPPUDisplay.PATTERN_TABLE_SIZE
    )

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
                    decodeMirroring(data)
                    decodePalettes(data)
                    ppuData = null
                } catch (ex: InterruptedException) {
                    running = false
                }
            }
        }
    }

    private fun decodePatternTables(data: UByteArray) {
        display.patternTables[0].startDataTransmission {
            patternTableTransmissionColor(data, it, 0)
        }

        patternTableTransmission(data, decodedBackgroundPatternTable, 0x1000)
        display.patternTables[1].startDataTransmission {
            val selectedPalette = display.selectedPalette.id
            for (i in it.indices) {
                val pixel = decodedBackgroundPatternTable[i]
                it[i] = PPUColors.INT_COLORS[data[0x3F00 + (selectedPalette shl 2) + pixel.toInt()].toInt()]
            }
        }
    }

    private fun patternTableTransmission(data: UByteArray, buffer: UByteArray, patternOffset: Int) {
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val offset = y * 256 + x * 16
                for (row in 0 until 8) {
                    var least = data[patternOffset + offset + row]
                    var most = data[patternOffset + offset + row + 8]
                    for (column in 0 until 8) {
                        val pixel = (most and 0x01u shl 1) or (least and 0x01u)
                        most = most shr 1
                        least = least shr 1

                        val px = (x shl 3) + 7 - column
                        val py = (y shl 3) + row
                        buffer[px + (py shl 7)] = pixel
                    }
                }
            }
        }
    }

    private fun patternTableTransmissionColor(data: UByteArray, buffer: IntArray, patternOffset: Int) {
        val selectedPalette = display.selectedPalette.id
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val offset = y * 256 + x * 16
                for (row in 0 until 8) {
                    var least = data[patternOffset + offset + row]
                    var most = data[patternOffset + offset + row + 8]
                    for (column in 0 until 8) {
                        val pixel = (most and 0x01u shl 1) or (least and 0x01u)
                        most = most shr 1
                        least = least shr 1

                        val px = (x shl 3) + 7 - column
                        val py = (y shl 3) + row
                        buffer[px + (py shl 7)] =
                            PPUColors.INT_COLORS[data[0x3F00 + (selectedPalette shl 2) + pixel.toInt()].toInt()]
                    }
                }
            }
        }
    }

    private fun decodeMirroring(data: UByteArray) {
        display.nameTables.startDataTransmission { buffer ->
            for (ry in 0..1) {
                for (rx in 0..1) {
                    val start = 0x2000 + ((ry shl 1) + rx) * 0x400
                    val attributeStart = start + 0x3C0
                    val bufferOffsetX = rx * 256
                    val bufferOffsetY = ry * 240

                    for (y in 0 until 30) {
                        for (x in 0 until 32) {
                            // https://www.nesdev.org/wiki/PPU_attribute_tables
                            val paletteSet = data[attributeStart + (x shr 2) + (y shr 2 shl 3)].toInt()
                            val palette = (paletteSet shr ((x and 2) + (y and 2 shl 1))) and 0x3

                            val id = data[start + (y shl 5) + x].toInt()

                            val bufferX = (x shl 3) + bufferOffsetX
                            val bufferY = (y shl 3) + bufferOffsetY

                            val startX = (id and 0b1111) shl 3
                            val startY = (id ushr 4) shl 3

                            for (bx in 0 until 8) {
                                for (by in 0 until 8) {
                                    val color = decodedBackgroundPatternTable[startX + bx + ((startY + by) shl 7)]
                                    val finalX = bufferX + bx
                                    val finalY = bufferY + by
                                    buffer[finalX + (finalY shl 9)] =
                                        PPUColors.INT_COLORS[data[0x3F00 + (palette shl 2) + color.toInt()].toInt()]
                                }
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
                it.fill = PPUColors.COLORS[data[i++].toInt() % PPUColors.COLORS.size]
            }
        }
    }
}
