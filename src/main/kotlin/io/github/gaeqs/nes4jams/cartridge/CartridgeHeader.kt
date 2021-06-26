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

package io.github.gaeqs.nes4jams.cartridge

import io.github.gaeqs.nes4jams.ppu.Mirror
import io.github.gaeqs.nes4jams.utils.BIT0
import io.github.gaeqs.nes4jams.utils.BIT1
import io.github.gaeqs.nes4jams.utils.BIT2
import io.github.gaeqs.nes4jams.utils.BIT3
import io.github.gaeqs.nes4jams.utils.extension.shl
import io.github.gaeqs.nes4jams.utils.extension.shr
import java.io.InputStream
import java.nio.charset.StandardCharsets

data class CartridgeHeader(
    var name: String,           // 0-3 NES<EOF>
    var prgRomData: UByte,
    var chrRomData: UByte,
    var flag6: UByte,
    var flag7: UByte,
    var flag8: UByte,
    var flag9: UByte,
    var flag10: UByte,
    var flag11: UByte,
    var flag12: UByte,
    var flag13: UByte,
    var flag14: UByte,
    var flag15: UByte,
) {

    constructor(inputStream: InputStream) : this(
        String(inputStream.readNBytes(4), 0, 4, StandardCharsets.US_ASCII),
        inputStream.read().toUByte(),
        inputStream.read().toUByte(),
        inputStream.read().toUByte(),
        inputStream.read().toUByte(),
        inputStream.read().toUByte(),
        inputStream.read().toUByte(),
        inputStream.read().toUByte(),
        inputStream.read().toUByte(),
        inputStream.read().toUByte(),
        inputStream.read().toUByte(),
        inputStream.read().toUByte(),
        inputStream.read().toUByte()
    )

    val isINES2 = (flag7 and 0x0Cu) == (0x08u).toUByte()

    val isValid get() = name[0] == 'N' && name[1] == 'E' && name[2] == 'S' && name[3].code == 0x1A

    var mirroring
        get() = if (flag6 and BIT0 > 0u) Mirror.VERTICAL else Mirror.HORIZONTAL
        set(value) {
            when (value) {
                Mirror.VERTICAL -> flag6 = flag6 or 1u
                Mirror.HORIZONTAL -> flag6 = flag6 and 0b11111110u
            }
        }

    var mapper: UShort
        get() =
            if (isINES2) {
                val first = flag6 shr 4
                val second = flag7 shr 4
                val third = flag8 and 0b00001111u
                (third.toUShort() shl 8) or (second.toUShort() shl 4) or first.toUShort()
            } else {
                (flag7 shr 4 shl 4 or (flag6 shr 4)).toUShort()
            }
        set(value) {
            flag6 = ((value and 0b00001111u).toUByte() shl 4) or (flag6 and 0b00001111u)
            flag7 = ((value shr 4 and 0b00001111u).toUByte() shl 4) or (flag7 and 0b00001111u)
            if (isINES2) {
                flag8 = (flag8 and 0b11110000u) or (value shr 8 and 0b00001111u).toUByte()
            }
        }

    var consoleType: ConsoleType
        get() =
            if (isINES2) {
                ConsoleType.values()[flag7.toInt() and 0x3]
            } else {
                when {
                    flag7 and BIT0 > 0u -> ConsoleType.VS_SYSTEM
                    flag7 and BIT1 > 0u -> ConsoleType.PLAYCHOICE_10
                    else -> ConsoleType.NES
                }
            }
        set(value) {
            flag7 = if (isINES2) {
                flag7 and 0b11111100u or (value.ordinal.toUByte() and 0x3u)
            } else {
                val data = when (value) {
                    ConsoleType.PLAYCHOICE_10 -> 0b10u
                    ConsoleType.VS_SYSTEM -> 1u
                    ConsoleType.NES -> 0u
                    else -> return
                }
                flag7 and 0b11111100u or data.toUByte()
            }
        }

    var tvType: TVType
        get() =
            if (isINES2) {
                when ((flag12 and 0x3u).toUInt()) {
                    0u, 2u -> TVType.NTSC
                    1u -> TVType.PAL
                    else -> TVType.DENDY
                }
            } else {
                if (flag9 and BIT0 > 0u) TVType.PAL else TVType.NTSC
            }
        set(value) {
            if (isINES2) {
                val data = when (value) {
                    TVType.NTSC -> 0u
                    TVType.PAL -> 1u
                    TVType.DENDY -> 3u
                }
                flag12 = flag12 and 0b11111100u or data.toUByte()
            } else {
                flag9 = when (value) {
                    TVType.PAL -> flag9 or 1u
                    TVType.NTSC -> flag9 and 0b11111110u
                    else -> return
                }
            }
        }


    var hasBatteryComponents
        get() = flag6 and BIT1 > 0u
        set(value) {
            flag6 = flag6 and BIT1.inv() or (if (value) BIT1 else 0u)
        }

    var hasTrainerData
        get() = flag6 and BIT2 > 0u
        set(value) {
            flag6 = flag6 and BIT2.inv() or (if (value) BIT2 else 0u)
        }


    var hardWiredFourScreenMode
        get() = flag6 and BIT3 > 0u
        set(value) {
            flag6 = flag6 and BIT3.inv() or (if (value) BIT3 else 0u)
        }

    var subMapper
        get() = if (isINES2) flag8 shr 4 else 0u
        set(value) {
            if (isINES2) {
                flag8 = flag8 and 0b00001111u or (value shl 4)
            }
        }

    //region PRG/CHR

    val prgRomSize: UInt
        get() {
            return if (isINES2) {
                val msb = flag9 and 0b00001111u
                if (msb == (0xFu).toUByte()) {
                    val multiplier = (prgRomData and 0x3u).toUInt()
                    val exponent = (prgRomData shr 2).toInt()
                    (1u shl exponent) * ((multiplier shl 2) + 1u)
                } else {
                    ((msb.toUShort() shl 8) or prgRomData.toUShort()) * 0x4000u
                }
            } else {
                prgRomData * 0x4000u
            }
        }

    val chrRomSize: UInt
        get() {
            return if (isINES2) {
                val msb = flag9 shr 4
                if (msb == (0xFu).toUByte()) {
                    val multiplier = (chrRomData and 0x3u).toUInt()
                    val exponent = (chrRomData shr 2).toInt()
                    (1u shl exponent) * ((multiplier shl 2) + 1u)
                } else {
                    ((msb.toUShort() shl 8) or chrRomData.toUShort()) * 0x2000u
                }
            } else {
                chrRomData * 0x2000u
            }
        }

    fun setPrgRomBanks(banks: UShort) {
        prgRomData = banks.toUByte()
        if (isINES2) {
            flag9 = flag9 and 0b11110000u or ((banks shl 8).toUByte() and 0b1111u)
        }
    }

    fun setChrRomBanks(banks: UShort) {
        chrRomData = banks.toUByte()
        if (isINES2) {
            flag9 = flag9 and 0b00001111u or ((banks shl 4).toUByte() and 0b11110000u)
        }
    }

    fun setPrgRomExponential(multiplier: UByte, exponent: UByte) {
        if (isINES2) {
            prgRomData = exponent shl 2 or ((multiplier - 1u) / 2u and 0b11u).toUByte()
            flag9 = flag9 or 0b1111u
        }
    }

    fun setChrRomExponential(multiplier: UByte, exponent: UByte) {
        if (isINES2) {
            chrRomData = exponent shl 2 or ((multiplier - 1u) / 2u and 0b11u).toUByte()
            flag9 = flag9 or 0b11110000u
        }
    }

    //endregion


    fun toByteArray(): ByteArray {
        return byteArrayOf(
            'N'.code.toByte(),
            'E'.code.toByte(),
            'S'.code.toByte(),
            0x1A,
            prgRomData.toByte(),
            chrRomData.toByte(),
            flag6.toByte(),
            flag7.toByte(),
            flag8.toByte(),
            flag9.toByte(),
            flag10.toByte(),
            flag11.toByte(),
            flag12.toByte(),
            flag13.toByte(),
            flag14.toByte(),
            flag15.toByte()
        )
    }
}