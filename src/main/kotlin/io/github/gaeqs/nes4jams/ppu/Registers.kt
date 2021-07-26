package io.github.gaeqs.nes4jams.ppu

import io.github.gaeqs.nes4jams.cartridge.TVType
import io.github.gaeqs.nes4jams.util.extension.shl
import io.github.gaeqs.nes4jams.util.extension.shr

data class PPUStatusRegister(var value: UByte) {

    var verticalBlank: UByte
        get() {
            return value shr 7 and 0x1u
        }
        set(value) {
            this.value = this.value and ((0x1u).toUByte() shl 7).inv() or (value and 0x1u shl 7)
        }

    var verticalZeroHit: UByte
        get() {
            return value shr 6 and 0x1u
        }
        set(value) {
            this.value = this.value and ((0x1u).toUByte() shl 6).inv() or (value and 0x1u shl 6)
        }

    var spriteOverflow: UByte
        get() {
            return value shr 5 and 0x1u
        }
        set(value) {
            this.value = this.value and ((0x1u).toUByte() shl 5).inv() or (value and 0x1u shl 5)
        }
}

data class PPUMaskRegister(var value: UByte) {

    val emphasizeBlue: UByte
        get() {
            return value shr 7
        }

    val emphasizeGreen: UByte
        get() {
            return value shr 6 and 0x1u
        }

    val emphasizeRed: UByte
        get() {
            return value shr 5 and 0x1u
        }

    val showSprites: UByte
        get() {
            return value shr 4 and 0x1u
        }

    val showBackground: UByte
        get() {
            return value shr 3 and 0x1u
        }

    val showSpritesInLeft: UByte
        get() {
            return value shr 2 and 0x1u
        }

    val showBackgroundInLeftmost: UByte
        get() {
            return value shr 1 and 0x1u
        }

    val grayscale: UByte
        get() {
            return value and 0x1u
        }

    val isRendering: Boolean get() = showBackground > 0u || showSprites > 0u
}

data class PPUControlRegister(var value: UByte) {

    val enableNmi: UByte
        get() {
            return value shr 7
        }

    val slaveMode: UByte
        get() {
            return value shr 6 and 0x1u
        }

    val spriteSize: UByte
        get() {
            return value shr 5 and 0x1u
        }

    val patternBackground: UByte
        get() {
            return value shr 4 and 0x1u
        }

    val patternSprite: UByte
        get() {
            return value shr 3 and 0x1u
        }

    val incrementMode: UByte
        get() {
            return value shr 2 and 0x1u
        }

    val nameTableY: UByte
        get() {
            return value shr 1 and 0x1u
        }

    val nameTableX: UByte
        get() {
            return value and 0x1u
        }
}

data class PPULoopyRegister(var value: UShort) {

    var fineY: UByte
        get() {
            return (value shr 12 and 0x7u).toUByte()
        }
        set(value) {
            this.value = this.value and ((7u).toUShort() shl 12).inv() or (value.toUShort() and 0x7u shl 12)
        }

    var nameTableY: UByte
        get() {
            return (value shr 11 and 0x1u).toUByte()
        }
        set(value) {
            this.value = this.value and ((1u).toUShort() shl 11).inv() or (value.toUShort() and 0x1u shl 11)
        }

    var nameTableX: UByte
        get() {
            return (value shr 10 and 0x1u).toUByte()
        }
        set(value) {
            this.value = this.value and ((1u).toUShort() shl 10).inv() or (value.toUShort() and 0x1u shl 10)
        }

    var coarseY: UByte
        get() {
            return (value shr 5 and 0x1Fu).toUByte()
        }
        set(value) {
            this.value = this.value and ((0x1Fu).toUShort() shl 5).inv() or (value.toUShort() and 0x1Fu shl 5)
        }


    var coarseX: UByte
        get() {
            return (value and 0x1Fu).toUByte()
        }
        set(value) {
            this.value = this.value and (0x1Fu).toUShort().inv() or (value.toUShort() and 0x1Fu)
        }

}