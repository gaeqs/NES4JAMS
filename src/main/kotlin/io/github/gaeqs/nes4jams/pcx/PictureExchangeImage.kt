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

package io.github.gaeqs.nes4jams.pcx

import io.github.gaeqs.nes4jams.util.extension.concatenate
import java.io.InputStream

class PictureExchangeImage {

    data class Header(
        val fixed: UByte,
        val version: UByte,
        val encoding: Boolean,
        val bitsInPixel: UByte,
        val minX: UShort,
        val minY: UShort,
        val maxX: UShort,
        val maxY: UShort,
        val hdpi: UShort,
        val ydpi: UShort,
        val egaPalette: UByteArray,
        val reserved: UByte,
        val colors: UByte,
        val bytesInScanline: UShort,
        val paletteMode: UShort,
        val originWidth: UShort,
        val originHeight: UShort,
    ) {
        constructor(stream: InputStream) : this(
            stream.read().toUByte(),
            stream.read().toUByte(),
            stream.read() > 0,
            stream.read().toUByte(),
            stream.readUShort(),
            stream.readUShort(),
            stream.readUShort(),
            stream.readUShort(),
            stream.readUShort(),
            stream.readUShort(),
            stream.readNBytes(48).toUByteArray(),
            stream.read().toUByte(),
            stream.read().toUByte(),
            stream.readUShort(),
            stream.readUShort(),
            stream.readUShort(),
            stream.readUShort()
        ) {

        }
    }

}

private fun InputStream.readUShort(): UShort {
    val a = read().toUByte()
    val b = read().toUByte()
    return b concatenate a
}