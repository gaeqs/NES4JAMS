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

package io.github.gaeqs.nes4jams.ppu

import io.github.gaeqs.nes4jams.simulation.NESSimulation
import io.github.gaeqs.nes4jams.util.extension.isZero
import io.github.gaeqs.nes4jams.util.extension.shl
import io.github.gaeqs.nes4jams.util.extension.shr

class NESPPU(val simulation: NESSimulation) {

    companion object {
        val SCREEN_WIDTH = 256
        val SCREEN_HEIGHT = 240
        private val ZERO_PAIR = Pair<UByte, UByte>(0u, 0u)
    }

    val tvType = simulation.cartridge.header.tvType

    val nameTables = Array(2) { UByteArray(1024) }
    val palette = UByteArray(32)
    val patternTables = Array(2) { UByteArray(4096) }

    val objectAttributeMemory = Array(64) { PPUSprite() }
    var oamAddress: UByte = 0u

    val control = PPUControlRegister(0u)
    val status = PPUStatusRegister(0u)
    val mask = PPUMaskRegister(0u)
    var nmiRequest = false

    var addressLatch = false
    var ppuDataBuffer: UByte = 0u

    var scanline = 0
        private set
    var cycle = 0
        private set
    var frameCompleted = false

    val screen = ByteArray(SCREEN_WIDTH * SCREEN_HEIGHT)

    private val backgroundRenderer = PPUBackgroundRenderer(this)
    private val spriteRenderer = PPUSpriteRenderer(this)

    fun cpuWrite(address: UShort, data: UByte) {
        when (address.toUInt()) {
            // Control
            0x0000u -> {
                control.value = data
                backgroundRenderer.tRamAddress.nameTableX = control.nameTableX
                backgroundRenderer.tRamAddress.nameTableY = control.nameTableY
            }
            // Mask
            0x0001u -> mask.value = data
            // Status
            0x0002u -> {
            }
            // OAM Address
            0x0003u -> {
                oamAddress = data
            }
            // OAM Data
            0x0004u -> {
                objectAttributeMemory[oamAddress.toInt() shr 2][oamAddress.toInt() and 0x3] = data
            }
            // Scroll
            0x0005u -> {
                if (addressLatch) {
                    backgroundRenderer.tRamAddress.fineY = data and 0x7u
                    backgroundRenderer.tRamAddress.coarseY = data shr 3
                    addressLatch = false
                } else {
                    backgroundRenderer.fineX = data and 0x7u
                    backgroundRenderer.tRamAddress.coarseX = data shr 3
                    addressLatch = true
                }
            }
            // PPU Address
            0x0006u -> {
                if (addressLatch) {
                    backgroundRenderer.tRamAddress.value =
                        backgroundRenderer.tRamAddress.value and 0xFF00u or data.toUShort()
                    backgroundRenderer.vRamAddress.value = backgroundRenderer.tRamAddress.value
                    addressLatch = false
                } else {
                    backgroundRenderer.tRamAddress.value =
                        backgroundRenderer.tRamAddress.value and 0x00FFu or (data.toUShort() shl 8)
                    addressLatch = true
                }
            }
            // PPU Data
            0x0007u -> {
                ppuWrite(backgroundRenderer.vRamAddress.value, data)
                backgroundRenderer.vRamAddress.value =
                    (backgroundRenderer.vRamAddress.value + (if (control.incrementMode > 0u) 32u else 1u)).toUShort()
            }
        }
    }

    fun cpuRead(address: UShort, readOnly: Boolean = false): UByte {
        return when (address.toUInt()) {
            // Control
            0x0000u -> 0u
            // Mask
            0x0001u -> 0u
            // Status
            0x0002u -> {
                val data = (status.value and 0xE0u) or (ppuDataBuffer and 0x1Fu)
                status.verticalBlank = 0u
                addressLatch = false
                data
            }
            // OAM Address
            0x0003u -> {
                0u
            }
            // OAM Data
            0x0004u -> {
                objectAttributeMemory[oamAddress.toInt() shr 2][oamAddress.toInt() and 0x3]
            }
            // Scroll
            0x0005u -> {
                0u
            }
            // PPU Address
            0x0006u -> {
                0u
            }
            // PPU Data
            0x0007u -> {
                var data = ppuDataBuffer

                ppuDataBuffer = ppuRead(backgroundRenderer.vRamAddress.value)

                data = if (backgroundRenderer.vRamAddress.value >= 0x3F00u) ppuDataBuffer else data

                backgroundRenderer.vRamAddress.value =
                    (backgroundRenderer.vRamAddress.value + (if (control.incrementMode > 0u) 32u else 1u)).toUShort()
                data
            }
            else -> 0u
        }
    }

    fun ppuWrite(address: UShort, data: UByte) {
        if (simulation.cartridge.ppuWrite(address, data)) return

        when (address) {
            in 0x0000u..0x1FFFu -> {
                patternTables[(address and 0x1000u shr 12).toInt()][(address and 0x0FFFu).toInt()] = data
            }
            in 0x2000u..0x3EFFu -> {
                when (simulation.cartridge.mirroring) {
                    Mirror.VERTICAL -> {
                        when (address and 0x0FFFu) {
                            in 0x0000u..0x03FFu,
                            in 0x0800u..0x0BFFu -> nameTables[0][(address and 0x03FFu).toInt()] = data
                            else -> nameTables[1][(address and 0x03FFu).toInt()] = data
                        }
                    }
                    Mirror.HORIZONTAL -> {
                        when (address and 0x0FFFu) {
                            in 0x0000u..0x07FFu -> nameTables[0][(address and 0x03FFu).toInt()] = data
                            else -> nameTables[1][(address and 0x03FFu).toInt()] = data
                        }
                    }
                    else -> {
                        println("READ NOT SUPPORTED ${simulation.cartridge.mirroring}")
                    }
                }
            }
            in 0x3F00u..0x3FFFu -> {
                val masked = when ((address and 0x001Fu).toUInt()) {
                    0x0010u -> 0x0000
                    0x0014u -> 0x0004
                    0x0018u -> 0x0008
                    0x001Cu -> 0x000C
                    else -> (address.toInt() and 0x001F)
                }
                palette[masked] = data
            }
        }
    }

    fun ppuRead(address: UShort): UByte {
        val (success, data) = simulation.cartridge.ppuRead(address)
        if (success) return data

        return when (address) {
            in 0x0000u..0x1FFFu -> {
                patternTables[(address and 0x1000u shr 12).toInt()][(address and 0x0FFFu).toInt()]
            }
            in 0x2000u..0x3EFFu -> {
                simulation.cartridge.mirroring.map(nameTables, address) ?: 0u
            }
            in 0x3F00u..0x3FFFu -> {
                val masked = when ((address and 0x001Fu).toUInt()) {
                    0x0010u -> 0x0000
                    0x0014u -> 0x0004
                    0x0018u -> 0x0008
                    0x001Cu -> 0x000C
                    else -> (address.toInt() and 0x001F)
                }
                palette[masked]
            }
            else -> 0u
        }
    }

    fun clock() {
        if (scanline == 0 && cycle == 0) cycle = 1 // Odd frame cycle skip
        if (scanline == -1 && cycle == 1) {
            status.verticalBlank = 0u
            status.spriteOverflow = 0u
            status.verticalZeroHit = 0u
            spriteRenderer.reset()
        }

        val (bgPixel, bgPalette) = backgroundRenderer.clock(scanline, cycle)
        val (fgPixel, fgPalette, fgPriority) = spriteRenderer.clock(scanline, cycle)
        val bgPixel0 = bgPixel.isZero()
        val fgPixel0 = fgPixel.isZero()

        // SCANLINE 240 does nothing :)
        if (scanline == tvType.videoVerticalBlankLine && cycle == 1) {
            status.verticalBlank = 1u
            if (control.enableNmi > 0u) {
                nmiRequest = true
            }
        }

        val (pixel: UByte, palette: UByte) = when {
            bgPixel0 && fgPixel0 -> ZERO_PAIR
            bgPixel0 && !fgPixel0 -> Pair(fgPixel, fgPalette)
            !bgPixel0 && fgPixel0 -> Pair(bgPixel, bgPalette)
            else -> {
                updateZeroHit()
                if (fgPriority) Pair(fgPixel, fgPalette) else Pair(bgPixel, bgPalette)
            }
        }

        if (cycle - 1 in 0 until SCREEN_WIDTH && scanline in 0 until SCREEN_HEIGHT) {
            screen[cycle - 1 + scanline * SCREEN_WIDTH] =
                ppuRead((0x03F00u + (palette shl 2) + pixel).toUShort()).toByte()
        }

        cycle++

        if (cycle == 260 && scanline < 240 && (mask.showBackground > 0u || mask.showSprites > 0u)) {
            simulation.cartridge.mapper.onScanLine()
        }

        if (cycle > 340) {
            cycle = 0
            scanline++
            if (scanline >= 260) {
                scanline = -1
                frameCompleted = true
            }
        }
    }

    private fun updateZeroHit() {
        if (spriteRenderer.spriteZeroHitPossible && spriteRenderer.spriteZeroBeingRendered
            && mask.showBackground > 0u && mask.showSprites > 0u
        ) {
            if ((mask.showBackgroundInLeftmost or mask.showSpritesInLeft).inv() > 0u) {
                if (cycle in 9 until 258) {
                    status.verticalZeroHit = 1u
                }
            } else {
                if (cycle in 1 until 258) {
                    status.verticalZeroHit = 1u
                }
            }
        }
    }

}