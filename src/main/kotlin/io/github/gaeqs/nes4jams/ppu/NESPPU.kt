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
import kotlin.experimental.and

class NESPPU(val simulation: NESSimulation) {

    companion object {
        val SCREEN_WIDTH = 256
        val SCREEN_WIDTH_SHIFT = 8
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

    var addressLatch = false
    var ppuDataBuffer: UByte = 0u
    var openbus: UByte = 0u

    var scanline = 0
        private set
    var cycle = 0
        private set
    var oddFrame = false
        private set
    var frameCompleted = false

    val screen = ByteArray(SCREEN_WIDTH * SCREEN_HEIGHT)

    private val backgroundRenderer = PPUBackgroundRenderer(this)
    private val spriteRenderer = PPUSpriteRenderer(this)

    fun isRequestingNMI() = control.enableNmi > 0u && status.verticalBlank > 0u

    fun cpuWrite(address: UShort, data: UByte) {
        openbus = data
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
                oamAddress++
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
                val value = backgroundRenderer.vRamAddress.value
                ppuWrite(value and 0x3FFFu, data)
                if (!mask.isRendering || scanline in 241..tvType.videoScanlines) {
                    val increment: UShort = if (control.incrementMode > 0u) 32u else 1u
                    backgroundRenderer.vRamAddress.value = (value + increment).toUShort()
                } else if (value.toUInt() and 0x7000u == 0x7000u) {
                    val yScroll = (value and 0x3E0u).toUInt()
                    backgroundRenderer.vRamAddress.value = value and 0xFFFu
                    val nValue = backgroundRenderer.vRamAddress.value
                    when (yScroll) {
                        0x3A0u -> backgroundRenderer.vRamAddress.value = nValue xor 0xBA0u
                        0x3E0u -> backgroundRenderer.vRamAddress.value = nValue xor 0x3E0u
                        else -> backgroundRenderer.vRamAddress.value = (nValue + 0x20u).toUShort()
                    }
                } else {
                    backgroundRenderer.vRamAddress.value = (value + 0x1000u).toUShort()
                }
            }
        }
    }

    fun cpuRead(address: UShort, readOnly: Boolean = false): UByte {
        if (readOnly) {
            // Return the current status and do nothing!
            return when (address.toUInt()) {
                // Status
                0x0002u -> (status.value and 0xE0u) or (ppuDataBuffer and 0x1Fu)
                // OAM Data
                0x0004u -> objectAttributeMemory[oamAddress.toInt() shr 2][oamAddress.toInt() and 0x3]
                // PPU Data
                0x0007u -> {
                    var data = ppuDataBuffer
                    data = if (backgroundRenderer.vRamAddress.value >= 0x3F00u)
                        ppuRead(backgroundRenderer.vRamAddress.value) else data
                    data
                }
                else -> openbus
            }
        }

        openbus = when (address.toUInt()) {
            // Status
            0x0002u -> {
                if (scanline == tvType.videoVerticalBlankLine) {
                    if (cycle == 2) {
                        // The VBL flag was just set. Clear it before we calculate the data
                        status.verticalBlank = 0u
                    }
                }
                val data = (status.value and 0xE0u) or (ppuDataBuffer and 0x1Fu)
                status.verticalBlank = 0u
                addressLatch = false
                data
            }
            // OAM Data
            0x0004u -> {
                objectAttributeMemory[oamAddress.toInt() shr 2][oamAddress.toInt() and 0x3]
            }
            // PPU Data
            0x0007u -> {

                val data = if (backgroundRenderer.vRamAddress.value and 0x3FFFu < 0x3F00u) {
                    val temp = ppuDataBuffer
                    ppuDataBuffer = ppuRead(backgroundRenderer.vRamAddress.value and 0x3FFFu)
                    temp
                } else {
                    ppuDataBuffer = ppuRead(((backgroundRenderer.vRamAddress.value and 0x3FFFu) - 0x1000u).toUShort())
                    ppuRead(backgroundRenderer.vRamAddress.value and 0x3FFFu)
                }

                if (!mask.isRendering || scanline > 240) {
                    if (control.incrementMode > 0u) {
                        backgroundRenderer.vRamAddress.value = (backgroundRenderer.vRamAddress.value + 32u).toUShort()
                    } else {
                        backgroundRenderer.vRamAddress.value++
                    }
                } else {
                    backgroundRenderer.incrementScrollX()
                    backgroundRenderer.incrementScrollY()
                }

                data
            }
            else -> openbus
        }
        return openbus
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
                simulation.cartridge.mirroring.map(nameTables, address)
            }
            in 0x3F00u..0x3FFFu -> {
                val i = address.toInt()
                palette[if (i and 0b11 == 0) i and 0x0F else i and 0x001F]
            }
            else -> 0u
        }
    }

    fun clock() {

        if (scanline == -1 && cycle == 1) {
            status.spriteOverflow = 0u
            status.verticalZeroHit = 0u
            spriteRenderer.resetShifter()
        }

        // VBL flag should be cleared one cycle after because of its delay.
        if (scanline == -1 && cycle == 2) {
            status.verticalBlank = 0u
        }

        if (scanline == -1 && cycle in 280..304 && mask.isRendering) {
            backgroundRenderer.vRamAddress.value = backgroundRenderer.tRamAddress.value
        }

        // Skip on odd frame
        if (scanline == 0 && cycle == 0 && mask.isRendering && oddFrame) cycle = 1

        // On render updates
        if (scanline in -1 until 240 && cycle in 258..341) {
            oamAddress = 0u
        }

        // SCANLINE 240 does nothing :)
        if (scanline == tvType.videoVerticalBlankLine && cycle == 1) {
            status.verticalBlank = 1u
        }

        if (cycle == 257) {
            simulation.cartridge.mapper.onScanLine()
        }


        val (pixel, palette) = clockRenderers()
        paint(pixel, palette)

        moveToNextClock()
    }

    private fun clockRenderers(): Pair<UByte, UByte> {
        val (bgPixel, bgPalette) = backgroundRenderer.clock(scanline, cycle)
        val (fgPixel, fgPalette, fgPriority) = spriteRenderer.clock(scanline, cycle)
        val bgPixel0 = bgPixel.isZero()
        val fgPixel0 = fgPixel.isZero()

        return when {
            bgPixel0 && fgPixel0 -> ZERO_PAIR
            bgPixel0 && !fgPixel0 -> Pair(fgPixel, fgPalette)
            !bgPixel0 && fgPixel0 -> Pair(bgPixel, bgPalette)
            else -> {
                updateZeroHit()
                if (fgPriority) Pair(fgPixel, fgPalette) else Pair(bgPixel, bgPalette)
            }
        }
    }

    private fun paint(pixel: UByte, palette: UByte) {
        val x = cycle - 1
        val y = scanline
        if (x in 0 until SCREEN_WIDTH && y in 0 until SCREEN_HEIGHT) {
            val pointer = x + (y shl SCREEN_WIDTH_SHIFT)
            if (mask.isRendering) {
                val value = ppuRead((0x03F00u + (palette shl 2) + pixel).toUShort()).toByte()
                screen[pointer] = if (mask.grayscale) value and 0x30 else value and 0x3F
            } else {
                // Write PPU address
                val loopyV = backgroundRenderer.vRamAddress.value
                screen[pointer] = (if (loopyV > 0x3F00u) ppuRead(loopyV) else this.palette[0]).toByte()
            }
        }
    }

    private fun moveToNextClock() {
        cycle++
        if (cycle > 340) {
            cycle = 0
            scanline++
            if (scanline > tvType.videoScanlines) {
                scanline = -1
                frameCompleted = true
                oddFrame = !oddFrame
            }
        }
    }

    private fun updateZeroHit() {
        if (spriteRenderer.spriteZeroHitPossible && spriteRenderer.spriteZeroBeingRendered && mask.isRendering) {
            if (!mask.showBackgroundInLeft && !mask.showSpritesInLeft) {
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

    fun reset() {
        nameTables.forEach { it.fill(0u) }
        palette.fill(0u)
        patternTables.forEach { it.fill(0u) }
        objectAttributeMemory.forEach { it.attribute = 0u; it.id = 0u; it.x = 0u; it.y = 0u }
        oamAddress = 0u
        control.value = 0u
        status.value = 0u
        mask.value = 0u
        addressLatch = false
        ppuDataBuffer = 0u
        scanline = 0
        cycle = 0
        frameCompleted = false
        screen.fill(0)
        backgroundRenderer.reset()
        spriteRenderer.reset()
    }

}