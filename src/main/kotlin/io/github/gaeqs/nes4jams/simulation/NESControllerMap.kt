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

package io.github.gaeqs.nes4jams.simulation

data class NESControllerMap(
    val a: Boolean = false,
    val b: Boolean = false,
    val select: Boolean = false,
    val start: Boolean = false,
    val up: Boolean = false,
    val down: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false
) {

    fun toByte(): UByte {
        var byte: UByte = 0u
        byte = byte or if (a) 0x80u else 0x0u
        byte = byte or if (b) 0x40u else 0x0u
        byte = byte or if (select) 0x20u else 0x0u
        byte = byte or if (start) 0x10u else 0x0u
        byte = byte or if (up) 0x08u else 0x0u
        byte = byte or if (down) 0x04u else 0x0u
        byte = byte or if (left) 0x02u else 0x0u
        byte = byte or if (right) 0x01u else 0x0u
        return byte
    }

    infix fun union(other: NESControllerMap) = NESControllerMap(
        a || other.a,
        b || other.b,
        select || other.select,
        start || other.start,
        up || other.up,
        down || other.down,
        left || other.left,
        right || other.right
    )

}