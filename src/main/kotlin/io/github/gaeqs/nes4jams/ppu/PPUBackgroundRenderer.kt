package io.github.gaeqs.nes4jams.ppu

import io.github.gaeqs.nes4jams.util.extension.concatenate
import io.github.gaeqs.nes4jams.util.extension.shl
import io.github.gaeqs.nes4jams.util.extension.shr

class PPUBackgroundRenderer(private val ppu: NESPPU) {

    var fineX: UByte = 0u
    val vRamAddress = PPULoopyRegister(0u)
    val tRamAddress = PPULoopyRegister(0u)

    private var nextTileId: UByte = 0u
    private var nextTileAttribute: UByte = 0u
    private var nextTileLSB: UByte = 0u
    private var nextTileMSB: UByte = 0u
    private var shifterPatternLow: UShort = 0u
    private var shifterPatternHigh: UShort = 0u
    private var shifterAttributeLow: UShort = 0u
    private var shifterAttributeHigh: UShort = 0u


    fun clock(scanline: Int, cycle: Int): Pair<UByte, UByte> {
        // Visible scanlines
        if (scanline in -1 until 240) {
            if (cycle in 2 until 258 || cycle in 321 until 338) {
                updateShifters()
                when ((cycle - 1) and 0x7) {
                    0 -> {
                        loadBackgroundShifters()
                        loadBackgroundNextTileId()
                    }
                    2 -> loadBackgroundNextTileAttribute()
                    4 -> loadBackgroundNextLeastSignificantBytes()
                    6 -> loadBackgroundNextMostSignificantBytes()
                    7 -> incrementScrollX()
                }
            }

            when (cycle) {
                256 -> incrementScrollY()
                257 -> {
                    loadBackgroundShifters()
                    transferAddressX()
                }
                338, 340 -> loadBackgroundNextTileId()
                in 280 until 305 -> if (scanline == -1) transferAddressY()
            }
        }

        if (ppu.mask.showBackground > 0u) {
            val bitMux = (0x8000u).toUShort() shr fineX.toInt()
            val pixel = (shifterPatternHigh and bitMux > 0u) concatenate (shifterPatternLow and bitMux > 0u)
            val palette = (shifterAttributeHigh and bitMux > 0u) concatenate (shifterAttributeLow and bitMux > 0u)
            return Pair(pixel, palette)
        }
        return Pair(0u, 0u)
    }


    private fun loadBackgroundNextTileId() {
        var address: UShort = 0x2000u
        address = (address or (vRamAddress.value and 0x0FFFu))
        nextTileId = ppu.ppuRead(address)
    }

    private fun loadBackgroundNextTileAttribute() {
        var address: UShort = 0x23C0u
        address = address or (vRamAddress.nameTableY.toUShort() shl 11)
        address = address or (vRamAddress.nameTableX.toUShort() shl 10)
        address = address or (vRamAddress.coarseY.toUShort() shr 2 shl 3)
        address = address or (vRamAddress.coarseX.toUShort() shr 2)

        nextTileAttribute = ppu.ppuRead(address)
        if (vRamAddress.coarseY and 0x02u > 0u) nextTileAttribute = nextTileAttribute shr 4
        if (vRamAddress.coarseX and 0x02u > 0u) nextTileAttribute = nextTileAttribute shr 2
        nextTileAttribute = nextTileAttribute and 0x03u
    }

    private fun loadBackgroundNextLeastSignificantBytes() {
        var address = ppu.control.patternBackground.toUShort() shl 12
        address = (address + (nextTileId.toUShort() shl 4)).toUShort()
        address = (address + (vRamAddress.fineY) + 0u).toUShort()
        nextTileLSB = ppu.ppuRead(address)
    }

    private fun loadBackgroundNextMostSignificantBytes() {
        var address = ppu.control.patternBackground.toUShort() shl 12
        address = (address + (nextTileId.toUShort() shl 4)).toUShort()
        address = (address + (vRamAddress.fineY) + 8u).toUShort()
        nextTileMSB = ppu.ppuRead(address)
    }

    private fun incrementScrollX() {
        if (ppu.mask.showBackground > 0u || ppu.mask.showSprites > 0u) {
            if (vRamAddress.coarseX.toUInt() == 31u) {
                vRamAddress.coarseX = 0u
                vRamAddress.nameTableX = vRamAddress.nameTableX.inv()
            } else {
                vRamAddress.coarseX++
            }
        }
    }

    private fun incrementScrollY() {
        if (ppu.mask.showBackground > 0u || ppu.mask.showSprites > 0u) {
            if (vRamAddress.fineY < 7u) {
                vRamAddress.fineY++
            } else {
                vRamAddress.fineY = 0u
                when (vRamAddress.coarseY.toUInt()) {
                    29u -> {
                        vRamAddress.coarseY = 0u
                        vRamAddress.nameTableY = vRamAddress.nameTableY.inv()
                    }
                    31u -> vRamAddress.coarseY = 0u
                    else -> vRamAddress.coarseY++
                }
            }
        }
    }

    private fun transferAddressX() {
        if (ppu.mask.showBackground > 0u || ppu.mask.showSprites > 0u) {
            vRamAddress.nameTableX = tRamAddress.nameTableX
            vRamAddress.coarseX = tRamAddress.coarseX
        }
    }

    private fun transferAddressY() {
        if (ppu.mask.showBackground > 0u || ppu.mask.showSprites > 0u) {
            vRamAddress.fineY = tRamAddress.fineY
            vRamAddress.nameTableY = tRamAddress.nameTableY
            vRamAddress.coarseY = tRamAddress.coarseY
        }
    }

    private fun loadBackgroundShifters() {
        shifterPatternLow = shifterPatternLow and 0xFF00u or nextTileLSB.toUShort()
        shifterPatternHigh = shifterPatternHigh and 0xFF00u or nextTileMSB.toUShort()

        val extendedLow: UShort = if (nextTileAttribute and 0b01u > 0u) 0xFFu else 0x00u
        val extendedHigh: UShort = if (nextTileAttribute and 0b10u > 0u) 0xFFu else 0x00u
        shifterAttributeLow = (shifterAttributeLow and 0xFF00u) or extendedLow
        shifterAttributeHigh = (shifterAttributeHigh and 0xFF00u) or extendedHigh
    }

    private fun updateShifters() {
        if (ppu.mask.showBackground > 0u) {
            shifterPatternLow = shifterPatternLow shl 1
            shifterPatternHigh = shifterPatternHigh shl 1
            shifterAttributeLow = shifterAttributeLow shl 1
            shifterAttributeHigh = shifterAttributeHigh shl 1
        }
    }

    fun reset() {
        fineX = 0u
        vRamAddress.value = 0u
        tRamAddress.value = 0u
        nextTileId = 0u
        nextTileAttribute = 0u
        nextTileLSB = 0u
        nextTileMSB = 0u
        shifterPatternLow = 0u
        shifterPatternHigh = 0u
        shifterAttributeLow = 0u
        shifterAttributeHigh = 0u
    }
}