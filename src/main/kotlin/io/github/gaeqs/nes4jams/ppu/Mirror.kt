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

@ExperimentalUnsignedTypes
enum class Mirror(private val mapper: (Array<UByteArray>, UShort) -> UByte) {
    HARDWARE({ nameTables, address ->
        0u
    }),
    VERTICAL({ nameTables, address ->
        when (address and 0x0FFFu) {
            in 0x0000u..0x03FFu,
            in 0x0800u..0x0BFFu -> nameTables[0][(address and 0x03FFu).toInt()]
            else -> nameTables[1][(address and 0x03FFu).toInt()]
        }
    }),
    HORIZONTAL({ nameTables, address ->
        when (address and 0x0FFFu) {
            in 0x0000u..0x07FFu -> nameTables[0][(address and 0x03FFu).toInt()]
            else -> nameTables[1][(address and 0x03FFu).toInt()]
        }
    }),
    ONESCREEN_LO({ nameTables, address ->
        nameTables[0][(address and 0x03FFu).toInt()]
    }),
    ONESCREEN_HI({ nameTables, address ->
        nameTables[1][(address and 0x03FFu).toInt()]
    });

    fun map(nameTables: Array<UByteArray>, address: UShort): UByte = mapper(nameTables, address)
}