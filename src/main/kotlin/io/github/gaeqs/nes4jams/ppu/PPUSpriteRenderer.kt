package io.github.gaeqs.nes4jams.ppu

import io.github.gaeqs.nes4jams.util.extension.concatenate
import io.github.gaeqs.nes4jams.util.extension.flip
import io.github.gaeqs.nes4jams.util.extension.isZero
import io.github.gaeqs.nes4jams.util.extension.shl

class PPUSpriteRenderer(private val ppu: NESPPU) {

    companion object {
        private val ZERO_TRIPLE = Triple((0u).toUByte(), (0u).toUByte(), false)
    }

    private val scanlineSprites = Array(64) { PPUSprite(0u, 0u, 0u, 0u) }
    private var spriteCount = 0
    private val shifterPatternLow = UByteArray(8)
    private val shifterPatternHigh = UByteArray(8)

    var spriteZeroHitPossible = false
    var spriteZeroBeingRendered = false

    fun reset() {
        scanlineSprites.forEach { it.attribute = 0u; it.id = 0u; it.x = 0u; it.y = 0u }
        spriteCount = 0
        resetShifter()
    }

    fun resetShifter() {
        shifterPatternLow.fill(0u)
        shifterPatternHigh.fill(0u)
    }

    fun clock(scanline: Int, cycle: Int): Triple<UByte, UByte, Boolean> {
        // Sprite renderer should work even if the show sprite flag is not set.
        // If both sprites and background are not being rendered, this code shouldn't be executed.
        if (!ppu.mask.isRendering || scanline !in -1 until 240) return ZERO_TRIPLE
        when (cycle) {
            257 -> if (scanline >= 0) populateSpriteArray(scanline)
            340 -> repeat(spriteCount) { generateSpriteShifters(scanline, it) }
        }

        spriteZeroBeingRendered = false
        if (!ppu.mask.showSprites || cycle !in 1..256) return ZERO_TRIPLE
        return clockSprites(cycle)
    }

    private fun clockSprites(cycle: Int): Triple<UByte, UByte, Boolean> {
        var found: Triple<UByte, UByte, Boolean>? = null
        for (i in 0 until spriteCount) {
            if (scanlineSprites[i].x.isZero()) {
                if (found != null || !ppu.mask.showSpritesInLeft && cycle < 9) {
                    // If the pixel was already found in a previous sprite or sprites are not being shown in left,
                    // just shift this sprite's registers.
                    shifterPatternLow[i] = shifterPatternLow[i] shl 1
                    shifterPatternHigh[i] = shifterPatternHigh[i] shl 1
                    continue
                }
                val pixel = (shifterPatternHigh[i] and 0x80u > 0u) concatenate (shifterPatternLow[i] and 0x80u > 0u)
                val palette = ((scanlineSprites[i].attribute and 0x03u) + 0x04u).toUByte()
                val priority = (scanlineSprites[i].attribute and 0x20u).isZero()

                // Pixel found
                if (pixel > 0u) {
                    // Last cycle should ignore sprite 0 check.
                    if (i == 0 && cycle != 256) spriteZeroBeingRendered = true
                    found = Triple(pixel, palette, priority)
                }

                // Shift
                shifterPatternLow[i] = shifterPatternLow[i] shl 1
                shifterPatternHigh[i] = shifterPatternHigh[i] shl 1
            } else {
                scanlineSprites[i].x--
            }
        }
        return found ?: ZERO_TRIPLE
    }

    private fun populateSpriteArray(scanline: Int) {
        spriteZeroHitPossible = false
        scanlineSprites.forEach { it.fill(0xFFu) }
        spriteCount = 0
        var entry = 0
        var overflow = false
        while (entry < 64 && !overflow) {
            val diff = (scanline.toShort() - ppu.objectAttributeMemory[entry].y.toShort())

            if (diff >= 0 && diff < if (ppu.control.spriteSize > 0u) 16 else 8) {

                if (spriteCount < 8) {
                    if (entry == 0) spriteZeroHitPossible = true
                    scanlineSprites[spriteCount].moveFrom(ppu.objectAttributeMemory[entry])
                    spriteCount++
                } else {
                    overflow = true
                    ppu.status.spriteOverflow = 1u
                }
            }

            entry++
        }
    }

    private fun generateSpriteShifters(scanline: Int, i: Int) {
        val spritePatternAddressLow: UShort = if (ppu.control.spriteSize > 0u) {
            // 8x16 MODE
            val flip = scanlineSprites[i].attribute and 0x80u > 0u
            val bottom = if ((scanline - scanlineSprites[i].y.toInt() < 8) xor flip) 0u else 1u
            val temp = (ppu.control.patternSprite.toUShort() and 0x01u shl 12) or
                    (((scanlineSprites[i].id.toUShort() and 0xFEu) + bottom).toUShort() shl 4)
            val offset = if (!flip) scanline.toUShort() - scanlineSprites[i].y
            else 7u - scanline.toUShort() + scanlineSprites[i].y

            temp or (offset.toUShort() and 0x07u)
        } else {
            // 8x8 MODE
            val flip = scanlineSprites[i].attribute and 0x80u > 0u
            val temp = (ppu.control.patternSprite.toUShort() shl 12) or (scanlineSprites[i].id.toUShort() shl 4)
            val offset = scanline.toUShort() - scanlineSprites[i].y

            (temp or (if (flip) 7u - offset else offset).toUShort())
        }

        if (scanlineSprites[i].attribute and 0x40u > 0u) {
            shifterPatternLow[i] = ppu.ppuRead(spritePatternAddressLow).flip()
            shifterPatternHigh[i] = ppu.ppuRead((spritePatternAddressLow + 8u).toUShort()).flip()
        } else {
            shifterPatternLow[i] = ppu.ppuRead(spritePatternAddressLow)
            shifterPatternHigh[i] = ppu.ppuRead((spritePatternAddressLow + 8u).toUShort())
        }
    }
}