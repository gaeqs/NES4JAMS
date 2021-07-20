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

import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.experimental.and
import kotlin.math.min

/**
 * This class implements the PCX Run-Length encoding.
 * With this class you can read and write PCX files that uses the RLE codec.
 *
 * This class stores status data between reads and writes.
 * You can use the codec to write and read two files simultaneously, but don't use it to write two files or read
 * to files!
 */
class PCXRLECodec(val compressed: Boolean) {

    private var readCount = 0
    private var readSample: Byte = 0

    /**
     * Reads [amount] bytes form the [stream] and stores them in the given [buffer], starting from the given [offset][start].
     * @param stream the stream to read.
     * @param buffer the buffer where the data will be stored.
     * @param start the position where the method will start to write data to the buffer.
     * @param amount the amount of data to read.
     */
    fun read(stream: InputStream, buffer: ByteArray, start: Int = 0, amount: Int = buffer.size) {
        if (amount + start > buffer.size) throw IllegalArgumentException("Amount + Start is bigger than the buffer size.")
        if (compressed) {
            val prefill = min(readCount, amount)
            Arrays.fill(buffer, start, prefill + start, readSample)
            readCount -= prefill

            var bytesRead = prefill

            while (bytesRead < amount) {
                val b = stream.read().toByte()
                if (b and 0xC0.toByte() == 0xC0.toByte()) {
                    readCount = (b and 0x3F.toByte()).toInt()
                    readSample = stream.read().toByte()
                } else {
                    readCount = 1
                    readSample = b
                }
                val toAdd = min(readCount, amount - bytesRead)
                Arrays.fill(buffer, bytesRead + start, bytesRead + toAdd + start, readSample)
                bytesRead += toAdd
                readCount -= toAdd
            }
        } else {
            val result = stream.readNBytes(buffer, start, amount)
            if (result < amount) throw IllegalStateException("Premature end of file reading image data")
        }
    }

    private var previousByte: Byte? = null
    private var repeatCount: Int = 0

    /**
     * Reads [amount] bytes from the [buffer], starting from the given [offset][start], and writes them in the given [stream].
     * @param stream the stream to write.
     * @param buffer the buffer where the data is stored.
     * @param start the position where the method will start to read data to the buffer.
     * @param amount the amount of data to write.
     */
    fun write(stream: OutputStream, buffer: ByteArray, start: Int = 0, amount: Int = buffer.size) {
        if (amount + start > buffer.size) throw IllegalArgumentException("Amount + Start is bigger than the buffer size.")
        if (compressed) {
            repeat(amount) {
                val element = buffer[it + start]
                if (element == previousByte && repeatCount < 63) {
                    repeatCount++
                } else {
                    if (repeatCount > 0) {
                        writeCount(stream)
                    }
                    previousByte = element
                    repeatCount = 1
                }
            }

        } else {
            stream.write(buffer, start, amount)
        }
    }

    /**
     * Flush the remaining data in this codec and writes it in the given [stream]
     *
     * This method must be executed after the data has been written into the stream.
     *
     * @param stream the stream where the data will be stored.
     */
    fun flush(stream: OutputStream) {
        if (repeatCount == 0) return
        writeCount(stream)
        repeatCount = 0
    }

    private fun writeCount(stream: OutputStream) {
        // We make sure the previous byte is not null!
        val byte = previousByte ?: return

        if (repeatCount == 1 && (byte and 0xC0.toByte() != 0xC0.toByte())) {
            stream.write(byte.toInt())
        } else {
            stream.write(0xC0 or repeatCount)
            stream.write(byte.toInt())
        }
    }

}