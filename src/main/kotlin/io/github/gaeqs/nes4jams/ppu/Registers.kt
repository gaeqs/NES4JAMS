package io.github.gaeqs.nes4jams.ppu

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

class PPUMaskRegister(value: UByte) {

    var emphasizeBlue: Boolean = value shr 7 > 0u
        private set
    var emphasizeGreen: Boolean = value shr 6 and 0x1u > 0u
        private set
    var emphasizeRed: Boolean = value shr 5 and 0x1u > 0u
        private set
    var showSprites: Boolean = value shr 4 and 0x1u > 0u
        private set
    var showBackground: Boolean = value shr 3 and 0x1u > 0u
        private set
    var showSpritesInLeft: Boolean = value shr 2 and 0x1u > 0u
        private set
    var showBackgroundInLeftmost: Boolean = value shr 1 and 0x1u > 0u
        private set
    var grayscale: Boolean = value and 0x1u > 0u
        private set
    var isRendering: Boolean = showBackground || showSprites
        private set

    var value: UByte = value
        set(value) {
            field = value
            emphasizeBlue = value shr 7 > 0u
            emphasizeGreen = value shr 6 and 0x1u > 0u
            emphasizeRed = value shr 5 and 0x1u > 0u
            showSprites = value shr 4 and 0x1u > 0u
            showBackground = value shr 3 and 0x1u > 0u
            showSpritesInLeft = value shr 2 and 0x1u > 0u
            showBackgroundInLeftmost = value shr 1 and 0x1u > 0u
            grayscale = value and 0x1u > 0u
            isRendering = showBackground || showSprites
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PPUMaskRegister

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "PPUMaskRegister(emphasizeBlue=$emphasizeBlue, emphasizeGreen=$emphasizeGreen, emphasizeRed=$emphasizeRed, showSprites=$showSprites, showBackground=$showBackground, showSpritesInLeft=$showSpritesInLeft, showBackgroundInLeftmost=$showBackgroundInLeftmost, grayscale=$grayscale, isRendering=$isRendering, value=$value)"
    }


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