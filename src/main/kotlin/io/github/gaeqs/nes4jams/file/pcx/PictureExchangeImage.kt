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

package io.github.gaeqs.nes4jams.file.pcx

import io.github.gaeqs.nes4jams.util.extension.concatenate
import io.github.gaeqs.nes4jams.util.extension.shl
import io.github.gaeqs.nes4jams.util.extension.shr
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import net.jamsimulator.jams.utils.StringUtils
import java.awt.image.*
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.min

/**
 * Represents a PCX image.
 *
 * PCX images are widely used by NES developers to store CHR data after assembling due to their palette support.
 *
 * This class only supports the palette version of PCX images: you cannot load a RGB PCX image.
 */
class PictureExchangeImage {

    /**
     * This class represents the header of a PCX file.
     */
    data class Header(
        val fixed: UByte,
        val version: UByte,
        val encoding: Boolean,
        val bitsPerPixel: UByte,
        val minX: UShort,
        val minY: UShort,
        val maxX: UShort,
        val maxY: UShort,
        val hdpi: UShort,
        val ydpi: UShort,
        val egaPalette: ColorPalette,
        val reserved: UByte,
        val colorPlanes: UByte,
        val bytesPerLine: UShort,
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
            ColorPalette(16, stream),
            stream.read().toUByte(),
            stream.read().toUByte(),
            stream.readUShort(),
            stream.readUShort(),
            stream.readUShort(),
            stream.readUShort()
        ) {
            // Skip unused header data
            stream.skip(54)
        }

        val width get() = (maxX - minX + 1u).toInt()
        val height get() = (maxY - minY + 1u).toInt()

        val readInPaletteMode: Boolean
            get() {
                val bitsInPixel = bitsPerPixel.toInt()
                return bitsInPixel == 1 || bitsInPixel == 2 || bitsInPixel == 4 || bitsInPixel == 8
                        && colorPlanes == 1.toUByte()
            }

        fun write(stream: OutputStream) {
            stream.write(fixed.toInt())
            stream.write(version.toInt())
            stream.write(if (encoding) 1 else 0)
            stream.write(bitsPerPixel.toInt())
            stream.writeUShort(minX)
            stream.writeUShort(minY)
            stream.writeUShort(maxX)
            stream.writeUShort(maxY)
            stream.writeUShort(hdpi)
            stream.writeUShort(ydpi)
            egaPalette.write(stream, 16)
            stream.write(reserved.toInt())
            stream.write(colorPlanes.toInt())
            stream.writeUShort(bytesPerLine)
            stream.writeUShort(paletteMode)
            stream.writeUShort(originWidth)
            stream.writeUShort(originHeight)
            repeat(54) { stream.write(0) }
        }
    }

    /**
     * Helper class used to store color palettes of a PCX file.
     */
    class ColorPalette(array: IntArray) {

        val rawPalette = array.copyOf()

        constructor(size: Int, stream: InputStream) : this(IntArray(size) {
            stream.readColor()
        })

        fun write(stream: OutputStream, min: Int = 0) {
            rawPalette.forEach { stream.writeColor(it) }
            repeat(min - rawPalette.size) { stream.writeColor(0) }
        }

        override fun toString(): String {
            return "ColorPalette[${
                rawPalette.joinToString(separator = ", ") { "0x${StringUtils.addZeros(Integer.toHexString(it), 8)}" }
            }]"
        }
    }

    companion object {
        /**
         * Creates an empty NES-optimized PCX file.
         */
        fun createEmpty(width: Int = 128, height: Int = 128): PictureExchangeImage {
            val header = Header(
                10u,
                5u,
                true,
                2u,
                0u,
                0u,
                (width - 1).toUShort(),
                (height - 1).toUShort(),
                0u,
                0u,
                ColorPalette(
                    intArrayOf(
                        0xFF000000.toInt(),
                        0xFF555555.toInt(),
                        0xFFAAAAAA.toInt(),
                        0xFFFFFFFF.toInt()
                    )
                ),
                0u,
                1u,
                ((width * 2 + 7) / 8).toUShort(),
                1u,
                width.toUShort(),
                height.toUShort()
            )
            return PictureExchangeImage(header, header.egaPalette)
        }

        fun fromCHRData(data: ByteArray): PictureExchangeImage {
            // Width must be 128 pixels width and 8n pixels height.
            // Each 8 pixels constitute 2 bytes (4 pixels per byte)
            val pixels = data.size shl 2
            if (pixels % 128 != 0)
                throw IllegalStateException("Cannot convert PCX image to CHR. The width must be 128 pixels.")
            val height = pixels / 128
            if (height % 8 != 0)
                throw IllegalStateException("Cannot convert PCX image to CHR. The width must be 8n pixels.")
            val image = createEmpty(height = height)

            for (y in 0 until height / 8) {
                for (x in 0 until 16) {
                    val offset = y * 256 + x * 16
                    for (row in 0 until 8) {
                        var most = data[offset + row].toUByte()
                        var least = data[offset + row + 8].toUByte()
                        for (column in 0 until 8) {
                            val pixel = ((most and 0x01u shl 1) or (least and 0x01u)).toInt()
                            most = most shr 1
                            least = least shr 1
                            image[x * 8 + (7 - column), y * 8 + row] = pixel
                        }
                    }
                }
            }
            return image
        }
    }

    val header: Header
    val data: ByteArray
    val palette: ColorPalette

    /**
     * Directly creates a PCX file using a header, the data and the palette to use.
     */
    constructor(
        header: Header,
        palette: ColorPalette,
        data: ByteArray = ByteArray(header.height * header.bytesPerLine.toInt()),
    ) {
        this.header = header
        this.data = data
        this.palette = palette
    }

    /**
     * Loads a PCX file from the given [stream].
     */
    constructor(stream: InputStream) {
        header = Header(stream)
        data = readImageData(stream)
        palette = readPalette(stream)
    }

    /**
     * Returns the palette index at the given coordinate.
     */
    operator fun get(x: Number, y: Number): Int {
        val bytesPerLine = header.bytesPerLine.toInt()
        val bitsPerPixel = header.bitsPerPixel.toInt()
        val index = y.toInt() * bytesPerLine + (x.toInt() * bitsPerPixel) / 8

        // We have the byte, but we need only the pixel's bits!
        val offset = (bitsPerPixel * x.toInt()) % 8
        return (data[index].toInt() shr offset) and ((1 shl bitsPerPixel) - 1)
    }

    /**
     * Sets the given palette index at the given coordinate.
     */
    operator fun set(x: Number, y: Number, value: Int) {
        val bytesPerLine = header.bytesPerLine.toInt()
        val bitsPerPixel = header.bitsPerPixel.toInt()
        val index = y.toInt() * bytesPerLine + (x.toInt() * bitsPerPixel) / 8

        // We have the byte, but we need to set the pixel's bits!
        val offset = (bitsPerPixel * x.toInt()) % 8

        // Clear the space:
        val mask = (((1 shl bitsPerPixel) - 1) shl offset)
        val maskedByte = data[index] and mask.inv().toByte()

        data[index] = maskedByte or (value shl offset and mask).toByte()
    }

    /**
     * Returns a modified copy of this PCX image.
     * This copy is optimized to work with it as a NES CHR file.
     */
    fun optimizeForNES(): PictureExchangeImage {
        val newHeader = header.copy(
            bitsPerPixel = 2u,
            egaPalette = ColorPalette(
                intArrayOf(
                    0xFF000000.toInt(),
                    0xFF555555.toInt(),
                    0xFFAAAAAA.toInt(),
                    0xFFFFFFFF.toInt()
                )
            ),
            bytesPerLine = ((header.width * 2 + 7) / 8).toUShort() // +7 to ceil
        )

        if (header.bitsPerPixel.toInt() == 2) {
            return PictureExchangeImage(newHeader, newHeader.egaPalette, data.copyOf())
        }

        val image = PictureExchangeImage(newHeader, newHeader.egaPalette)

        for (x in 0 until header.width) {
            for (y in 0 until header.height) {
                image[x, y] = min(this[x, y], 3)
            }
        }

        return image
    }

    /**
     * Returns a copy of this PCX image that uses 8 bits per pixel in its palette.
     */
    fun to8BPP(): PictureExchangeImage {
        if (header.bitsPerPixel.toInt() == 8) {
            return PictureExchangeImage(header.copy(), ColorPalette(palette.rawPalette), data.copyOf())
        }

        val newHeader = header.copy(
            bitsPerPixel = 8u,
            egaPalette = ColorPalette(IntArray(256) {
                if (palette.rawPalette.size > it) palette.rawPalette[it] else 0xFFFFFFFF.toInt()
            }),
            bytesPerLine = header.width.toUShort()
        )

        return PictureExchangeImage(newHeader, newHeader.egaPalette, to8BPPDataBuffer())
    }

    /**
     * Transforms this PCX image into CHR data that can be written directly to a .nes file.
     *
     * @throws IllegalStateException if the width or the height of the image are not a multiple of 8.
     */
    fun toCHRData(): ByteArray {
        if (header.width % 8 != 0)
            throw IllegalStateException("Cannot convert PCX image to CHR. The width must be a multiple of 8.")
        if (header.height % 8 != 0)
            throw IllegalStateException("Cannot convert PCX image to CHR. The height must be a multiple of 8.")
        val byteArray = ByteArray(2 * header.width / 8 * header.height) // Two bytes for 8 pixels!

        for (y in 0 until header.height / 8) {
            for (x in 0 until header.width / 8) {
                val offset = (y * 256 + x * 16)

                for (row in 0 until 8) {
                    var least = 0
                    var most = 0

                    for (column in 0 until 8) {
                        val pixelX = x * 8 + column
                        val pixelY = y * 8 + row
                        val value = get(pixelX, pixelY)
                        least = (least shl 1) or (value and 0x1)
                        most = (most shl 1) or (value shr 1 and 0x1)
                    }

                    byteArray[offset + row] = most.toByte()
                    byteArray[offset + row + 8] = least.toByte()
                }

            }
        }

        return byteArray
    }

    /**
     * Saves this PCX image into the given [stream]
     */
    fun write(stream: OutputStream) {
        header.write(stream)

        val rle = PCXRLECodec(header.encoding)
        rle.write(stream, data)
        rle.flush(stream)
        if (palette.rawPalette.size == 256) {
            stream.write(12)
            palette.write(stream)
        }
    }

    private fun readImageData(stream: InputStream): ByteArray {
        if (!header.readInPaletteMode) throw IllegalArgumentException("Only palette images are supported!")
        if (header.width < 0) throw IllegalArgumentException("Width is negative.")
        if (header.height < 0) throw IllegalArgumentException("Height is negative.")
        if (header.colorPlanes.toInt() !in 0..4)
            throw IllegalArgumentException("Illegal planes argument: ${header.colorPlanes} planes.")

        val codec = PCXRLECodec(header.encoding)
        val image = ByteArray(header.height * header.bytesPerLine.toInt())
        codec.read(stream, image)

        return image
    }

    private fun readPalette(stream: InputStream) = when (header.bitsPerPixel.toInt()) {
        1 -> ColorPalette(intArrayOf(0, 0xFFFFFFFF.toInt()))
        8 -> read256Palette(stream)
        else -> header.egaPalette
    }

    private fun read256Palette(stream: InputStream) =
        if (stream.read() != 12) read265FromEndOfFile(stream) else ColorPalette(256, stream)

    private fun read265FromEndOfFile(stream: InputStream): ColorPalette {
        val all = stream.readAllBytes()
        val newStream = all.inputStream().apply { skip(all.size - 769L) } // 769 = 256 * 3 + 1
        if (newStream.read() != 12) {
            newStream.close()
            throw IllegalStateException("Couldn't find palette!")
        }
        val palette = ColorPalette(256, newStream)
        newStream.close()
        return palette
    }

    fun toBufferedImage(paletteToApply: ColorPalette = palette): BufferedImage {

        val raster = Raster.createInterleavedRaster(
            to8BPPDataBuffer().let { DataBufferByte(it, it.size) },
            header.width,
            header.height,
            header.width,
            1,
            intArrayOf(0),
            null
        )

        val raw = paletteToApply.rawPalette
        val paletteSize = 1 shl header.bitsPerPixel.toInt()
        val newRaw = if (paletteSize <= raw.size) raw
        else IntArray(paletteSize) { if (it < raw.size) raw[it] else 0 }

        val colorModel = IndexColorModel(
            header.bitsPerPixel.toInt(),
            1 shl header.bitsPerPixel.toInt(),
            newRaw,
            0,
            false,
            -1,
            DataBuffer.TYPE_BYTE
        )

        return BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, Properties())
    }

    fun toFXImage(paletteToApply: ColorPalette = palette): WritableImage {
        val image = WritableImage(header.width, header.height)
        image.pixelWriter.setPixels(
            0,
            0,
            header.width,
            header.height,
            PixelFormat.createByteIndexedInstance(paletteToApply.rawPalette),
            to8BPPDataBuffer(),
            0,
            header.width
        )
        return image
    }

    private fun to8BPPDataBuffer(): ByteArray {
        if (header.bitsPerPixel.toInt() == 8) return data
        val bpp = header.bitsPerPixel.toInt()
        val mask = (1 shl bpp) - 1
        val width = header.width
        val array = ByteArray(header.width * header.height)

        var index = 0
        var x = 0

        data.forEach {
            var bit = 0
            while (bit < 8 && x < width) {
                array[index++] = (it.toInt() shr bit and mask).toByte()
                bit += bpp
                x++
            }
            if (x == width) x = 0
        }
        return array
    }
}


private fun InputStream.readUShort(): UShort {
    val a = read().toUByte()
    val b = read().toUByte()
    return b concatenate a
}

private fun OutputStream.writeUShort(short: UShort) {
    write(short.toInt())
    write(short.toInt() shr 8)
}

private fun InputStream.readColor(): Int {
    val a = 0xFF000000.toInt()
    val r = read() shl 16
    val g = read() shl 8
    val b = read()
    return a or r or g or b
}

private fun OutputStream.writeColor(color: Int) {
    write(color shr 16)
    write(color shr 8)
    write(color)
}

